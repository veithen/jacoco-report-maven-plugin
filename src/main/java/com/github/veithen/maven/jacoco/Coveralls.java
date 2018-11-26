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

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.maven.plugin.MojoFailureException;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;

final class Coveralls implements CoverageService {
    static final Coveralls INSTANCE = new Coveralls();

    private Coveralls() {}

    @Override
    public String getName() {
        return "Coveralls";
    }

    @Override
    public boolean isConfigured(String repoSlug, HttpClient httpClient) throws IOException {
        HttpGet request = new HttpGet(String.format("https://coveralls.io/github/%s.json", repoSlug));
        return httpClient.execute(request).getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    }

    @Override
    public HttpResponse upload(String jobId, IBundleCoverage bundleCoverage, Sources sources, HttpClient httpClient) throws MojoFailureException, IOException {
        JsonArrayBuilder sourceFilesBuilder = Json.createArrayBuilder();
        for (IPackageCoverage packageCoverage : bundleCoverage.getPackages()) {
            for (ISourceFileCoverage sourceFileCoverage : packageCoverage.getSourceFiles()) {
                Source source = sources.lookup(sourceFileCoverage);
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
                .add("service_job_id", jobId)
                .add("source_files", sourceFilesBuilder.build())
                .build();
        HttpEntity entity = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addPart("json_file", new JsonContentBody(jsonFile, "coverage.json"))
                .build();
        HttpPost post = new HttpPost("https://coveralls.io/api/v1/jobs");
        post.setEntity(entity);
        return httpClient.execute(post);
    }
}
