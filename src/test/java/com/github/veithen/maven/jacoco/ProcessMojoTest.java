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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

public class ProcessMojoTest {
    @Test
    public void testProcessException() {
        JsonObject content =
                Json.createReader(
                                new StringReader(
                                        "{\"message\":\"JSON file not found or failed to parse.\",\"error\":true,\"url\":\"\"}"))
                        .readObject();
        Response response = mock(Response.class);
        when(response.readEntity(JsonObject.class)).thenReturn(content);
        when(response.getStatusInfo()).thenReturn(Response.Status.INTERNAL_SERVER_ERROR);
        MojoFailureException exception =
                ProcessMojo.processException("Foobar", new WebApplicationException(response));
        assertThat(exception.getMessage())
                .isEqualTo(
                        "Failed to send request to Foobar: JSON file not found or failed to parse.");
    }
}
