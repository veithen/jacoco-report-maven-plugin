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
import java.io.OutputStream;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;

import org.jacoco.report.xml.XMLFormatter;

final class Codecov implements CoverageService {
    private final WebTarget target;

    Codecov(WebTarget target) {
        this.target = target;
    }

    @Override
    public String getName() {
        return "Codecov";
    }

    @Override
    public boolean isConfigured(TravisContext travisContext) {
        try {
            target.path(String.format("api/gh/%s", travisContext.getRepoSlug())).request().get();
            return true;
        } catch (NotFoundException ex) {
            return false;
        }
    }

    @Override
    public void upload(TravisContext travisContext, CoverageContext coverageContext) {
        System.out.println(target.path("upload/v2")
                .queryParam("service", "travis")
                .queryParam("slug", travisContext.getRepoSlug())
                .queryParam("job", travisContext.getJobId())
                .queryParam("commit", travisContext.getCommit())
                .request()
                .post(Entity.entity(
                        new StreamingOutput() {
                            @Override
                            public void write(OutputStream out) throws IOException {
                                coverageContext.visit(new XMLFormatter().createVisitor(out));
                            }
                        }, MediaType.TEXT_PLAIN), String.class));
    }
}
