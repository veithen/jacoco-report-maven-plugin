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
package com.github.veithen.maven.jacoco.ipfs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.BiConsumer;

final class ReportOutputStream extends ByteArrayOutputStream {
    private final BiConsumer<String, byte[]> consumer;
    private final String path;

    ReportOutputStream(BiConsumer<String, byte[]> consumer, String path) {
        this.consumer = consumer;
        this.path = path;
    }

    @Override
    public void close() throws IOException {
        consumer.accept(path, toByteArray());
    }
}
