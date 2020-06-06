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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.ConnectException;
import java.util.Locale;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.maven.plugin.MojoFailureException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jacoco.report.html.HTMLFormatter;

final class Ipfs implements CoverageService {
    private static final String ROOT_DIR = "report";

    private final WebTarget target;

    Ipfs(WebTarget target) {
        this.target = target;
    }

    @Override
    public String getName() {
        return "IPFS";
    }

    @Override
    public boolean isEnabled(ContinuousIntegrationContext ciContext) {
        try {
            target.path("api/v0/id").request(MediaType.APPLICATION_JSON_TYPE).get(JsonObject.class);
            return true;
        } catch (ProcessingException ex) {
            if (ex.getCause() instanceof ConnectException) {
                return false;
            } else {
                throw ex;
            }
        } catch (BadRequestException ex) {
            // On Jenkins, port 5001 can be taken by the slave agent and the request fails with status 400.
            return false;
        }
    }

    @Override
    public String upload(ContinuousIntegrationContext ciContext, CoverageContext coverageContext) throws MojoFailureException {
        FormDataMultiPart multipart = new FormDataMultiPart();
        multipart.bodyPart(new FormDataBodyPart(
                FormDataContentDisposition.name("file").fileName(ROOT_DIR).build(),
                new byte[0],
                new MediaType("application", "x-directory")));
        HTMLFormatter htmlFormatter = new HTMLFormatter();
        htmlFormatter.setOutputEncoding("utf-8");
        htmlFormatter.setLocale(Locale.ENGLISH);
        try {
            coverageContext.visit(htmlFormatter.createVisitor(new MultiReportOutput((path, content) -> {
                multipart.bodyPart(new FormDataBodyPart(
                        FormDataContentDisposition.name("file").fileName(ROOT_DIR + "/" + path).build(),
                        content,
                        MediaType.APPLICATION_OCTET_STREAM_TYPE));
            })));
        } catch (IOException ex) {
            throw new MojoFailureException(String.format("Failed to generate coverage report: %s", ex.getMessage()), ex);
        }
        // The response isn't proper JSON. It's a sequence of JSON objects, one per line.
        String response = target.path("api/v0/add")
                // https://stackoverflow.com/questions/37580093/ipfs-add-returns-2-jsons
                .queryParam("progress", "false")
                .request(MediaType.APPLICATION_JSON_TYPE)
                // https://github.com/ipfs/java-ipfs-http-client/commit/2d1ffbcf6643e460ee1ba9581358f4735e954f09
                .header("Expect", "100-continue")
                .post(Entity.entity(multipart, multipart.getMediaType()), String.class);
        try (BufferedReader reader = new BufferedReader(new StringReader(response))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonObject object = Json.createReader(new StringReader(line)).readObject();
                if (object.getString("Name").equals(ROOT_DIR)) {
                    return String.format("https://ipfs.io/ipfs/%s/index.html", object.getString("Hash"));
                }
            }
        } catch (IOException ex) {
            // We should never get here because we are doing I/O on a StringReader.
            throw new Error(ex);
        }
        throw new MojoFailureException("Failed to extract hash from IPFS response");
    }
}
