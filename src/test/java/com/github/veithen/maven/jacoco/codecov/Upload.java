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
package com.github.veithen.maven.jacoco.codecov;

import static com.google.common.truth.Truth.assertThat;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("upload/v2")
public class Upload {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String post(
            @QueryParam("service") String service,
            @QueryParam("slug") String slug,
            @QueryParam("job") String jobId,
            @QueryParam("commit") String commit,
            String body) {
        assertThat(service).isEqualTo("travis");
        assertThat(slug).isEqualTo("dummy/test");
        assertThat(jobId).isEqualTo("123456");
        assertThat(commit).isEqualTo("4d4f3aba8752b5147fc56d6502b9eb6dcde8aa33");
        System.out.println(body);
        return "OK";
    }
}