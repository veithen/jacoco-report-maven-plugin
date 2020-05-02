/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 - 2020 Andreas Veithen
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

import static com.github.veithen.maven.jacoco.Retry.withRetry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;

final class Codecov implements CoverageService {
    private final WebTarget target;

    Codecov(WebTarget target) {
        this.target = target;
    }

    @Override
    public String getName() {
        return "Codecov";
    }

    @Override
    public boolean isEnabled(TravisContext travisContext) {
        if (travisContext == null) {
            return false;
        }
        try {
            withRetry(() -> target.path("api/gh/{user}/{repo}")
                    .resolveTemplate("user", travisContext.getUser())
                    .resolveTemplate("repo", travisContext.getRepository())
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonObject.class));
            return true;
        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Override
    public String upload(TravisContext travisContext, CoverageContext coverageContext) {
        // Use JSON reporting because source file locations can't be properly resolved
        // from a JaCoCo XML report.
        JsonObjectBuilder sourceFilesBuilder = Json.createObjectBuilder();
        for (IPackageCoverage packageCoverage : coverageContext.getBundle().getPackages()) {
            for (ISourceFileCoverage sourceFileCoverage : packageCoverage.getSourceFiles()) {
                Source source = coverageContext.lookupSource(sourceFileCoverage);
                if (source == null) {
                    continue;
                }
                JsonObjectBuilder coverageBuilder = Json.createObjectBuilder();
                for (int i=sourceFileCoverage.getFirstLine(); i<=sourceFileCoverage.getLastLine(); i++) {
                    ILine line = sourceFileCoverage.getLine(i);
                    if (line.getStatus() == ICounter.EMPTY) {
                        continue;
                    }
                    ICounter branchCounter = line.getBranchCounter();
                    String value;
                    if (branchCounter.getTotalCount() > 0) {
                        value = String.format("%s/%s", branchCounter.getCoveredCount(), branchCounter.getTotalCount());
                    } else if (line.getStatus() == ICounter.NOT_COVERED) {
                        value = "0";
                    } else {
                        value = "1";
                    }
                    coverageBuilder.add(String.valueOf(i), value);
                }
                sourceFilesBuilder.add(source.getPathRelativeToRepositoryRoot(), coverageBuilder.build());
            }
        }
        JsonObject report = Json.createObjectBuilder().add("coverage", sourceFilesBuilder.build()).build();
        target.path("upload/v2")
                .queryParam("service", "travis")
                .queryParam("slug", travisContext.getRepoSlug())
                .queryParam("job", travisContext.getJobId())
                .queryParam("build", travisContext.getJobNumber())
                .queryParam("build_url", travisContext.getJobUrl())
                .queryParam("branch", travisContext.getBranch())
                .queryParam("commit", travisContext.getCommit())
                .request()
                .post(Entity.entity(report, MediaType.APPLICATION_JSON_TYPE), String.class);
        return target.path("gh/{user}/{repo}/tree/{commit}")
                .resolveTemplate("user", travisContext.getUser())
                .resolveTemplate("repo", travisContext.getRepository())
                .resolveTemplate("commit", travisContext.getCommit())
                .getUri()
                .toString();
    }
}
