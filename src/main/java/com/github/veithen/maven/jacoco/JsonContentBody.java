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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;

import org.apache.http.entity.ContentType;

final class JsonContentBody extends StreamableContentBody {
    private final JsonObject jsonObject;

    JsonContentBody(JsonObject jsonObject, String filename) {
        super(ContentType.create("application/json", "utf-8"), filename);
        this.jsonObject = jsonObject;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        JsonWriter writer = Json.createWriter(out);
        writer.write(jsonObject);
    }
}
