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
package com.github.veithen.maven.jacoco.travis;

import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;

import com.github.veithen.maven.jacoco.ContinuousIntegrationContext;
import com.github.veithen.maven.jacoco.ContinuousIntegrationContextFactory;

@Component(role=ContinuousIntegrationContextFactory.class, hint="travis")
public class TravisContextFactory implements ContinuousIntegrationContextFactory {
    @Override
    public ContinuousIntegrationContext createContext(Map<String,String> env) {
        String repoSlug = env.get("TRAVIS_REPO_SLUG");
        String jobId = env.get("TRAVIS_JOB_ID");
        String jobNumber = env.get("TRAVIS_JOB_NUMBER");
        String jobUrl = env.get("TRAVIS_JOB_WEB_URL");
        String branch = env.get("TRAVIS_BRANCH");
        String commit = env.get("TRAVIS_COMMIT");
        if (repoSlug != null && jobId != null && jobNumber != null && jobUrl != null && branch != null && commit != null) {
            return new ContinuousIntegrationContext("travis", repoSlug, jobId, jobNumber, jobUrl, branch, commit);
        } else {
            return null;
        }
    }
}
