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
package com.github.veithen.maven.jacoco.coveralls;

import static com.github.veithen.maven.jacoco.Retry.withRetry;

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.apache.maven.plugin.MojoFailureException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;

import com.github.veithen.maven.jacoco.ContinuousIntegrationContext;
import com.github.veithen.maven.jacoco.CoverageContext;
import com.github.veithen.maven.jacoco.CoverageService;
import com.github.veithen.maven.jacoco.ServiceMap;
import com.github.veithen.maven.jacoco.Source;

final class Coveralls implements CoverageService {
    private final WebTarget target;
    private final Map<String, String> serviceMap;

    Coveralls(WebTarget target) {
        this.target = target;
        serviceMap = ServiceMap.loadServiceMap("META-INF/coveralls-services.properties");
    }

    @Override
    public String getName() {
        return "Coveralls";
    }

    @Override
    public boolean isEnabled(ContinuousIntegrationContext ciContext) {
        if (ciContext == null || !serviceMap.containsKey(ciContext.getService())) {
            return false;
        }
        try {
            withRetry(
                    () ->
                            target.path("github/{user}/{repo}.json")
                                    .resolveTemplate("user", ciContext.getUser())
                                    .resolveTemplate("repo", ciContext.getRepository())
                                    .request()
                                    .accept(MediaType.APPLICATION_JSON_TYPE)
                                    // For newly enabled repositories, the API returns "null", i.e.
                                    // this needs to
                                    // be JsonValue, not JsonObject.
                                    .get(JsonValue.class));
            return true;
        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Override
    public String upload(ContinuousIntegrationContext ciContext, CoverageContext coverageContext)
            throws MojoFailureException {
        JsonArrayBuilder sourceFilesBuilder = Json.createArrayBuilder();
        for (IPackageCoverage packageCoverage : coverageContext.getBundle().getPackages()) {
            for (ISourceFileCoverage sourceFileCoverage : packageCoverage.getSourceFiles()) {
                Source source = coverageContext.lookupSource(sourceFileCoverage);
                if (source == null) {
                    continue;
                }
                JsonArrayBuilder coverageBuilder = Json.createArrayBuilder();
                for (int i = 1; i < sourceFileCoverage.getFirstLine(); i++) {
                    coverageBuilder.add(JsonValue.NULL);
                }
                for (int i = sourceFileCoverage.getFirstLine();
                        i <= sourceFileCoverage.getLastLine();
                        i++) {
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
                sourceFilesBuilder.add(
                        Json.createObjectBuilder()
                                .add("name", source.getPathRelativeToRepositoryRoot())
                                .add("source_digest", source.digest())
                                .add("coverage", coverageBuilder.build())
                                .build());
            }
        }
        JsonObjectBuilder jsonFileBuilder =
                Json.createObjectBuilder()
                        .add("service_name", serviceMap.get(ciContext.getService()));
        if (ciContext.getBuildId() != null) {
            jsonFileBuilder.add("service_number", ciContext.getBuildId());
        }
        if (ciContext.getBuildRunId() != null) {
            jsonFileBuilder.add("service_job_id", ciContext.getBuildRunId());
        }
        jsonFileBuilder.add("source_files", sourceFilesBuilder.build());
        JsonObject jsonFile = jsonFileBuilder.build();
        FormDataMultiPart multipart = new FormDataMultiPart();
        multipart.bodyPart(
                new FormDataBodyPart(
                        FormDataContentDisposition.name("json_file")
                                .fileName("coverage.json")
                                .build(),
                        jsonFile,
                        MediaType.APPLICATION_JSON_TYPE));
        return withRetry(
                () ->
                        target.path("api/v1/jobs")
                                .request()
                                .post(
                                        Entity.entity(multipart, multipart.getMediaType()),
                                        JsonObject.class)
                                .getString("url"));
    }
}
