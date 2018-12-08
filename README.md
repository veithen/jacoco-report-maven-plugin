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

When running on [Travis](https://travis-ci.org) this will automatically upload reports to coveralls.io and/or codecov.io if the project is enabled on one of these services. Note that public Github repositories are always enabled on codecov.io by default, so no further action is required for that service.

You might want to exclude the code from some modules from the coverage reports, e.g. modules that contain test utilities. In this case, set the `includeClasses` parameter to false for those modules:

    <plugin>
        <groupId>com.github.veithen.maven</groupId>
        <artifactId>jacoco-report-maven-plugin</artifactId>
        <configuration>
            <includeClasses>false</includeClasses>
        </configuration>
    </plugin>
