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

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.BiConsumer;

import org.jacoco.report.IMultiReportOutput;

final class MultiReportOutput implements IMultiReportOutput {
    private final BiConsumer<String, byte[]> consumer;

    MultiReportOutput(BiConsumer<String, byte[]> consumer) {
        this.consumer = consumer;
    }

    @Override
    public OutputStream createFile(String path) throws IOException {
        return new ReportOutputStream(consumer, path);
    }

    @Override
    public void close() throws IOException {}
}