/*-
 * #%L
 * jacoco-report-maven-plugin
 * %%
 * Copyright (C) 2018 - 2024 Andreas Veithen
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Supplier;

import org.apache.commons.codec.binary.Hex;
import org.apache.maven.plugin.MojoFailureException;

public final class Source {
    private final File file;
    private final Supplier<File> rootDirSupplier;

    Source(File file, Supplier<File> rootDir) {
        this.file = file;
        this.rootDirSupplier = rootDir;
    }

    public String getPathRelativeToRepositoryRoot() {
        File rootDir = rootDirSupplier.get();
        File file = this.file;
        Deque<String> components = new LinkedList<>();
        while (!file.equals(rootDir)) {
            components.addFirst(file.getName());
            file = file.getParentFile();
            if (file == null) {
                throw new IllegalArgumentException(
                        String.format("%s is not a descendant of %s", this.file, rootDir));
            }
        }
        return String.join("/", components);
    }

    public String digest() throws MojoFailureException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new MojoFailureException("Failed to instantiate message digest", ex);
        }
        byte[] buffer = new byte[4096];
        try (FileInputStream in = new FileInputStream(file)) {
            int c;
            while ((c = in.read(buffer)) != -1) {
                digest.update(buffer, 0, c);
            }
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to compute checksum", ex);
        }
        return Hex.encodeHexString(digest.digest(), true);
    }
}
