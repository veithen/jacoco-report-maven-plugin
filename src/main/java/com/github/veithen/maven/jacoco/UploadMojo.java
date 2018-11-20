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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.tools.ExecFileLoader;

import com.github.veithen.maven.shared.mojo.aggregating.AggregatingMojo;

@Mojo(name="upload", defaultPhase=LifecyclePhase.POST_INTEGRATION_TEST, threadSafe=true)
public final class UploadMojo extends AggregatingMojo<CoverageData> {
    private static final char[] HEX_CHARS;
    
    static {
        HEX_CHARS = new char[16];
        for (int i = 0; i < 10; i++) {
            HEX_CHARS[i] = (char)('0' + i);
        }
        for (int i = 0; i < 6; i++) {
            HEX_CHARS[10+i] = (char)('a' + i);
        }
    }

    @Parameter(defaultValue="${project.build.directory}/jacoco.exec", required=true)
    private File dataFile;

    public UploadMojo() {
        super(CoverageData.class);
    }

    @Override
    protected CoverageData doExecute() throws MojoExecutionException, MojoFailureException {
        if (dataFile.exists()) {
            Map<String, File> sources = new HashMap<>();
            for (String compileSourceRoot : project.getCompileSourceRoots()) {
                File basedir = new File(compileSourceRoot);
                if (basedir.exists()) {
                    DirectoryScanner scanner = new DirectoryScanner();
                    scanner.setBasedir(basedir);
                    scanner.scan();
                    for (String includedFile : scanner.getIncludedFiles()) {
                        sources.put(includedFile, new File(basedir, includedFile));
                    }
                }
            }
            return new CoverageData(dataFile, project.getArtifact().getFile(), sources);
        } else {
            return null;
        }
    }

    private File findRootDir() throws MojoExecutionException {
        File rootDir = project.getBasedir();
        while (!new File(rootDir, ".git").exists()) {
            rootDir = rootDir.getParentFile();
            if (rootDir == null) {
                throw new MojoExecutionException("Root directory not found; are we running from a Git clone?");
            }
        }
        return rootDir;
    }

    private static String makeRelative(File file, File rootDir) {
        Deque<String> components = new LinkedList<>();
        while (!file.equals(rootDir)) {
            components.addFirst(file.getName());
            file = file.getParentFile();
            if (file == null) {
                throw new IllegalArgumentException(String.format("%s is not a descendant of %s", file, rootDir));
            }
        }
        return String.join("/", components);
    }

    private static String digest(File file) throws MojoFailureException {
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
        byte[] digestData = digest.digest();
        StringBuilder sb = new StringBuilder(digestData.length * 2);
        for (byte b : digestData) {
            sb.append(HEX_CHARS[(b >> 4) & 0xF]);
            sb.append(HEX_CHARS[b & 0xF]);
        }
        return sb.toString();
    }

    @Override
    protected void doAggregate(List<CoverageData> results) throws MojoExecutionException, MojoFailureException {
        ExecFileLoader loader = new ExecFileLoader();
        for (CoverageData coverageData : results) {
            try {
                loader.load(coverageData.getDataFile());
            } catch (IOException ex) {
                throw new MojoExecutionException(String.format("Failed to load exec file %s: %s", coverageData.getDataFile(), ex.getMessage()), ex);
            }
        }
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
        for (CoverageData coverageData : results) {
            try {
                analyzer.analyzeAll(coverageData.getClasses());
            } catch (IOException ex) {
                throw new MojoExecutionException(String.format("Failed to analyze %s: %s", coverageData.getClasses(), ex.getMessage()), ex);
            }
        }
        File rootDir = findRootDir();
        Map<String, File> sources = new HashMap<>();
        results.stream().map(CoverageData::getSources).forEach(sources::putAll);
        IBundleCoverage bundle = builder.getBundle("Coverage Report");
        JsonArrayBuilder sourceFilesBuilder = Json.createArrayBuilder();
        for (IPackageCoverage packageCoverage : bundle.getPackages()) {
            for (ISourceFileCoverage sourceFileCoverage : packageCoverage.getSourceFiles()) {
                File sourceFile = sources.get(sourceFileCoverage.getPackageName().replace('.', '/') + "/" + sourceFileCoverage.getName());
                if (sourceFile == null) {
                    break;
                }
                JsonArrayBuilder coverageBuilder = Json.createArrayBuilder();
                for (int i=0; i<sourceFileCoverage.getFirstLine(); i++) {
                    coverageBuilder.add(JsonValue.NULL);
                }
                for (int i=sourceFileCoverage.getFirstLine(); i<=sourceFileCoverage.getLastLine(); i++) {
                    switch (sourceFileCoverage.getLine(i).getStatus()) {
                        case ICounter.EMPTY:
                            coverageBuilder.add(JsonValue.NULL);
                            break;
                        case ICounter.NOT_COVERED:
                            coverageBuilder.add(0);
                            break;
                        default:
                            coverageBuilder.add(1);
                    }
                }
                sourceFilesBuilder.add(Json.createObjectBuilder()
                        .add("name", makeRelative(sourceFile, rootDir))
                        .add("source_digest", digest(sourceFile))
                        .add("coverage", coverageBuilder.build())
                        .build());
            }
        }
        JsonWriter out = Json.createWriter(System.out);
        out.write(sourceFilesBuilder.build());
    }
}
