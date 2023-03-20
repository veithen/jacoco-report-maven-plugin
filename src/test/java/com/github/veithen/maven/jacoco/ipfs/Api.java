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
package com.github.veithen.maven.jacoco.ipfs;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.codehaus.plexus.util.IOUtil;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

@Path("api/v0")
public class Api {
    private static String loadResource(String name) {
        try (InputStream in = Api.class.getResourceAsStream(name)) {
            return IOUtil.toString(in, "UTF-8");
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    @Path("id")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String get() {
        return loadResource("id_response.json");
    }

    @Path("add")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String add(FormDataMultiPart multipart) {
        return loadResource("add_response.json");
    }
}
