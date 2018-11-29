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
package com.github.veithen.maven.jacoco.coveralls;

import static com.google.common.truth.Truth.assertThat;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

@Path("api/v1/jobs")
public class Job {
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String post(FormDataMultiPart multipart) {
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
        assertThat(sourceFile.getString("name", null)).isEqualTo("target/its/test1/bundle/src/main/java/test/HelloService.java");
        JsonArray coverage = sourceFile.getJsonArray("coverage");
        int coveredLines = 0;
        for (int i=0; i<coverage.size(); i++) {
            coveredLines += coverage.getInt(i, 0);
        }
        assertThat(coveredLines).isEqualTo(2);
        return "{\"message\":\"Job #44.1\",\"url\":\"https://coveralls.io/jobs/42722376\"}";
    }
}
