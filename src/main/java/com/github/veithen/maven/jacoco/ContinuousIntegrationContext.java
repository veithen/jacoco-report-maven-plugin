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
    private final String repoSlug;
    private final String jobId;
    private final String jobNumber;
    private final String jobUrl;
    private final String branch;
    private final String commit;

    public ContinuousIntegrationContext(String repoSlug, String jobId, String jobNumber, String jobUrl, String branch, String commit) {
        this.repoSlug = repoSlug;
        this.jobId = jobId;
        this.jobUrl = jobUrl;
        this.jobNumber = jobNumber;
        this.branch = branch;
        this.commit = commit;
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

    public String getJobId() {
        return jobId;
    }

    public String getJobNumber() {
        return jobNumber;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public String getBranch() {
        return branch;
    }

    public String getCommit() {
        return commit;
    }
}
