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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;

import com.github.veithen.maven.shared.mojo.aggregating.AggregatingMojo;

@Mojo(name="upload", defaultPhase=LifecyclePhase.POST_INTEGRATION_TEST, threadSafe=true)
public final class UploadMojo extends AggregatingMojo<CoverageData> {
    @Parameter(defaultValue="${project.build.directory}/jacoco.exec", required=true)
    private File dataFile;

    @Parameter(defaultValue="${env.TRAVIS_REPO_SLUG}", required=true, readonly=true)
    private String repoSlug;

    @Parameter(defaultValue="${env.TRAVIS_JOB_ID}", required=true, readonly=true)
    private String jobId;

    @Parameter(defaultValue="true")
    private boolean includeClasses;

    @Parameter(defaultValue="https://coveralls.io", required=true)
    private String coverallsApiEndpoint;

    @Parameter(defaultValue="https://codecov.io", required=true)
    private String codecovApiEndpoint;

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
        if (results.stream().map(CoverageData::getDataFile).allMatch(Objects::isNull)) {
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
        IBundleCoverage bundle = builder.getBundle("Coverage Report");
        Client client = ClientBuilder.newBuilder()
                .register(MultiPartFeature.class)
                .build();
        CoverageService[] coverageServices = {
                new Coveralls(client.target(coverallsApiEndpoint)),
                new Codecov(client.target(codecovApiEndpoint)),
        };
        Context context = new Context(loader, bundle, sourceFiles, findRootDir());
        for (CoverageService service : coverageServices) {
            try {
                if (!service.isConfigured(repoSlug)) {
                    log.info(String.format("Skipping upload to %s: not configured", service.getName()));
                    continue;
                }
                service.upload(jobId, context);
            } catch (WebApplicationException ex) {
                throw new MojoFailureException(String.format("Failed to send request to %s", service.getName()), ex);
            }
            log.info(String.format("Successfully uploaded coverage data to %s", service.getName()));
        }
    }
}
