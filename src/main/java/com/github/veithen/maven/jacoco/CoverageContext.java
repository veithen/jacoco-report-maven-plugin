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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.InputStreamSourceFileLocator;

final class CoverageContext {
    private final ExecFileLoader loader;
    private final IBundleCoverage bundle;
    private final Map<String, File> sourceFiles;
    private final File rootDir;

    CoverageContext(ExecFileLoader loader, IBundleCoverage bundle, Map<String, File> sourceFiles, File rootDir) {
        this.loader = loader;
        this.bundle = bundle;
        this.sourceFiles = sourceFiles;
        this.rootDir = rootDir;
    }

    IBundleCoverage getBundle() {
        return bundle;
    }

    void visit(IReportVisitor visitor) throws IOException {
        visitor.visitInfo(
                loader.getSessionInfoStore().getInfos(),
                loader.getExecutionDataStore().getContents());
        // TODO: make encoding and tab with configurable
        visitor.visitBundle(bundle, new InputStreamSourceFileLocator("utf-8", 4) {
            @Override
            protected InputStream getSourceStream(String path) throws IOException {
                File file = sourceFiles.get(path);
                return file == null ? null : new FileInputStream(file);
            }
        });
        visitor.visitEnd();
    }

    Source lookupSource(ISourceFileCoverage sourceFileCoverage) {
        File sourceFile = sourceFiles.get(sourceFileCoverage.getPackageName().replace('.', '/') + "/" + sourceFileCoverage.getName());
        return sourceFile == null ? null : new Source(sourceFile, rootDir);
    }
}
