/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 - 2021 Andreas Veithen
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.cxf.common.logging.Slf4jLogger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;

import com.github.veithen.maven.shared.mojo.aggregating.AggregatingMojo;

@Mojo(name = "process", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST, threadSafe = true)
public final class ProcessMojo extends AggregatingMojo<CoverageData> {
    @Parameter(defaultValue = "${project.build.directory}/jacoco.exec", required = true)
    private File dataFile;

    @Parameter(defaultValue = "true")
    private boolean includeClasses;

    @Component(role = ContinuousIntegrationContextFactory.class)
    private Map<String, ContinuousIntegrationContextFactory> continuousIntegrationContextFactories;

    @Component(role = CoverageServiceFactory.class)
    private Map<String, CoverageServiceFactory> coverageServiceFactories;

    @Parameter private Map<String, String> apiEndpoints;

    public ProcessMojo() {
        super(CoverageData.class);
    }

    private static boolean isChild(File parent, File child) {
        File candidate = child;
        while (candidate != null) {
            if (candidate.equals(parent)) {
                return true;
            }
            candidate = candidate.getParentFile();
        }
        return false;
    }

    @Override
    protected CoverageData doExecute() throws MojoExecutionException, MojoFailureException {
        boolean dataFileExists = dataFile.exists();
        Map<String, File> sources = new HashMap<>();
        if (includeClasses) {
            File buildDirectory = new File(project.getBuild().getDirectory());
            for (String compileSourceRoot : project.getCompileSourceRoots()) {
                File basedir = new File(compileSourceRoot);
                if (isChild(buildDirectory, basedir)) {
                    // Never include generated sources. Neither Coveralls nor Codecov will be
                    // able to display them.
                    continue;
                }
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
            File classes = new File(project.getBuild().getOutputDirectory());
            return new CoverageData(
                    // Can't use optional here because it's not serializable.
                    dataFileExists ? dataFile : null,
                    includeClasses && classes.exists() ? classes : null,
                    sources);
        } else {
            return null;
        }
    }

    private File findRootDir() {
        File rootDir = project.getBasedir();
        while (!new File(rootDir, ".git").exists()) {
            rootDir = rootDir.getParentFile();
            if (rootDir == null) {
                throw new IllegalStateException(
                        "Root directory not found; are we running from a Git clone?");
            }
        }
        return rootDir;
    }

    private static <T> Iterable<T> toIterable(Stream<T> stream) {
        return stream::iterator;
    }

    static MojoFailureException processException(
            String serviceName, WebApplicationException exception) {
        String message = null;
        try {
            JsonObject entity = exception.getResponse().readEntity(JsonObject.class);
            if (entity.getBoolean("error", true)) {
                message = entity.getString("message", null);
            }
        } catch (ProcessingException ex) {
            // Ignore
        }
        if (message == null) {
            message = exception.getMessage();
        }
        return new MojoFailureException(
                String.format("Failed to send request to %s: %s", serviceName, message), exception);
    }

    @Override
    protected void doAggregate(List<CoverageData> results)
            throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        if (results.stream().map(CoverageData::getDataFile).allMatch(Objects::isNull)) {
            log.info("No coverage data collected; skipping execution.");
            return;
        }
        if (results.stream().map(CoverageData::getClasses).allMatch(Objects::isNull)) {
            log.info("No classes included; skipping execution.");
            return;
        }
        ContinuousIntegrationContext ciContext = null;
        for (ContinuousIntegrationContextFactory factory :
                continuousIntegrationContextFactories.values()) {
            ciContext = factory.createContext(System.getenv());
            if (ciContext != null) {
                break;
            }
        }
        if (ciContext != null) {
            log.info("Continuous integration context:");
            log.info("  Repo slug: " + ciContext.getRepoSlug());
            log.info("  Branch: " + ciContext.getBranch());
            log.info("  Commit: " + ciContext.getCommit());
            log.info("  Service: " + ciContext.getService());
            log.info("  Build ID: " + ciContext.getBuildId());
            log.info("  Build run ID: " + ciContext.getBuildRunId());
            log.info("  Build URL: " + ciContext.getBuildUrl());
        }
        Client client =
                ClientBuilder.newBuilder()
                        .register(MultiPartFeature.class)
                        .register(UserAgentFeature.class)
                        .register(new LoggingFeature(new Slf4jLogger("jersey", null)))
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(1, TimeUnit.MINUTES)
                        .build();
        List<CoverageService> coverageServices = new ArrayList<>();
        for (Map.Entry<String, CoverageServiceFactory> entry :
                coverageServiceFactories.entrySet()) {
            coverageServices.add(
                    entry.getValue()
                            .newInstance(
                                    client,
                                    apiEndpoints == null
                                            ? Optional.empty()
                                            : Optional.ofNullable(
                                                    apiEndpoints.get(entry.getKey()))));
        }
        for (Iterator<CoverageService> it = coverageServices.iterator(); it.hasNext(); ) {
            CoverageService service = it.next();
            if (!service.isEnabled(ciContext)) {
                log.info(String.format("%s not configured/enabled", service.getName()));
                it.remove();
            }
        }
        if (coverageServices.isEmpty()) {
            log.info("No usable coverage services found; skipping execution.");
            return;
        }
        ExecFileLoader loader = new ExecFileLoader();
        for (File dataFile :
                toIterable(
                        results.stream().map(CoverageData::getDataFile).filter(Objects::nonNull))) {
            try {
                loader.load(dataFile);
            } catch (IOException ex) {
                throw new MojoExecutionException(
                        String.format("Failed to load exec file %s: %s", dataFile, ex.getMessage()),
                        ex);
            }
        }
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
        for (File classes :
                toIterable(
                        results.stream().map(CoverageData::getClasses).filter(Objects::nonNull))) {
            try {
                analyzer.analyzeAll(classes);
            } catch (IOException ex) {
                throw new MojoExecutionException(
                        String.format("Failed to analyze %s: %s", classes, ex.getMessage()), ex);
            }
        }
        Map<String, File> sourceFiles = new HashMap<>();
        results.stream().map(CoverageData::getSources).forEach(sourceFiles::putAll);
        IBundleCoverage bundle = builder.getBundle("Coverage Report");
        CoverageContext coverageContext =
                new CoverageContext(
                        loader,
                        bundle,
                        sourceFiles,
                        // Only try to find the root directory if we need to, so that the plugin
                        // works with Subversion and IPFS.
                        new Lazy<File>(this::findRootDir));
        for (CoverageService service : coverageServices) {
            String link;
            try {
                link = service.upload(ciContext, coverageContext);
            } catch (WebApplicationException ex) {
                throw processException(service.getName(), ex);
            }
            log.info(
                    String.format(
                            "Successfully uploaded coverage data to %s: %s",
                            service.getName(), link));
        }
    }
}
