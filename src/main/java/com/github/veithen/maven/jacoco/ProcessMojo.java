/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 - 2024 Andreas Veithen
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
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

    @Component(role = CoverageFileFormat.class)
    private Map<String, CoverageFileFormat> coverageFileFormats;

    @Parameter(defaultValue = "codecov", required = true)
    private String format;

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
        CoverageFileFormat coverageFileFormat = coverageFileFormats.get(format);
        if (coverageFileFormat == null) {
            throw new MojoExecutionException(String.format("Unknown format \"%s\"", format));
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
        File outputDir = new File(mavenSession.getTopLevelProject().getBuild().getDirectory());
        outputDir.mkdirs();
        File outputFile = new File(outputDir, coverageFileFormat.getDefaultFileName());
        try (OutputStream out = new FileOutputStream(outputFile)) {
            coverageFileFormat.write(coverageContext, out);
        } catch (IOException ex) {
            throw new MojoExecutionException(
                    String.format("Failed to write %s: %s", outputFile, ex.getMessage()), ex);
        }
    }
}
