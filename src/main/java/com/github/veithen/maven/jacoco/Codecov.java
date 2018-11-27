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

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.maven.plugin.MojoFailureException;
import org.jacoco.report.xml.XMLFormatter;

final class Codecov implements CoverageService {
    static final Codecov INSTANCE = new Codecov();

    private Codecov() {}

    @Override
    public String getName() {
        return "Codecov";
    }

    @Override
    public boolean isConfigured(String repoSlug, HttpClient httpClient) throws IOException {
        HttpGet request = new HttpGet(String.format("https://codecov.io/api/gh/%s", repoSlug));
        return httpClient.execute(request).getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    }

    @Override
    public HttpResponse upload(String jobId, Context context, HttpClient httpClient) throws MojoFailureException, IOException {
        HttpPost post = new HttpPost(String.format("https://codecov.io/upload/v2?service=travis&job=%s", jobId));
        post.setEntity(new StreamableHttpEntity("text/plain") {
            @Override
            public void writeTo(OutputStream out) throws IOException {
                context.visit(new XMLFormatter().createVisitor(out));
            }
        });
        return httpClient.execute(post);
    }
}
