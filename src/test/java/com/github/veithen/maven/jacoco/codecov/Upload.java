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

import static com.google.common.truth.Truth.assertThat;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

@Path("upload/v4")
public class Upload {
    @Context ServletContext servletContext;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String post(
            @QueryParam("service") String service,
            @QueryParam("slug") String slug,
            @QueryParam("job") String job,
            @QueryParam("build") String build,
            @QueryParam("build_url") String buildUrl,
            @QueryParam("commit") String commit,
            @QueryParam("branch") String branch,
            @Context UriInfo uriInfo,
            String body) {
        assertThat(service).isEqualTo("travis");
        assertThat(slug).isEqualTo("dummy/test");
        assertThat(job).isEqualTo("123456");
        assertThat(build).isEqualTo("45.1");
        assertThat(buildUrl).isEqualTo("https://travis-ci.org/dummy/test/jobs/123456");
        assertThat(commit).isEqualTo("4d4f3aba8752b5147fc56d6502b9eb6dcde8aa33");
        assertThat(branch).isEqualTo("master");
        return "http://foobar\n" + uriInfo.getBaseUriBuilder().path("storage").toString();
    }
}
