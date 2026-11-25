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

[Example config output (TOML)](examples/application-config.toml)

[Example config output (JSON)](examples/application-config.json)

[Example config output (JSON5)](examples/application-config.json5)

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

# Config fields must not be final

Options are loaded by setting the fields of your config and its sections with reflection, so don't
make them `final`. AnnotationConfig doesn't skip or reject final fields, as has been the case since
1.0.0, but setting them doesn't work reliably:

- A `final` field initialized with a constant (`private final int port = 25565;`, or a `String`
  literal) is inlined by the compiler: the field is set, but your code keeps reading the constant.
- `static final` fields can't be set at all: with `ACDefaultSettings.FIND_STATIC_FIELDS` set,
  loading them fails.
- Since Java 26, setting a `final` field with reflection prints a warning unless it is allowed with
  `--enable-final-field-mutation`, and a later Java release is to make it an error.

```java
public class MyConfig {

  private int port = 25565; // not: private final int port = 25565;
}
```

Records are the exception: a record is created with its constructor, so its components are final
as usual. The field that holds the record must still not be final.

# Installation

Maven:

```xml
<dependency>
  <groupId>com.mrivanplays</groupId>
  <!-- Types: toml, yaml, json -->
  <!-- For .properties files, or a format of your own, set the type to core -->
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
