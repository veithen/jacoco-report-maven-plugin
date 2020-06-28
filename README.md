# jacoco-report-maven-plugin

This Maven plugin processes JaCoCo execution data and uploads coverage reports to [coveralls.io](https://coveralls.io) and/or [codecov.io](https://codecov.io). It can also add standard JaCoCo HTML reports to [IPFS](https://ipfs.io). The main difference with respect to other tools is that it correctly computes cross-module coverage in multi-module Maven builds.

To use the plugin, add the following to your project's root POM:

    <plugin>
        <groupId>com.github.veithen.maven</groupId>
        <artifactId>jacoco-report-maven-plugin</artifactId>
        <version>x.y.z</version>
        <executions>
            <execution>
                <goals>
                    <goal>process</goal>
                </goals>
            </execution>
        </executions>
    </plugin>

This would be used in conjunction with the standard jacoco-maven-plugin to generate the execution data that is processed by jacoco-report-maven-plugin:

    <plugin>
        <groupId>org.jacoco</groupId>
        <artifactId>jacoco-maven-plugin</artifactId>
        <version>0.8.2</version>
        <executions>
            <execution>
                <goals>
                    <goal>prepare-agent</goal>
                </goals>
            </execution>
        </executions>
    </plugin>

When running on [Travis](https://travis-ci.org) or Github Actions this will automatically upload reports to coveralls.io and/or codecov.io if the project is enabled on one of these services. Note that public Github repositories are always enabled on codecov.io by default, so no further action is required for that service.

You might want to exclude the code from some modules from the coverage reports, e.g. modules that contain test utilities. In this case, set the `includeClasses` parameter to false for those modules:

    <plugin>
        <groupId>com.github.veithen.maven</groupId>
        <artifactId>jacoco-report-maven-plugin</artifactId>
        <configuration>
            <includeClasses>false</includeClasses>
        </configuration>
    </plugin>

If your project provides a Maven plugin and has integration tests set up with [maven-invoker-plugin](https://maven.apache.org/plugins/maven-invoker-plugin/), use the following configuration to collect coverage data from those integration tests:

    <pluginManagement>
        <plugins>
            <plugin>
                <artifactId>maven-invoker-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mavenOpts>${argLine}</mavenOpts>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
