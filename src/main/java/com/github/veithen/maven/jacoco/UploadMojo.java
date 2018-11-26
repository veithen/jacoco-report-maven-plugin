/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 Andreas Veithen
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.veithen.maven.jacoco;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.tools.ExecFileLoader;

import com.github.veithen.maven.shared.mojo.aggregating.AggregatingMojo;

@Mojo(name="upload", defaultPhase=LifecyclePhase.POST_INTEGRATION_TEST, threadSafe=true)
public final class UploadMojo extends AggregatingMojo<CoverageData> {
    @Parameter(defaultValue="${project.build.directory}/jacoco.exec", required=true)
    private File dataFile;

    @Parameter(defaultValue="${env.TRAVIS_JOB_ID}", required=true, readonly=true)
    private String jobId;

    @Parameter(defaultValue="https://coveralls.io/api/v1/jobs", required=true)
    private String apiEndpoint;

    @Parameter(defaultValue="true")
    private boolean includeClasses;

    public UploadMojo() {
        super(CoverageData.class);
    }

    @Override
    protected CoverageData doExecute() throws MojoExecutionException, MojoFailureException {
        boolean dataFileExists = dataFile.exists();
        Map<String, File> sources = new HashMap<>();
        if (includeClasses) {
            for (String compileSourceRoot : project.getCompileSourceRoots()) {
                File basedir = new File(compileSourceRoot);
                if (basedir.exists()) {
                    DirectoryScanner scanner = new DirectoryScanner();
                    scanner.setBasedir(basedir);
                    scanner.scan();
                    for (String includedFile : scanner.getIncludedFiles()) {
                        sources.put(includedFile, new File(basedir, includedFile));
                    }
                }
            }
        }
        if (dataFileExists || !sources.isEmpty()) {
            return new CoverageData(
                    // Can't use optional here because it's not serializable.
                    dataFileExists ? dataFile : null,
                    includeClasses ? project.getArtifact().getFile() : null,
                    sources);
        } else {
            return null;
        }
    }

    private File findRootDir() throws MojoExecutionException {
        File rootDir = project.getBasedir();
        while (!new File(rootDir, ".git").exists()) {
            rootDir = rootDir.getParentFile();
            if (rootDir == null) {
                throw new MojoExecutionException("Root directory not found; are we running from a Git clone?");
            }
        }
        return rootDir;
    }

    private static <T> Iterable<T> toIterable(Stream<T> stream) {
        return stream::iterator;
    }

    @Override
    protected void doAggregate(List<CoverageData> results) throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        if (results.stream().map(CoverageData::getDataFile).noneMatch(Objects::isNull)) {
            log.info("No coverage data collected; skipping execution.");
            return;
        }
        ExecFileLoader loader = new ExecFileLoader();
        for (File dataFile : toIterable(results.stream().map(CoverageData::getDataFile).filter(Objects::nonNull))) {
            try {
                loader.load(dataFile);
            } catch (IOException ex) {
                throw new MojoExecutionException(String.format("Failed to load exec file %s: %s", dataFile, ex.getMessage()), ex);
            }
        }
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
        for (File classes : toIterable(results.stream().map(CoverageData::getClasses).filter(Objects::nonNull))) {
            try {
                analyzer.analyzeAll(classes);
            } catch (IOException ex) {
                throw new MojoExecutionException(String.format("Failed to analyze %s: %s", classes, ex.getMessage()), ex);
            }
        }
        Map<String, File> sourceFiles = new HashMap<>();
        results.stream().map(CoverageData::getSources).forEach(sourceFiles::putAll);
        Sources sources = new Sources(sourceFiles, findRootDir());
        IBundleCoverage bundle = builder.getBundle("Coverage Report");
        JsonArrayBuilder sourceFilesBuilder = Json.createArrayBuilder();
        for (IPackageCoverage packageCoverage : bundle.getPackages()) {
            for (ISourceFileCoverage sourceFileCoverage : packageCoverage.getSourceFiles()) {
                Source source = sources.lookup(sourceFileCoverage);
                if (source == null) {
                    break;
                }
                JsonArrayBuilder coverageBuilder = Json.createArrayBuilder();
                for (int i=1; i<sourceFileCoverage.getFirstLine(); i++) {
                    coverageBuilder.add(JsonValue.NULL);
                }
                for (int i=sourceFileCoverage.getFirstLine(); i<=sourceFileCoverage.getLastLine(); i++) {
                    switch (sourceFileCoverage.getLine(i).getStatus()) {
                        case ICounter.EMPTY:
                            coverageBuilder.add(JsonValue.NULL);
                            break;
                        case ICounter.NOT_COVERED:
                            coverageBuilder.add(0);
                            break;
                        default:
                            coverageBuilder.add(1);
                    }
                }
                sourceFilesBuilder.add(Json.createObjectBuilder()
                        .add("name", source.getPathRelativeToRepositoryRoot())
                        .add("source_digest", source.digest())
                        .add("coverage", coverageBuilder.build())
                        .build());
            }
        }
        JsonObject jsonFile = Json.createObjectBuilder()
                .add("service_name", "travis-ci")
                .add("service_job_id", jobId)
                .add("source_files", sourceFilesBuilder.build())
                .build();
        HttpEntity entity = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addPart("json_file", new JsonContentBody(jsonFile, "coverage.json"))
                .build();
        HttpPost post = new HttpPost(apiEndpoint);
        post.setEntity(entity);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpResponse response = httpClient.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new MojoFailureException(String.format("Coveralls responded with status code %s", statusCode));
            }
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to send request to Coveralls", ex);
        }
    }
}
