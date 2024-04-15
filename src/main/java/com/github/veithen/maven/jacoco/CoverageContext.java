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
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.InputStreamSourceFileLocator;

public final class CoverageContext {
    private static final Pattern autoSessionIdPattern = Pattern.compile(".*-([0-9a-f]{1,8})");

    private final ExecFileLoader loader;
    private final IBundleCoverage bundle;
    private final Map<String, File> sourceFiles;
    private final Supplier<File> rootDirSupplier;

    CoverageContext(
            ExecFileLoader loader,
            IBundleCoverage bundle,
            Map<String, File> sourceFiles,
            Supplier<File> rootDirSupplier) {
        this.loader = loader;
        this.bundle = bundle;
        this.sourceFiles = sourceFiles;
        this.rootDirSupplier = rootDirSupplier;
    }

    public IBundleCoverage getBundle() {
        return bundle;
    }

    static SessionInfo anonymize(SessionInfo sessionInfo) {
        Matcher matcher = autoSessionIdPattern.matcher(sessionInfo.getId());
        if (matcher.matches()) {
            return new SessionInfo(
                    matcher.group(1),
                    sessionInfo.getStartTimeStamp(),
                    sessionInfo.getDumpTimeStamp());
        } else {
            return sessionInfo;
        }
    }

    public void visit(IReportVisitor visitor) throws IOException {
        visitor.visitInfo(
                loader.getSessionInfoStore().getInfos().stream()
                        .map(CoverageContext::anonymize)
                        .collect(Collectors.toList()),
                loader.getExecutionDataStore().getContents());
        // TODO: make encoding and tab with configurable
        visitor.visitBundle(
                bundle,
                new InputStreamSourceFileLocator("utf-8", 4) {
                    @Override
                    protected InputStream getSourceStream(String path) throws IOException {
                        File file = sourceFiles.get(path);
                        return file == null ? null : new FileInputStream(file);
                    }
                });
        visitor.visitEnd();
    }

    public Source lookupSource(ISourceFileCoverage sourceFileCoverage) {
        File sourceFile =
                sourceFiles.get(
                        sourceFileCoverage.getPackageName().replace('.', '/')
                                + "/"
                                + sourceFileCoverage.getName());
        return sourceFile == null ? null : new Source(sourceFile, rootDirSupplier);
    }
}
