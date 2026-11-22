# AnnotationConfig

## What

AnnotationConfig is a "reflection-based Java object-to-configuration mapping library with extensible
serialization and format-specific resolution", or in simpler terms a library that "cooks"

```java
@Comment("Hello")
public class MyCoolConfig {

  private String custom = "My name is Ivan";

  @Comment("Configurable messages")
  private MessagesMap messages =
      new MessagesMap(
          "I usually get up at 7 o'clock",
          "And brush my teeth",
          "Then I have breakfast and go to school by bus.");

  record MessagesMap(String first, String second, String third) {
  }
}

```

and, generates the following config (YAML):

```yaml
# Hello

custom: "My name is Ivan"

# Configurable messages
messages:
  first: "I usually get up at 7 o'clock"
  second: "And brush my teeth"
  third: "Then I have breakfast and go to school by bus."
```

By default the supported configuration file formats are "properties", [JSON](https://www.json.org/)
using [gson](https://github.com/google/gson) ; [TOML](https://toml.io/)
utilising [Jackson data formats](https://github.com/FasterXML/jackson-dataformats-text) and last but
not least - [YAML](https://yaml.org/) utilising [snakeyaml](https://github.com/snakeyaml/snakeyaml)
for reading configuration files (writing for YAML is home-grown inside the `yaml` module).

# Prerequisites

Versions >=3.0.3 require Java 25, ~~internet access and maven central~~

# Examples

Examples are available in `examples/`. Newest additions (if examples are not updated) are mentioned
in the changelog.

[Example config](/examples/src/main/java/com/mrivanplays/annotationconfig/examples/ExampleConfig.java)

[Example load/dump](/examples/src/main/java/com/mrivanplays/annotationconfig/examples/ExampleLoadConfig.java)

[Example config output (YAML)](examples/application-config.yaml)

### About configuration resolvers

`ConfigResolver` is the abstraction layer between AnnotationConfig internals and a specific
configuration file format. Each module that AnnotationConfig ships by default has a standardised
class named `[Configuration type]Config` which consists of `getConfigResolver()` and (if applicable)
custom settings.

YAML example:

```java
YamlConfig.getConfigResolver().
      loadOrDump(anotatedConfig, file, /*opt:loader settings*/);
```

Due to this abstraction layer existing, every feature AnnotationConfig adds can be and is 99%
self-contained in the `core` module. Configuration file format modules only implement
`ConfigResolver` via its designated builder. This also means that you are capable of creating your
own resolver:

```java
// ...

ConfigResolver configResolver =
    ConfigResolver.newBuilder()
        .withValueWriter(/* value writer */)
        .withSetting(/* setting */, /* value */)
        .withCommentPrefix("# ") // if the conf spedc supports
        .withFileExtension(".myextension")
        .withFileExtension(".myotherextension")
        .withValueReader(/* value reader */)
        .build();

// ...

// make use of that resolver
configResolver.loadOrDump(
    annotatedConfig, file, /* loader settings */);
```

WHILE ON THAT NOTE: `ConfigResolver` is not meant to be implemented, only created using the designated builder. Implementing it means YOU are going to deal with the internal code which can break without a moment's notice. Oh, something doesn't work properly? It looks like YOU implemented it and YOU used the internals incorrectly, and it is entirely YOUR fault.

AnnotationConfig internals serialize to a `Map<String, Object>`, and for comments a
`Map<String, List<String>>` where the map key is the specified field name, or, if annotated with
`@Key`, the value from that annotation.

# Installation

Maven:

```xml
<dependency>
  <groupId>com.mrivanplays</groupId>
  <!-- Types: toml, yaml, json -->
  <!-- If you want .conf/.properties, or custom implementation configuration, you can set the type to core -->
  <artifactId>annotationconfig-(type)</artifactId> <!-- Replace type -->
  <version>3.0.4</version>
  <scope>compile</scope>
</dependency>
```

Gradle:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.mrivanplays:annotationconfig-(type):3.0.4")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
```

# License

(c) 2020-2026 Ivan Pekov and contributors
AnnotationConfig is licensed under MIT, available to read [here](LICENSE)