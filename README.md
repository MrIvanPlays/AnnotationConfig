![license](https://img.shields.io/github/license/MrIvanPlays/AnnotationConfig.svg?style=for-the-badge)
![issues](https://img.shields.io/github/issues/MrIvanPlays/AnnotationConfig.svg?style=for-the-badge)
![api version](https://img.shields.io/maven-metadata/v?color=%20blue&label=latest%20version&metadataUrl=https%3A%2F%2Frepo.mrivanplays.com%2Frepository%2Fivan%2Fcom%2Fmrivanplays%2Fannotationconfig-core%2Fmaven-metadata.xml&style=for-the-badge)
[![support](https://img.shields.io/discord/493674712334073878.svg?colorB=Blue&logo=discord&label=Support&style=for-the-badge)](https://mrivanplays.com/discord)

# AnnotationConfig

Make configurations with ease

JavaDocs:
- [core](https://mrivanplays.com/javadocs/annotationconfig/core/com/mrivanplays/annotationconfig/core/package-summary.html)
- [toml](https://mrivanplays.com/javadocs/annotationconfig/toml/com/mrivanplays/annotationconfig/toml/package-summary.html)
- [yaml](https://mrivanplays.com/javadocs/annotationconfig/yaml/com/mrivanplays/annotationconfig/yaml/package-summary.html)

# Usage examples
Can be found in the tests in each of the modules

# Installation

Maven:
```xml

    <build>
        <plugins>
            <plugin>
                <version>3.7.0</version>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <compilerArgs>
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.1.1</version>
                <configuration>
                    <relocations>
                        <!-- Relocating is only necessary if you're shading for other library addition -->
                        <relocation>
                            <pattern>com.mrivanplays.annotationconfig</pattern>
                            <shadedPattern>[YOUR PLUGIN PACKAGE].annotationconfig</shadedPattern> <!-- Replace this -->
                        </relocation>
                    </relocations>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <id>ivan</id>
            <url>https://repo.mrivanplays.com/repository/ivan/</url>
        </repository>
    </repositories>

    <dependency>
        <groupId>com.mrivanplays</groupId>
        <!-- Types: toml, yaml -->
        <!-- If you want .conf/.properties configuration, you can set the type to core -->
        <artifactId>annotationconfig-(type)</artifactId> <!-- Replace type -->
        <version>VERSION</version> <!-- Replace with latest version -->
        <scope>compile</scope>
    </dependency>
```