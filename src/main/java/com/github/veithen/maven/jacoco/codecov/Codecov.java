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
package com.github.veithen.maven.jacoco.codecov;

import static com.github.veithen.maven.jacoco.Retry.withRetry;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;

import com.github.veithen.maven.jacoco.ContinuousIntegrationContext;
import com.github.veithen.maven.jacoco.CoverageContext;
import com.github.veithen.maven.jacoco.CoverageService;
import com.github.veithen.maven.jacoco.ServiceMap;
import com.github.veithen.maven.jacoco.Source;

final class Codecov implements CoverageService {
    private final WebTarget target;
    private final Client client;
    private final Map<String, String> serviceMap;

    Codecov(WebTarget target, Client client) {
        this.target = target;
        this.client = client;
        serviceMap = ServiceMap.loadServiceMap("META-INF/codecov-services.properties");
    }

    @Override
    public String getName() {
        return "Codecov";
    }

    @Override
    public boolean isEnabled(ContinuousIntegrationContext ciContext) {
        if (ciContext == null || !serviceMap.containsKey(ciContext.getService())) {
            return false;
        }
        try {
            withRetry(
                    () ->
                            target.path("api/gh/{user}/{repo}")
                                    .resolveTemplate("user", ciContext.getUser())
                                    .resolveTemplate("repo", ciContext.getRepository())
                                    .request()
                                    .accept(MediaType.APPLICATION_JSON_TYPE)
                                    .get(JsonObject.class));
            return true;
        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Override
    public String upload(ContinuousIntegrationContext ciContext, CoverageContext coverageContext) {
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
        String[] responseParts =
                withRetry(
                                () ->
                                        target.path("upload/v4")
                                                .queryParam(
                                                        "service",
                                                        serviceMap.get(ciContext.getService()))
                                                .queryParam("slug", ciContext.getRepoSlug())
                                                .queryParam("job", ciContext.getBuildRunId())
                                                .queryParam("build", ciContext.getBuildId())
                                                .queryParam("build_url", ciContext.getBuildUrl())
                                                .queryParam("branch", ciContext.getBranch())
                                                .queryParam("commit", ciContext.getCommit())
                                                .queryParam("pr", ciContext.getPullRequest())
                                                .request()
                                                .accept(MediaType.TEXT_PLAIN)
                                                .post(
                                                        Entity.entity("", MediaType.TEXT_PLAIN),
                                                        String.class))
                        .split("\\r?\\n");
        withRetry(
                () ->
                        client.target(responseParts[1])
                                .request()
                                .put(
                                        Entity.entity(report, MediaType.APPLICATION_JSON_TYPE),
                                        String.class));
        return responseParts[0];
    }
}
