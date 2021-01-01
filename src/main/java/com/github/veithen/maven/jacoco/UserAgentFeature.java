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
package com.github.veithen.maven.jacoco;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;

import org.codehaus.plexus.util.IOUtil;

public final class UserAgentFeature implements Feature {
    @Override
    public boolean configure(FeatureContext context) {
        if (context.getConfiguration().getRuntimeType() == RuntimeType.CLIENT) {
            String userAgent;
            try (InputStream in = UserAgentFeature.class.getResourceAsStream("user-agent")) {
                userAgent = IOUtil.toString(in, "UTF-8");
            } catch (IOException ex) {
                throw new Error(ex);
            }
            context.register(
                    new ClientRequestFilter() {
                        @Override
                        public void filter(ClientRequestContext requestContext) throws IOException {
                            requestContext
                                    .getHeaders()
                                    .putSingle(HttpHeaders.USER_AGENT, userAgent);
                        }
                    });
            return true;
        } else {
            return false;
        }
    }
}
