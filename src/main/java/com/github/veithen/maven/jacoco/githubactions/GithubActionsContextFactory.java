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
package com.github.veithen.maven.jacoco.githubactions;

import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;

import com.github.veithen.maven.jacoco.ContinuousIntegrationContext;
import com.github.veithen.maven.jacoco.ContinuousIntegrationContextFactory;

@Component(role = ContinuousIntegrationContextFactory.class, hint = "github-actions")
public class GithubActionsContextFactory implements ContinuousIntegrationContextFactory {
    @Override
    public ContinuousIntegrationContext createContext(Map<String, String> env) {
        String action = env.get("GITHUB_ACTION");
        String repoSlug = env.get("GITHUB_REPOSITORY");
        String runId = env.get("GITHUB_RUN_ID");
        String ref = env.get("GITHUB_REF");
        String commit = env.get("GITHUB_SHA");
        if (action != null && repoSlug != null && runId != null && ref != null && commit != null) {
            String branch;
            String pullRequest;
            if (ref.startsWith("refs/heads/")) {
                branch = ref.substring(11);
                pullRequest = null;
            } else if (ref.startsWith("refs/pull/") && ref.endsWith("/merge")) {
                branch = env.get("GITHUB_HEAD_REF");
                if (branch == null) {
                    return null;
                }
                pullRequest = ref.substring(10, ref.length() - 6);
            } else {
                return null;
            }
            return new ContinuousIntegrationContext(
                    "github-actions",
                    repoSlug,
                    null,
                    runId,
                    String.format("http://github.com/%s/actions/runs/%s", repoSlug, runId),
                    branch,
                    commit,
                    pullRequest);
        } else {
            return null;
        }
    }
}
