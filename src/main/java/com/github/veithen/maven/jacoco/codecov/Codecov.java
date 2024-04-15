/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 - 2023 Andreas Veithen
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
package com.github.veithen.maven.jacoco.codecov;

import java.io.OutputStream;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import org.codehaus.plexus.component.annotations.Component;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;

import com.github.veithen.maven.jacoco.CoverageContext;
import com.github.veithen.maven.jacoco.CoverageFileFormat;
import com.github.veithen.maven.jacoco.Source;

/**
 * Implements the <a href="https://docs.codecov.com/docs/codecov-custom-coverage-format">Codecov
 * custom coverage format</a>.
 */
@Component(role = CoverageFileFormat.class, hint = "codecov")
public final class Codecov implements CoverageFileFormat {
    @Override
    public String getDefaultFileName() {
        return "coverage.json";
    }

    @Override
    public void write(CoverageContext coverageContext, OutputStream out) {
        JsonObjectBuilder sourceFilesBuilder = Json.createObjectBuilder();
        for (IPackageCoverage packageCoverage : coverageContext.getBundle().getPackages()) {
            for (ISourceFileCoverage sourceFileCoverage : packageCoverage.getSourceFiles()) {
                Source source = coverageContext.lookupSource(sourceFileCoverage);
                if (source == null) {
                    continue;
                }
                JsonObjectBuilder coverageBuilder = Json.createObjectBuilder();
                for (int i = sourceFileCoverage.getFirstLine();
                        i <= sourceFileCoverage.getLastLine();
                        i++) {
                    ILine line = sourceFileCoverage.getLine(i);
                    if (line.getStatus() == ICounter.EMPTY) {
                        continue;
                    }
                    ICounter branchCounter = line.getBranchCounter();
                    String value;
                    if (branchCounter.getTotalCount() > 0) {
                        value =
                                String.format(
                                        "%s/%s",
                                        branchCounter.getCoveredCount(),
                                        branchCounter.getTotalCount());
                    } else if (line.getStatus() == ICounter.NOT_COVERED) {
                        value = "0";
                    } else {
                        value = "1";
                    }
                    coverageBuilder.add(String.valueOf(i), value);
                }
                sourceFilesBuilder.add(
                        source.getPathRelativeToRepositoryRoot(), coverageBuilder.build());
            }
        }
        JsonObject report =
                Json.createObjectBuilder().add("coverage", sourceFilesBuilder.build()).build();
        Json.createWriter(out).writeObject(report);
    }
}
