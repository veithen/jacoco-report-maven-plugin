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

import java.io.IOException;
import java.net.ConnectException;
import java.util.Locale;

import javax.json.JsonObject;
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
    private final WebTarget target;

    Ipfs(WebTarget target) {
        this.target = target;
    }

    @Override
    public String getName() {
        return "IPFS";
    }

    @Override
    public boolean isEnabled(TravisContext travisContext) {
        try {
            target.path("api/v0/id").request(MediaType.APPLICATION_JSON_TYPE).get(JsonObject.class);
            return true;
        } catch (ProcessingException ex) {
            if (ex.getCause() instanceof ConnectException) {
                return false;
            } else {
                throw ex;
            }
        }
    }

    @Override
    public void upload(TravisContext travisContext, CoverageContext coverageContext) throws MojoFailureException {
        FormDataMultiPart multipart = new FormDataMultiPart();
        multipart.bodyPart(new FormDataBodyPart(
                FormDataContentDisposition.name("file").fileName("report").build(),
                new byte[0],
                new MediaType("application", "x-directory")));
        HTMLFormatter htmlFormatter = new HTMLFormatter();
        htmlFormatter.setOutputEncoding("utf-8");
        htmlFormatter.setLocale(Locale.ENGLISH);
        try {
            coverageContext.visit(htmlFormatter.createVisitor(new MultiReportOutput(multipart)));
        } catch (IOException ex) {
            throw new MojoFailureException(String.format("Failed to generate coverage report: %s", ex.getMessage()), ex);
        }
        System.out.println(target.path("api/v0/add")
                // https://stackoverflow.com/questions/37580093/ipfs-add-returns-2-jsons
                .queryParam("progress", "false")
                .request(MediaType.APPLICATION_JSON_TYPE)
                // https://github.com/ipfs/java-ipfs-http-client/commit/2d1ffbcf6643e460ee1ba9581358f4735e954f09
                .header("Expect", "100-continue")
                .post(Entity.entity(multipart, multipart.getMediaType()), String.class));
    }
}
