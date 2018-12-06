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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

final class ReportOutputStream extends ByteArrayOutputStream {
    private final FormDataMultiPart multipart;
    private final String path;

    ReportOutputStream(FormDataMultiPart multipart, String path) {
        this.multipart = multipart;
        this.path = path;
    }

    @Override
    public void close() throws IOException {
        multipart.bodyPart(new FormDataBodyPart(
                FormDataContentDisposition.name("file").fileName("report/" + path).build(),
                buf,
                MediaType.APPLICATION_OCTET_STREAM_TYPE));
    }
}
