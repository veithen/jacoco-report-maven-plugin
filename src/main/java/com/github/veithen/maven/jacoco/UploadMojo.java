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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.csv.CSVFormatter;

import com.github.veithen.maven.shared.mojo.aggregating.AggregatingMojo;

@Mojo(name="upload", defaultPhase=LifecyclePhase.POST_INTEGRATION_TEST, threadSafe=true)
public final class UploadMojo extends AggregatingMojo<CoverageData> {
    @Parameter(defaultValue="${project.build.directory}/jacoco.exec", required=true)
    private File dataFile;

    public UploadMojo() {
        super(CoverageData.class);
    }

    @Override
    protected CoverageData doExecute() throws MojoExecutionException, MojoFailureException {
        if (dataFile.exists()) {
            return new CoverageData(
                    dataFile,
                    project.getArtifact().getFile(),
                    project.getCompileSourceRoots().stream().map(File::new).collect(Collectors.toList()));
        } else {
            return null;
        }
    }

    @Override
    protected void doAggregate(List<CoverageData> results) throws MojoExecutionException, MojoFailureException {
        ExecFileLoader loader = new ExecFileLoader();
        for (CoverageData coverageData : results) {
            try {
                loader.load(coverageData.getDataFile());
            } catch (IOException ex) {
                throw new MojoExecutionException(String.format("Failed to load exec file %s: %s", coverageData.getDataFile(), ex.getMessage()), ex);
            }
        }
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
        for (CoverageData coverageData : results) {
            try {
                analyzer.analyzeAll(coverageData.getClasses());
            } catch (IOException ex) {
                throw new MojoExecutionException(String.format("Failed to analyze %s: %s", coverageData.getClasses(), ex.getMessage()), ex);
            }
        }
        IBundleCoverage bundle = builder.getBundle("Coverage Report");
        try {
            IReportVisitor visitor = new CSVFormatter().createVisitor(System.out);
            visitor.visitInfo(
                    loader.getSessionInfoStore().getInfos(),
                    loader.getExecutionDataStore().getContents());
            visitor.visitBundle(bundle, null /* TODO */);
            visitor.visitEnd();
        } catch (IOException ex) {
            throw new MojoExecutionException(String.format("Failed to generate coverage report: %s", ex.getMessage()), ex);
        }
    }
}
