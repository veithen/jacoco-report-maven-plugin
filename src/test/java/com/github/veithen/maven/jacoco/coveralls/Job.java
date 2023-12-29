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
package com.github.veithen.maven.jacoco.coveralls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

@Path("api/v1/jobs")
public class Job {
    @Context ServletContext servletContext;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String post(
            @HeaderParam(HttpHeaders.USER_AGENT) String userAgent, FormDataMultiPart multipart)
            throws Exception {
        assertThat(userAgent).startsWith("com.github.veithen.maven:jacoco-report-maven-plugin/");
        FormDataBodyPart jsonFilePart = multipart.getField("json_file");
        assertThat(jsonFilePart).isNotNull();
        ContentDisposition contentDisposition = jsonFilePart.getContentDisposition();
        assertThat(contentDisposition.getFileName()).isNotNull();
        JsonObject jsonFile = jsonFilePart.getEntityAs(JsonObject.class);
        assertThat(jsonFile.getString("service_name", null)).isEqualTo("travis-ci");
        assertThat(jsonFile.getString("service_job_id", null)).isEqualTo("123456");
        JsonArray sourceFiles = jsonFile.getJsonArray("source_files");
        assertThat(sourceFiles).hasSize(1);
        JsonObject sourceFile = sourceFiles.getJsonObject(0);
        assertThat(sourceFile.getString("name", null))
                .isEqualTo("target/its/test1/bundle/src/main/java/test/HelloService.java");
        JsonArray coverage = sourceFile.getJsonArray("coverage");
        int coveredLines = 0;
        for (int i = 0; i < coverage.size(); i++) {
            coveredLines += coverage.getInt(i, 0);
        }
        assertThat(coveredLines).isEqualTo(2);
        try (OutputStream out =
                new FileOutputStream(
                        new File(
                                servletContext.getInitParameter("outputDirectory"),
                                "coveralls.json"))) {
            Json.createWriter(out).write(jsonFile);
        }
        return "{\"message\":\"Job #44.1\",\"url\":\"https://coveralls.io/jobs/42722376\"}";
    }
}
