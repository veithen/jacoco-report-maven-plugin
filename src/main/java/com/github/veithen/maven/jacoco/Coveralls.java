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

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.maven.plugin.MojoFailureException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;

final class Coveralls implements CoverageService {
    private final WebTarget target;

    Coveralls(WebTarget target) {
        this.target = target;
    }

    @Override
    public String getName() {
        return "Coveralls";
    }

    @Override
    public boolean isEnabled(TravisContext travisContext) {
        if (travisContext == null) {
            return false;
        }
        try {
            target.path("github/{user}/{repo}.json")
                    .resolveTemplate("user", travisContext.getUser())
                    .resolveTemplate("repo", travisContext.getRepository())
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(JsonObject.class);
            return true;
        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Override
    public String upload(TravisContext travisContext, CoverageContext coverageContext) throws MojoFailureException {
        JsonArrayBuilder sourceFilesBuilder = Json.createArrayBuilder();
        for (IPackageCoverage packageCoverage : coverageContext.getBundle().getPackages()) {
            for (ISourceFileCoverage sourceFileCoverage : packageCoverage.getSourceFiles()) {
                Source source = coverageContext.lookupSource(sourceFileCoverage);
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
                .add("service_job_id", travisContext.getJobId())
                .add("source_files", sourceFilesBuilder.build())
                .build();
        FormDataMultiPart multipart = new FormDataMultiPart();
        multipart.bodyPart(new FormDataBodyPart(
                FormDataContentDisposition.name("json_file").fileName("coverage.json").build(),
                jsonFile,
                MediaType.APPLICATION_JSON_TYPE));
        return target.path("api/v1/jobs")
                .request()
                .post(Entity.entity(multipart, multipart.getMediaType()), JsonObject.class)
                .getString("url");
    }
}
