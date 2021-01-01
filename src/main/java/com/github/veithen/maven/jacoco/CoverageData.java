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

import java.io.File;
import java.io.Serializable;
import java.util.Map;

class CoverageData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final File dataFile;
    private final File classes;
    private final Map<String, File> sources;

    CoverageData(File dataFile, File classes, Map<String, File> sources) {
        this.dataFile = dataFile;
        this.classes = classes;
        this.sources = sources;
    }

    File getDataFile() {
        return dataFile;
    }

    File getClasses() {
        return classes;
    }

    Map<String, File> getSources() {
        return sources;
    }
}
