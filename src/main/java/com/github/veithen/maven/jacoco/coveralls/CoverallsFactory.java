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
package com.github.veithen.maven.jacoco.coveralls;

import java.util.Optional;

import jakarta.ws.rs.client.Client;

import org.codehaus.plexus.component.annotations.Component;

import com.github.veithen.maven.jacoco.CoverageService;
import com.github.veithen.maven.jacoco.CoverageServiceFactory;

@Component(role = CoverageServiceFactory.class, hint = "coveralls")
public final class CoverallsFactory implements CoverageServiceFactory {
    @Override
    public CoverageService newInstance(Client client, Optional<String> apiEndpoint) {
        return new Coveralls(client.target(apiEndpoint.orElse("https://coveralls.io")));
    }
}
