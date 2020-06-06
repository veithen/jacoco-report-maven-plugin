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

public final class ContinuousIntegrationContext {
    private final String service;
    private final String repoSlug;
    private final String buildRunId;
    private final String buildId;
    private final String buildUrl;
    private final String branch;
    private final String commit;
    private final String pullRequest;

    public ContinuousIntegrationContext(String service, String repoSlug, String buildRunId, String buildId, String buildUrl, String branch, String commit, String pullRequest) {
        this.service = service;
        this.repoSlug = repoSlug;
        this.buildRunId = buildRunId;
        this.buildUrl = buildUrl;
        this.buildId = buildId;
        this.branch = branch;
        this.commit = commit;
        this.pullRequest = pullRequest;
    }

    public String getService() {
        return service;
    }

    public String getRepoSlug() {
        return repoSlug;
    }

    public String getUser() {
        return repoSlug.split("/")[0];
    }

    public String getRepository() {
        return repoSlug.split("/")[1];
    }

    /**
     * Get the build run ID. Unique for every build, i.e. changes when a build is re-run.
     */
    public String getBuildRunId() {
        return buildRunId;
    }

    /**
     * Get the build ID. Changes with every commit but doesn't change when a build is re-run.
     */
    public String getBuildId() {
        return buildId;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public String getBranch() {
        return branch;
    }

    public String getCommit() {
        return commit;
    }

    public String getPullRequest() {
        return pullRequest;
    }
}
