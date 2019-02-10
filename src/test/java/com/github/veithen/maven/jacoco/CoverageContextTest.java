/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 - 2019 Andreas Veithen
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

import static com.google.common.truth.Truth.assertThat;

import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.runtime.AbstractRuntime;
import org.junit.Test;

public class CoverageContextTest {
    @Test
    public void testAnonymize() {
        SessionInfo sessionInfo = new SessionInfo("host-" + AbstractRuntime.createRandomId(), 0, 0);
        SessionInfo anonymizedSessionInfo = CoverageContext.anonymize(sessionInfo);
        assertThat(anonymizedSessionInfo).isNotSameAs(sessionInfo);
        assertThat(anonymizedSessionInfo.getId()).doesNotContain("host");
    }
}
