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

and generates the following config (YAML):

```yaml
# Hello

custom: "My name is Ivan"

# Configurable messages
messages:
  first: "I usually get up at 7 o'clock"
  second: "And brush my teeth"
  third: "Then I have breakfast and go to school by bus."
```

By default the supported configuration file formats are "properties"; [JSON](https://www.json.org/)
using [gson](https://github.com/google/gson), and [JSON5](https://spec.json5.org) (JSON with
comments) in the same module; [TOML](https://toml.io/), read
with [Jackson data formats](https://github.com/FasterXML/jackson-dataformats-text) and written by
AnnotationConfig itself; and last but not least [YAML](https://yaml.org/), read and written
with [snakeyaml](https://github.com/snakeyaml/snakeyaml).

# Prerequisites

Versions >=3.0.3 require Java 25.

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
The only exception to that rule is `JsonConfig`, as it has two resolvers for two JSON specs (good
ol' JSON and JSON5).

YAML example:

```java
YamlConfig.getConfigResolver().loadOrDump(annotatedConfig, file);
// or, with settings of your own:
YamlConfig.getConfigResolver().loadOrDump(annotatedConfig, file, settings);
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
        .withCommentPrefix("# ") // if the config format supports comments
        .withFileExtension(".myextension")
        .withFileExtension(".myotherextension")
        .withValueReader(/* value reader */)
        .build();

// ...

// make use of that resolver
configResolver.loadOrDump(annotatedConfig, file);
```

WHILE ON THAT NOTE: `ConfigResolver` is not meant to be implemented, only created using the designated builder. Implementing it means YOU are going to depend on internal implementations which may change without a moment's notice. Oh, something doesn't work properly? It looks like YOU implemented it and YOU used the internals incorrectly, and it is entirely YOUR fault.

AnnotationConfig internals serialize to a `Map<String, Object>`, and for comments a
`Map<String, List<String>>` keyed by the dotted path of each option (`server.port`).

# Installation

Maven:

```xml
<dependency>
  <groupId>com.mrivanplays</groupId>
  <!-- Types: toml, yaml, json -->
  <!-- For .properties (or .conf) files, or a format of your own, set the type to core -->
  <artifactId>annotationconfig-(type)</artifactId> <!-- Replace type -->
  <version>3.1.0</version>
  <scope>compile</scope>
</dependency>
```

Gradle:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.mrivanplays:annotationconfig-(type):3.1.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
```

# License

(c) 2020-2026 Ivan Pekov and contributors

AnnotationConfig is licensed under MIT, available to read [here](LICENSE)
