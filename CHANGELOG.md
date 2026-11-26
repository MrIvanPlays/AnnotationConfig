# Change log

This file summarises the notable changes of each version.

## Version 3.1.0

This version was developed as 3.0.4, which was never released. Because it contains breaking
changes, it is released as 3.1.0. Read [Breaking changes](#breaking-changes) before upgrading.

### Breaking changes

#### Custom data types need an empty constructor

The default serializer no longer uses `sun.misc.Unsafe` to create instances, so custom data types
need a declared empty constructor:

```java
class SerializedObject {

  private int foo;
  private String baz;

  public SerializedObject() {}

  public SerializedObject(int foo, String baz) {
    this.foo = foo;
    this.baz = baz;
  }

  public int getFoo() {
    return foo;
  }

  public String getBaz() {
    return baz;
  }
}

class Subject {

  public SectionObjectList<SerializedObject> list =
      SectionObjectList.newBuilderForType(SerializedObject.class)
          .defaultValue("foo", new SerializedObject(1, "lorem"))
          .defaultValue("bar", new SerializedObject(2, "ipsum"))
          .build();
}
```

`Subject` is the config class and `SerializedObject` the custom data type, which needs the empty
constructor. The constructor does not have to be public: a private or package-private one works
too.

Not every custom data type strictly needs one: when the type is not used inside a `List`, `Map` or
`SectionObjectList`, AnnotationConfig modifies the default instance of the field directly. An empty
constructor is still **recommended** as a failsafe.

Records are deserialized through their canonical constructor, so they are the only custom data
type that is fully supported without an empty constructor.

#### Static fields are no longer config options

Static fields used to be dumped and loaded like any other field, and loading one changed it for
every instance of the class. They are now skipped. To keep the old behaviour, enable the new
`FIND_STATIC_FIELDS` setting:

```java
ConfigResolver resolver = YamlConfig.getConfigResolver();
resolver.settings().put(ACDefaultSettings.FIND_STATIC_FIELDS, true);
```

The same goes for the fields of custom data types inside a config, so their constants are no longer
written. A data type with one field and a constant is written as that field's value, as one with
only the field is: `port: 25565` instead of `port: {value: 25565}`. Files with the old form still
load. A data type whose only field is static can't be written or read without the setting; it used
to set the static field.

#### New `@Min` and `@Max` defaults

Every member of `@Min` and `@Max` used to default to `-1`, and `-1` meant "not set". That made `-1`
impossible to use as a bound. The members now default to the value that can never reject
anything: the lowest value of their type for `@Min` (`Integer.MIN_VALUE`,
`Double.NEGATIVE_INFINITY`, ...) and the highest for `@Max`.

```java
@Min(minInt = -1) // now works; used to fail with "invalid @Min specified"
private int offset = 0;
```

A member that is explicitly set to `-1` now counts as set, so `@Min(minInt = 5, minDouble = -1)`
fails with "it should set only one annotation member".

#### `@Min` and `@Max` compare exact values

Bounds used to be compared after converting both sides with `intValue()`. That truncated decimals
and wrapped long values that do not fit in an `int`, so some invalid values passed:

```java
@Max(maxDouble = 1.5)
private double ratio = 1.0; // 1.9 used to pass, as both became 1

@Max(maxLong = 10)
private long limit = 5; // 3000000000 used to pass, as it wrapped to a negative int
```

Numbers are now compared by their exact value, `BigInteger` and `BigDecimal` included. A `float`
bound uses the decimal value it was written as, so `@Min(minFloat = 0.1f)` accepts `0.1`.
Configs with values that only passed because of the truncation now fail to load. `NaN` fails
both annotations; it used to pass any `@Min`.

#### Empty strings are values

An empty string in a config (`message=` in `.properties`, `message: ""` in YAML, `message = ''` in
TOML) used to be read as no value: the field became `null`, or kept its default with the
`USE_DEFAULT_VALUE` null read handler. It is now read as `""`, also inside lists, so an option can
be configured to be empty.

Empty values of other types, such as a number written as `count=`, are still read as no value.

#### Unknown enum values are reported

Loading an enum value that matches no constant now throws an `IllegalArgumentException` that names
the enum and lists its constants. It used to fail later with an unrelated error.

#### `AdvancedEnumSerializer` matches each word once

`AdvancedEnumSerializer` accepts the words of a constant in any order. A word of the input used to
match every word of the constant that equals it, so repeated words could stand in for missing ones:
`deny deny true` was read as `DENY_ALL_TRUE`. Every word of a constant is now matched once, so such
a value matches no constant and is read as `null`: the field becomes `null` with the default
`SET_NULL` null read handler, or keeps its default with `USE_DEFAULT_VALUE`.

Repeated separators are now ignored, so `accept  all  true` and `true__deny all`, which used to
match nothing, are read as `ACCEPT_ALL_TRUE` and `DENY_ALL_TRUE`.

#### `SET_NULL` applies to the fields of custom data types

The fields of a custom data type (a field whose type has its own fields, read by the default
serializer) kept their value when the file had them without a value, as if `USE_DEFAULT_VALUE` was
set. They now follow the null read handler as the options of a config do: with the default
`SET_NULL`, a field read as an empty value (`amount=`) becomes `null`. A field missing from the
file or read as `null` (`amount: null`) still keeps its value, as an option of a config does.

A primitive field (`int`, `boolean`, ...) read as `null` used to fail to load with `SET_NULL`, in a
config and in a custom data type. It now keeps its value; `Integer`, `Boolean` and other fields that
can hold `null` are set to `null`.

#### `.properties` reading

Empty trailing array elements are kept. See
[Breaking changes in `.properties`](#breaking-changes-in-properties).

#### Empty config files get the defaults

An existing config file without any options, such as a file created empty by hand, was left as
it was by `load` and `loadOrDump`: as nothing was read, no option counted as missing. Every option
now counts as missing, so with `GENERATE_NEW_OPTIONS` (on by default, off for TOML) the file is
written with the defaults. Loading an empty file, or an empty `Map` through `load(Object, Map)`,
also sets `@RawConfig` fields to an empty `DataObject`; both used to do nothing.

#### Numbers in maps keep their type

`DataObject#getAsMap()`, which also fills `Map<String, Object>` fields, returns numbers as they were
read. It used to turn every `BigDecimal` into a `double` and every `BigInteger` into an `int`.

Jackson's TOML parser reads every float as a `BigDecimal`, so a float in a TOML
`Map<String, Object>` field, or in a map read through `getAsMap()` in a custom serializer, is now a
`BigDecimal` instead of a `Double`. Code that casts such a value to `Double` fails with a
`ClassCastException`; read it as a `Number` instead (`((Number) map.get("ratio")).doubleValue()`).
In YAML, only a decimal that a `double` can't hold exactly is a `BigDecimal`, see
[SnakeYAML-backed YAML writer](#snakeyaml-backed-yaml-writer).

Integers beyond the `long` range in a YAML map, which SnakeYAML reads as `BigInteger`, used to throw
an `ArithmeticException` and are now read. Lists are written the same way: `BigDecimal`s
in lists used to be written as `double`s and are now written exactly, and `BigInteger`s beyond the
`int` range no longer throw. TOML, whose integers are 64-bit, writes larger integers as strings.

#### YAML output changed

The YAML module now writes through SnakeYAML (see
[SnakeYAML-backed YAML writer](#snakeyaml-backed-yaml-writer)). Files are still read the same way,
but newly dumped files look slightly different:

- Lists nested inside other values are indented under their key:

  ```yaml
  # 3.0.x
  items:
    - name: "a"
      tags:
      - "x"

  # 3.1.0
  items:
    - name: "a"
      tags:
        - "x"
  ```

- There is no extra blank line at the start and end of the file.
- `@Multiline` strings that use the default `'"'` marker are written as one double quoted line with
  `\n` escapes. Use `@Multiline('|')` or `@Multiline('>')` to get a block scalar.
- A `Yaml` instance passed through `YamlConfig.YAML_INSTANCE` is now used for writing as well as
  reading. It has to be created with `DumperOptions.setProcessComments(true)`, otherwise SnakeYAML
  drops all comments.

#### TOML dotted keys are tables

A dotted key (`@Key("server.ip")`) is now read and written as a nested table, as in YAML and JSON.
It used to be one quoted key:

```toml
# 3.0.x
'server.ip' = 'localhost'

# 3.1.0
[server]
ip = 'localhost'
```

Files written by 3.0.x still load: a key that is in the file as it is, quoted, is read first.
What changes is that a file holding the value as a table or an unquoted TOML dotted key
(`server.ip = 'localhost'`), which used to be ignored, so the field kept its default, now sets the
field. A dotted key and a config object with the same first part (`server`) share one table.

### New features

#### Support for records

AnnotationConfig now serializes and deserializes records:

```java
public class MyConfig {

  private MyRecord record = new MyRecord("foo", "bar", 3);

  record MyRecord(@Key("my-super-cool-key") String key, String value, int foo) {}
}
```

- A record is created with its canonical constructor. Its components are its fields that are not
  static, and they are passed in the order the record declares them; the names of the
  constructor's parameters are not used, so records also work when a tool such as an obfuscator
  strips them.
- Components are read with the serializer registered for their generic type, e.g.
  `SectionObjectList<Item>`, as other fields are.
- A component that is missing from the file is `null` with the default `SET_NULL` null read
  handler. With `USE_DEFAULT_VALUE` it keeps its value from the record the field held, also for a
  field the config's class inherits. A primitive component, which can't be `null`, does that with
  either handler, and is `0` / `false` when there is no record to take the value from.
- `@Min`, `@Max` and the validators of custom annotations apply to components as they do to
  fields: `record Bounds(@Min(minInt = 0) int min, int max) {}` fails to load a negative `min`. A
  custom validator that fails silently makes the component take its value from the record the
  field held, as a missing component does.
- `@Ignore` and `@RawConfig` are not supported on record components. AnnotationConfig prints a
  warning the first time it finds them on a record.

#### `@Min`, `@Max` and custom annotations in custom data types

`@Min`, `@Max` and the validators of custom annotations apply to the fields of custom data types,
also of a type with a single field, which is written as that field's value, as they do to the
fields of a config and of its `@ConfigObject` sections:

```java
class Limits {

  @Max(maxInt = 100)
  int players = 20;

  public Limits() {}
}
```

A value out of bounds fails to load. A custom validator that fails silently keeps the value the
field had. For record components, see [Support for records](#support-for-records).

#### `@Min` and `@Max` on the elements of lists, maps and arrays

`@Min` and `@Max` can be put on the element types of a field's type, to bound every element:

```java
private List<@Max(maxInt = 65535) Integer> ports = List.of(25565);
private Map<String, @Min(minInt = 3) String> names = Map.of("admin", "Ivan");
private List<List<@Max(maxInt = 10) Integer>> grid = List.of(List.of(1));
private @Max(maxInt = 10) int[] weights = {1, 2};
```

- Lists and other collections, the values and keys of maps, nested types, the bound of a wildcard
  (`List<? extends @Max(...) Number>`) and record components are checked, in every format.
- On an array field, the annotation bounds the elements: Java puts `@Max(...) int[]` on the
  field and on the element type `int`. It failed with "annotation placed on invalid field type".
- On a `List` or `Map` field itself, the annotation still fails with that error, which now
  points to the element type.
- An error names the element: `ports[1]`, `grid[1][0]`, `names.admin`, or the key of a map.

#### Booleans accept `yes`, `no`, `on` and `off`

A `boolean` option, and `DataObject#getAsBoolean()`, now read `yes` and `on` as `true` in every
format, in any case, as YAML 1.1 already did; `no`, `off` and any other value are `false`, as
before. In `.properties`, TOML and JSON, `enabled=yes` used to turn the option off.

```properties
# 3.0.x: false, 3.1.0: true
enabled=yes
```

#### Polymorphic values with `@Polymorphic`

`@Polymorphic` lets the config choose the implementation of an interface, abstract class or sealed
type. The section names it under a discriminator key, `type` by default:

```java
class MyConfig {

  @Polymorphic({
    @Polymorphic.Subtype(name = "sqlite", type = SqliteStorage.class),
    @Polymorphic.Subtype(name = "mysql", type = MysqlStorage.class)
  })
  private Storage storage = new SqliteStorage();
}
```

```yaml
storage:
  type: "mysql"
  host: "localhost"
  port: 3306
```

- On a field, it applies to the field's value; a config field or a field of a `@ConfigObject`
  section becomes a section, without `@ConfigObject`. On a `List` or array field, it applies to
  the elements.
- On the declaration of a type, it applies to every value of that type: config fields, sections,
  lists, arrays, the fields of custom data types and record components.
- On a `sealed` type, the subtypes can be left out: the permitted subclasses are used, named by
  `@Polymorphic.Name` or by their simple class name. Use `@Polymorphic.Name` where classes can be
  renamed or obfuscated, as the simple name changes with them:

  ```java
  @Polymorphic(discriminator = "kind")
  sealed interface Shape permits Circle, Square {}

  record Circle(double radius) implements Shape {} // named "Circle"

  @Polymorphic.Name("box")
  record Square(int side) implements Shape {}

  class MyConfig {

    private List<Shape> shapes = List.of(new Circle(1.5), new Square(2));
  }
  ```

  ```yaml
  shapes:
    - kind: "Circle"
      radius: 1.5
    - kind: "box"
      side: 2
  ```
- The implementations are classes with an empty constructor, or records. A value of the
  implementation the field already holds is filled in place, as a `@ConfigObject` section is;
  otherwise a new one is created.
- A section without the discriminator keeps the implementation of the field's value and counts as
  a missing option, so `GENERATE_NEW_OPTIONS` writes it. Without such a value, for example in a
  list, it is an error, as are unknown names and classes that are not listed (also subclasses of
  listed ones). Errors name the field and the names that are allowed.
- The annotation is checked the first time it is used: implementations must be concrete subtypes
  with unique names, the discriminator must not be blank or hold a dot, and no implementation may
  have a field with the discriminator's key.
- Not supported: the values of maps, which are read and written as they are for every custom data
  type, and `.properties` files, which have no sections.

#### Config versions and migrations with `@ConfigVersion`

A config class can have a version, and strategies that migrate older files to it when they are
loaded:

```java
@ConfigVersion(2)
public class MyConfig {

  @Key("server-port")
  private int port = 8080;
}

// version 2 renamed "port" to "server-port"
MigrationStrategies.INSTANCE.registerStrategyFor(
    MyConfig.class,
    MigrationVersionRange.of(1, 2),
    (values, keyResolver) -> {
      values.renameKey("port", "server-port");
      return values;
    });
```

- The version is written as the first option of the file, under the key `config-version` (set
  another with `@ConfigVersion(value = 2, key = "version")`; it can't hold a dot).
- The version can have comments, written above it like the comments of an option:
  `@ConfigVersion(value = 2, comments = {"Don't change this", "Used to migrate the file"})`. As
  with `@Comment`, a line break starts a new comment line and registered comment placeholders are
  applied. Formats without comments (JSON) write the version without them.
- A file without the version is version 1, so files written before the class had a version are
  migrated from version 1. A file of the current version that lacks it counts as missing an option,
  and gets it written with `GENERATE_NEW_OPTIONS`. An empty file is not migrated.
- A strategy gets the values of the file, without the version, as a mutable `DataObject`, and the
  `KeyResolver` the config is read with (the `KEY_RESOLVER` setting of the load, so the format's
  unless one is passed), and returns the values of the version it migrates to. Keys are as the
  format read them: sections are nested in YAML, JSON and TOML, while `.properties` keeps dotted
  keys flat. Values are of the types the format's parser reads, so the `DataObject` getters
  (`getAsInt()`, ...) are the way to read them. Keys a strategy leaves behind are dropped when the
  file is rewritten.
- `DataObject#renameKey(from, to)` moves the value of an option to a new key and returns whether
  there was one. If the new key has a value already, that value is kept and the old key is dropped.
  Neither key may be empty: an empty one fails with an `IllegalArgumentException`.
  It works on the keys of that data object only, so a dotted key is renamed in `.properties` but has
  to be found in its section in YAML, JSON and TOML.
- `DataObject#renameDottedKey(from, to, keyResolver)` moves a value the way a format's key resolver
  reads and writes keys, so it can move an option between sections in any format:
  `values.renameDottedKey("server.port", "network.port", keyResolver)`, with the key resolver the
  strategy gets, makes one strategy work for every format (`KeyResolver.DEFAULT` keeps dotted keys
  flat in `.properties`, `DottedResolver` nests them in YAML and JSON). It keeps a value under the new key as `renameKey` does,
  removes sections the move leaves empty, and fails without changes if the new key can't be
  written, e.g. because a part of it is a value. TOML reads a dotted key as one quoted key first, as
  3.0.x wrote it, so such keys are moved too and written back as tables.
- Strategies are registered in `MigrationStrategies` for the class that carries `@ConfigVersion`
  (subclasses inherit it), one per version they migrate from. A step may skip versions
  (`MigrationVersionRange.of(1, 3)`); the chain follows the steps from the file's version.
- A file loaded from a path that is migrated is copied to `<name>.v<version>.bak` next to it
  (`<name>.v<version>.bak.2`, ... if that exists), then rewritten in the current version, also in
  TOML, which keeps `GENERATE_NEW_OPTIONS` off. The copy doesn't end with the config's extension,
  so `resolveMultiple` doesn't read it. Loading from a reader or a map migrates the values without
  writing anything.
- If no strategy is registered for the class at all, an older file is read as it is: it is not
  migrated, copied or rewritten, and a warning is logged on every load through the JDK's
  `System.Logger`, named `com.mrivanplays.annotationconfig.migration` (by default it goes to
  `java.util.logging`). Register the strategies before the first load: a dump, or a load that
  writes missing options with `GENERATE_NEW_OPTIONS`, replaces such a file in the current version
  without migrating it or making a backup, so its values that a migration would have moved are
  lost, and later loads count the file as current.
- Loading fails, without changing the file or any field, if the file is of a newer version than the
  class, its version is not a whole number, strategies are registered but a step on the way has
  none, or a strategy fails.
- Only the config's class is versioned: `@ConfigVersion` has no effect on `@ConfigObject` sections.

#### `FIND_STATIC_FIELDS` setting

`ACDefaultSettings.FIND_STATIC_FIELDS` (default `false`) controls whether static fields are config
options. See [Static fields are no longer config options](#static-fields-are-no-longer-config-options).

### JSON module

The new `json` module adds a `JsonConfig` implementation backed by Google's Gson library. Its JSON
resolver writes no comments, as JSON has none; the [JSON5 resolver](#json5-support) does.

- Config objects, records, maps and other objects are written as JSON objects, arrays and lists as
  JSON arrays.
- Dotted keys (`@Key("server.ip")`) are written and read as nested objects, as in YAML.
- Numbers are read exactly, also beyond the precision of a `double`. Values of fields typed `Object`
  (e.g. inside a `Map<String, Object>` or `DataObject`) are read as `BigDecimal`.
- Strings are written as they are, without HTML escaping (`=` is not written as `\u003d`).
- `@Multiline` strings are written as normal strings, since JSON has no multiline syntax.
- Files end with a line break, as the files of the other formats do. JSON5 files do too.
- A `Gson` instance can be passed through `JsonConfig.GSON_INSTANCE`. It is used for reading and
  writing, and should use `ToNumberPolicy.BIG_DECIMAL` to read numbers exactly.

#### JSON5 support

The `json` module also supports [JSON5](https://spec.json5.org) (spec 1.0.0), a superset of JSON
that adds comments, unquoted keys, single-quoted strings and more.
`JsonConfig.getJson5ConfigResolver()` returns a resolver for `.json5` files. It writes with the new
`Json5ValueWriter` and reads with Gson in lenient mode, like the JSON resolver, so existing `.json`
files load with it too. `JsonConfig.GSON_INSTANCE` is used for reading.

Writing uses only the JSON5 features that Gson reads back:

- The comments of options, also of options inside config objects, are written as `//` lines above
  their keys, and class comments at the top of the file. Comments of keys inside arrays are not
  written.
- Keys that are identifiers are written without quotes (`port: 25565`), other keys in double quotes
  (`"world-name": "w"`).
- Strings are written in double quotes. `@Multiline` strings are written on one line with `\n`
  escapes: JSON5's line continuations (a backslash before a line break) drop the line break, but
  Gson keeps it, so they would not read back the same.
- Infinity and NaN are written as `Infinity`, `-Infinity` and `NaN`.
- The layout is that of pretty-printed JSON, without trailing commas.

Reading hand-written JSON5 is limited to what Gson's lenient mode understands. `//` and `/* */`
comments, unquoted keys, single-quoted strings, `Infinity` and `NaN` are read. Gson does not read
these JSON5 features as the spec defines them:

- A trailing comma in an object fails to load; in an array it adds a `null` element.
- Hexadecimal numbers (`0x1F`), numbers with a leading `+` or a leading or trailing decimal point
  (`.5`, `5.`) are read as strings. Number fields parse all of them except hexadecimal ones; fields
  typed `Object` get the string. The same holds for `Infinity` and `NaN`.
- The `\x41`, `\v` and `\0` escapes fail to load.
- A line continuation in a string keeps the line break in the value.

### SnakeYAML-backed YAML writer

YAML used to be written by a home-grown writer, `YamlValueWriter`. The new `YamlHandler` reads and
writes YAML through SnakeYAML instead: values are turned into a SnakeYAML node tree with the
comments attached to their keys, so SnakeYAML takes care of indentation, quoting and escaping.
`YamlValueWriter` is deprecated.

- Strings are double quoted and long strings are not wrapped.
- `@Multiline` maps its marker to a SnakeYAML scalar style: `'|'` writes a literal block, `'>'` a
  folded block and `'"'` (the default, kept for compatibility) a double quoted string:

  ```java
  @Multiline('|')
  public String motd = """
      Look at ma!
      I am a multiline string!
      """;
  ```

  ```yaml
  motd: |
    Look at ma!
    I am a multiline string!
  ```

- Comments of keys inside list elements are not written.
- Decimals are read exactly. SnakeYAML reads every decimal as a `double`, so a `BigDecimal` such
  as `0.12345678901234567890` was written exactly but read back as `0.12345678901234568`. A decimal
  that a `double` can't hold exactly is now read as a `BigDecimal`, also in a
  `Map<String, Object>`; other decimals are still `Double`s. A `Yaml` instance passed through
  `YamlConfig.YAML_INSTANCE` reads decimals as SnakeYAML does.

### TOML writer

TOML used to be written by passing each top-level option to Jackson's TOML generator, which wrote
some values that are not valid TOML. `TomlValueWriter` now writes TOML 1.0 itself; Jackson is only
used for reading. Options are still separated by an empty line and objects inside arrays are still
written as inline tables.

- Config objects, maps and other objects are written as tables instead of dotted keys:

  ```toml
  # Server comment
  [server]
  ip = 'localhost'
  port = 25565

  [server.limits]
  players = 20
  ```

  As every key after a table header belongs to that table, the tables are written after the
  other options, so options declared after a config object now come before it. A table that only
  holds other tables and has no comment gets no header of its own (`[a.b]` defines `a`). Empty
  objects are still written as `{}`. Files written with dotted keys load as before. Dotted `@Key`s
  are tables too, see [TOML dotted keys are tables](#toml-dotted-keys-are-tables).
- Infinity and NaN are written as `inf`, `-inf` and `nan` instead of `Infinity` and `NaN`, which
  made the file unreadable.
- Comments of config objects are written above their table header, and comments of options
  inside them above the option; they used to be dropped. Comments are looked up by the exact key,
  so an option no longer gets the comments of another option whose name starts the same (`port`
  and `portal`), and every line of a comment that contains line breaks starts with `#`.
- `@Multiline` strings are written as multi-line basic strings (`"""`).
- `OffsetDateTime`, `LocalDateTime`, `LocalDate` and `LocalTime` are written as TOML date-times
  instead of strings, with seconds always present as TOML requires. Files with dates written as
  strings still load. Other formats still write these types as strings.
- Integers outside of 64 bits, which TOML does not allow, are written as strings.
- Jackson is updated to 2.23.0, whose TOML parser reads integers with 19 digits correctly; 2.22
  dropped their first 9 digits (`9223372036854775807` was read as `6854775807`).
- A `null` value fails with an error that names its key, as TOML has no null. The file is built
  before it is written, so a value that can't be written fails before anything is written.
- Files no longer end with an empty line.
- `TomlValueWriter(TomlMapper)` is deprecated in favour of `TomlValueWriter()`, as the mapper is
  not used for writing.

### `.properties`

`.properties` files are now written by a dedicated writer whose output `java.util.Properties`
reads back exactly.

#### Breaking changes in `.properties`

- Empty trailing elements of an array are kept: `a,b,` is read as three elements, the last one
  empty, where 3.0.x read two. An empty element fails to load in a primitive number array
  (`int[]`, ...) and is `null` in an `Integer[]` and the like.

#### Other changes in `.properties`

Files written by 3.0.x load as before. Newly written files differ where 3.0.x wrote something
that did not read back as it was:

- Keys and values are escaped: backslashes, line breaks, tabs, leading spaces, control characters,
  and in keys also spaces and `=`, `:`, `#` and `!`. Non-ASCII characters are written as `\uXXXX`.
  3.0.x wrote them as they were, so for example a backslash was lost when reading.
- Comments that contain line breaks get a `#` on every line.
- `@Multiline` strings are written over several lines using `\` continuation lines.
- `null` values are written as an empty value, instead of failing with a `NullPointerException`.
- Arrays are written as comma separated values, and elements keep their `[` and `]`, which 3.0.x
  removed. Nested arrays, maps and lists are rejected with an error that names the key.
- Lines end with `\n` on every OS, see [Config resolver fixes](#config-resolver-fixes).

As in 3.0.x, every value that holds a comma is read as an array, as the reader can't tell an array
from text: a string option with a comma in it (`motd=Hello, world`), and an element of a `String`
array that holds a comma, do not read back as they were written.

### Default serializer fixes

- `List<Long>`, `List<Short>` and similar lists hold the declared element type. Numbers used to be
  added as the type the format read (e.g. `Integer`), which caused a `ClassCastException` later.
- Lists of lists, wildcard lists (`List<? extends Number>`) and lists of generic types no longer
  fail with a `ClassCastException`. The elements of nested lists get their declared type too: a
  `List<List<Integer>>` read from JSON held `BigDecimal`s.
- The values of a map field with a declared value type, such as `Map<String, Long>` or
  `Map<String, List<Integer>>`, are converted to that type, as list elements are. They were kept as
  the format read them: `BigDecimal`s from JSON, and floats from TOML, where 3.0.x turned them into
  `Double`. A value that can't be converted, such as text in a `Map<String, Integer>`, is still kept
  as it was read, so that no config that loaded before fails now. Maps of other objects are read as
  before.
- `null` elements in lists are kept, when writing and reading, instead of throwing a
  `NullPointerException`. `null` elements
  of arrays of custom data types are kept too; they were left out, so the array was read back
  shorter. TOML, which has no null, rejects them with an error, as it does in lists.
- Arrays of custom data types are created with their declared component type (e.g. `Item[]`
  instead of `Object[]`), also for nested classes.
- Reading an empty list into an array field creates an empty array of the field's type.
- `BigInteger` and `BigDecimal` are read exactly. They used to be parsed through `long` and
  `double`, which lost precision and failed past 2^63.
- `BigDecimal` and `BigInteger` values in maps and lists are kept exact, see
  [Numbers in maps keep their type](#numbers-in-maps-keep-their-type).
- Whole numbers that arrive with an exponent are read as their value. A double such as
  `2000000000.0` has the text form `2.0E9`, which used to be read as `2`.
- `char` fields accept one character strings, which is how text formats store them.
- Enums are written with `Enum#name()`. Constants with a body used to fall back to `toString()`.
  Enum values are read case-insensitively. The error for an unknown value lists the constants as
  they are written, not by `toString()`.
- `@Min`, `@Max` and the validators of custom annotations are applied to the fields of custom data
  types. The default serializer applied neither; only the fields of a config and its
  `@ConfigObject` sections were validated. See
  [the new feature](#min-max-and-custom-annotations-in-custom-data-types).
- Primitive values that already have the right type are no longer converted through a `String`,
  and type checks are cached per class.
- A field that the config's class inherits (see `FIND_PARENT_FIELDS`) is found when its current
  value is looked up, so the object in it is reused instead of replaced by a new one. This only
  happened for fields of the class itself.
- Synthetic fields, such as the outer instance of an inner class, are not written or read.

### Config resolver fixes

- Fields that the compiler adds (synthetic fields) are no longer options of a config class or a
  `@ConfigObject` section either. A section that is a non-static inner class using its outer
  instance had that instance written as a `this$0` section holding a copy of the config, and with
  `FIND_STATIC_FIELDS` a class with an `assert` statement had a `$assertionsDisabled` option.
- With `FIND_PARENT_FIELDS`, a `@ConfigObject` section whose class extends a commented class
  repeated its own comment instead of writing the parent's comment.
- `FIND_PARENT_FIELDS` no longer throws a `NullPointerException` for local and anonymous classes.
- Dumping a `@ConfigObject` field that is `null` throws an `IllegalArgumentException` that names
  the field, instead of a `NullPointerException`.
- With the `SET_NULL` null read handler, a value that is read as `null` on a field with `@Min` or
  `@Max` no longer fails with "annotation placed on invalid field type".
- Dumping over an existing file no longer deletes it first: the config is written to a temporary
  file next to it, which then replaces it, so a failed dump leaves the old file as it was instead
  of a missing or half written one. A new file is written in place.
- Dumping to a file in a directory that doesn't exist creates the directory; it failed with a
  `NoSuchFileException`.
- Dumping through a `Path` keeps the file's permissions and a symbolic link stays a link to the
  file it points to. As the file was deleted and created again, its permissions became those of
  a new file and a link was replaced by a regular file. The `File` overloads, which wrote over the
  file, already did both.
- Errors about duplicate keys and invalid `@Min` / `@Max` values name the key.
- A config class with annotations but no `@Comment` no longer starts the file with a blank line.
- Configs are written with `\n` line endings on every OS. On Windows, files mixed CRLF and LF: the
  class comment was written with the OS line separator, as were `.properties` and TOML files, while
  YAML used `\n`. `.properties` and TOML files written on Windows now end their lines with
  `\n` instead of `\r\n`; both are read either way.
- `resolveMultiple` creates a missing directory and dumps the defaults into it, instead of
  throwing an `IllegalArgumentException`. It also dumps the defaults when the directory only holds
  files that are not configs, and returns the configs sorted by file name.
- Dotted keys that share more than three parts are all written. Of `@Key("a.b.c.d.x")` and
  `@Key("a.b.c.d.y")` only the last was written, without an error: `MapUtils#populateFirst`, which
  merges the values of dotted keys, only merged maps two levels deep. It now merges at any depth
  and keeps the first map's value when the two maps hold different values under a key.
- `getConfigResolver()` of `PropertyConfig`, `YamlConfig` and `TomlConfig` returns one instance
  even when it is first called from several threads at once. They created the resolver on first
  use without synchronization, so concurrent first calls could each get their own instance.
- The `File` overloads of `dump`, `load` and `loadOrDump` behave as the `Path` ones. Dumping to and
  loading from a directory throw an `IllegalArgumentException`; loading from one used to do
  nothing.
- `resolveMultiple` finds config files whatever the case of their extension (`CONFIG.YML`). A file
  extension given to `ConfigResolver.Builder` without its dot matches only after a dot, so `yml`
  no longer matches `notyml`.
- With `FIND_PARENT_FIELDS`, a field of a parent class that a field of the same name hides is not
  an option. Both were, and dumping failed with a duplicate key.
- The `InputStream` and `OutputStream` overloads of `load` and `dump` read and write UTF-8, as the
  `File` and `Path` overloads do. They used the default charset of the JVM.
- The Javadoc of the `Reader`, `Writer`, `InputStream`, `OutputStream` and `WritableObject`
  overloads of `load` and `dump` says that they close what they are given, as they always did.
- `ConfigResolver.Builder#copy()` and the copy constructor copy the settings and file extensions.
  The copy shared them, so setting a value on the copy changed the original builder too.
- When a custom annotation's `writeValue` returns a value of another type, the
  `SerializationContext` passed to its serializer describes that value instead of the field.

### Changes in `AnnotationAccessor`

`AnnotationAccessor` is now a `sealed interface` and can also access the annotations of a
`Class<?>`. `hasAnnotation(Class<? extends Annotation>)` and `hasAnnotation(AnnotationType)` were
added.

### Changes in `CommentPlaceholderRegistry`

Placeholders are now registered through the new `CommentPlaceholder` interface, and some helper
methods were added:

```java
CommentPlaceholderRegistry.INSTANCE.registerPlaceholder(
    CommentPlaceholder.simple("%foo%", "epic replacement"));
```

- Placeholders are applied in the order they were registered. They used to be applied in hash
  order, so the result of placeholders that produce other placeholders was unpredictable.
- Registering a placeholder for text that already has one replaces the registered placeholder,
  which keeps its position. Both used to be kept, and either replacement could win.
- `applyPlaceholders(List)` always returns a new mutable list. With nothing to replace it used to
  return the list it was given.

### Registry fixes

- `SerializerRegistry`, `CustomAnnotationRegistry` and `CommentPlaceholderRegistry` are safe to use
  from several threads. Registering checked for an existing entry and added the new one in two
  steps, so two threads registering the same type could both succeed.
- `registerSimpleValueSerializer` and `registerSimpleValueDeserializer` replace the entry in one
  step, so there is no moment in which the type has no serializer. Their Javadoc now says that
  they replace a registered serializer, while `registerSerializer` throws.
- The default serializer that `registerSimpleValueSerializer` and
  `registerSimpleValueDeserializer` fall back to gets the context of the value, with its settings,
  generic type and default value. It used to get a new context without them, and failed with a
  `NullPointerException` for values without an annotated config.
- `SimpleValueSerializer` accepts a `null` annotated config.
- Unregistering an annotation without a validator fails with "... is not registered" instead of
  "... already not registered".

### Changes in `AnnotationUtils`

`AnnotationUtils` now exposes the formerly internal way of handling comments.
`AnnotationUtils#getComments(Field)` is deprecated, as it did not apply placeholders.

### Misc changes

- What AnnotationConfig reads from a class (its fields, keys, annotations and comments) is read
  once per class instead of on every dump and load. Comment placeholders are still applied on
  every dump.
- `ValueWriter#writesComments()` (default `true`) tells AnnotationConfig whether a writer uses
  comments. When it returns `false`, comments are not collected and the writer gets none.
- `MapUtils` has new helpers: `copyWithStringKeys` and `deepCopyWithStringKeys` copy a map with
  its keys as text, `withStringKeys` does so only when a key is not a string, and `parentPaths`
  returns the dotted paths of the parents of a map's keys. `MapUtils#getLastCommonMap` is
  deprecated, as AnnotationConfig doesn't use it anymore.
- `LinesHelper#split` (in `core.utils`) splits text, such as a comment, into its lines at every
  line break, for a `ValueWriter` that writes a comment marker in front of every line. The YAML,
  JSON5 and TOML writers use it.
- An immutable `DataObject` created from a map reads the map when it is accessed instead of
  wrapping all of it up front, so the map should not be changed afterwards. Loading wrapped every
  value of the file this way, once for each level of nesting.
- `DataObject#getAs` now properly checks the type value before casting. Previously,
  `getAs(Integer.class)` on a `Number` would work, but `getAs(Number.class)` on an `Integer`
  would throw. `DataObject#getList` had the same check backwards and skipped every element that
  was a subclass of the requested type; it now also skips `null` elements instead of throwing.
- `DataObject#getAsChar` reads a one character string, which is how text formats store
  characters. It used to throw a `ClassCastException` for anything but a `Character`.
- `DataObject#getAsBigDecimal` and `getAsBigInteger` parse the value's text exactly. They went
  through `double` and `long`, which lost precision and overflowed.
- The number getters of `DataObject` (`getAsInt`, `getAsLong`, `getAsShort`, `getAsByte`,
  `getAsDouble`, `getAsFloat`, `getAsBigInteger`) read a value as a field of their type does. A
  whole number that a format read as a decimal, such as a `1.0` read by YAML, failed with a
  `NumberFormatException` in `getAsInt` while an `int` field read it; a decimal is now cut at its
  decimal point for the whole-number getters, as for fields. A value that is no number still
  throws a `NumberFormatException`.
- `DataObject#getAsMap` returns a new mutable map for a data object that holds a single value, as
  it does otherwise. It returned an immutable empty map.
- `new DataObject()` is an empty map: `isEmpty()` is `true` and `isSingleValue()` is `false`. It
  held no map until a value was put into it, so it was a single value (`null`) and not empty. A
  custom serializer that returns it for a value with nothing to write now leaves the option out,
  as for any empty data object, instead of writing a `null` (which TOML rejected).
- `DataObjectBuilder#copy()` and its copy constructor copy the data object. The copy shared it, so
  adding to the copy changed the original.
- `Setting` has a `hashCode` that agrees with `equals`, and two settings are equal when their keys
  and types are. A setting created without a key was not equal to an identical one.
- `SectionObjectList#iterator()` entries can't be changed with `setValue`, and every
  `SectionObjectListBuilder#build()` returns a list with its own values.
  `SectionObjectListBuilder#defaultValue` rejects `null`.
- `SectionObjectList` has `equals` and `hashCode`: two lists are equal when they hold the same
  objects type and the same entries, in any order, as maps are. They were only equal to
  themselves. `toString` shows the type and the entries.

- `AnnotationAccessor#hasAnnotation` and `getAnnotation` both look at the declared annotations
  only. For a class, `hasAnnotation` also found inherited annotations that `getAnnotation` did not
  return.
- `ReflectionUtils` is no longer used internally. It is deprecated for removal and will be removed
  in a future release.
- There are many smaller quality-of-code changes.

## Version 3.0.3

No significant changes, apart from that the library is now released on Maven Central.

## Version 3.0.1

This version features:

- Updated dependencies
- Added `CommentPlaceholderRegistry`

### `CommentPlaceholderRegistry`

With this, you can register placeholders to be replaced in comments by AnnotationConfig whenever it
creates a new config file.

Usage:

```java
CommentPlaceholderRegistry.INSTANCE.registerPlaceholder("%foo%", "epic replacement");
```

## Version 3.0.0

This version is all focused on serialization.

### Introducing `SimpleValueSerializer`

This is a utility class to help you with serializing objects you do not need a custom serializer for
in your own custom serializer of some kind. See the docs for more.

### FieldTypeSerializer now is not exactly `Field`

Instead of passing a `Field`, AnnotationConfig now passes a `SerializationContext`, because
AnnotationConfig can not always get a `Field` instance. We don't want to lie with another `Field`,
instead, we are now focused on accurate data - what is it exactly (de)serializing.
The other thing new we have - the `AnnotationAccessor` - this is a way of accessing a (perhaps)
`Field`'s annotations in a more controllable manner.

#### Better experience with FieldTypeSerializer?

Introducing the `FieldTypeSerializer#functional(TriFunction, TriFunction)` with which it shall be
easier to create `FieldTypeSerializer`s. Quick example:

```java
// Before
SerializerRegistry.INSTANCE.registerSerializer(
    Foo.class,
    new FieldTypeSerializer<Foo>() {

      @Override
      public Foo deserialize(
          DataObject data, SerializationContext<Foo> context, AnnotationAccessor annotations) {
        // deserialization logic
      }

      @Override
      public DataObject serialize(
          Foo value, SerializationContext<Foo> context, AnnotationAccessor annotations) {
        // serialization logic
      }
    });

// After
SerializerRegistry.INSTANCE.registerSerializer(
    Foo.class,
    FieldTypeSerializer.<Foo>functional(
        (data, context, annotations) -> {
          // deserialization logic
        },
        (value, context, annotations) -> {
          // serialization logic
        }));
```

### Beefed up default serializer

- The default serializer now recognizes `BigInteger` and `BigDecimal` as "primitives" and
  (de)serializes them.
- Fixed a few type mistakes on list (de)serialization, which caused everything to go to the
  default serializer rather than a proper serializer, if such is registered.
- The default serializer now recognizes `@Key` and `@Ignore` annotations whenever (de)serializing
  an object.

### `CustomOptions` and `LoadSettings` replaced

These two were confusing, so now they have been replaced with a unified `Settings` object which
works together with the `Setting` interface. They can be used just as the objects they replaced, but
with much better design.

#### Going smarter

##### Arrays

The default serializer now properly (de)serializes array types.
Examples:

```java
class MyConfig {

  private MyObj[] myObjArr = new MyObj[] {new MyObj("foo"), new MyObj("bar")};

  static class MyObj {

    String foo;

    MyObj(String foo) {
      this.foo = foo;
    }

    // serializer is not necessary
    static class Serializer implements FieldTypeSerializer<MyObj> {

      @Override
      public MyObj deserialize(
          DataObject data, SerializationContext<MyObj> context, AnnotationAccessor annotations) {
        return new MyObj(data.getAsString());
      }

      @Override
      public DataObject serialize(
          MyObj value, SerializationContext<MyObj> context, AnnotationAccessor annotations) {
        return new DataObject(value.foo);
      }
    }
  }
}

class Main {

  public static void main(String[] args) {
    MyConfig myConfig = new MyConfig();
    SerializerRegistry serializers = SerializerRegistry.INSTANCE;
    serializers.registerSerializer(MyConfig.MyObj.class, new MyConfig.MyObj.Serializer());
    YamlConfig.getConfigResolver().dump(myConfig, new File("config.yml"));
  }
}
```

The code above will output the following:

```yaml
myObjArr: [ "foo", "bar" ]
```

##### Single field class logic

The default serializer now (de)serializes classes with only 1 field better. An example of usage for
such class is a list where you may want some more redundancy.

Examples (YAML):

```yaml
# Version 2.1.x:
foo:
  - bar: "zzz"

# Version 3.0.0:
foo:
  - "zzz"
```

### Misc changes

- Removed deprecated methods
- Removed `SerializerRegistry#registerSerializer(Class, BiFunction, BiFunction)`. Replaced by
  `SerializerRegistry#registerSimpleSerializer(Class, Function, Function)`
- Quality-of-life code changes
- Added a class called `ReflectionUtils`
- Optimisations to `MapUtils`
- Better preserve dump order
- Added `SerializerRegistry#registerSimpleValueSerializer(Class, Function)` and
  `SerializerRegistry#registerSimpleValueDeserializer(Class, Function)`
- Deprecation of `DateSerializer` in the toml module.
- Since not everything can be listed in the changelog, there have been many miscellaneous changes
  improving performance and API design. The full changes can be seen in the release commit(s).

## Version 2.1.0

### Comments inside sections

Now you are able to do comments inside sections. This required breaking the `ValueWriter`, but
it's for good.

## Version 2.0.1

### Key resolver

You are able to resolve keys. There's a default resolver and implementation for dotted keys which
you can use.

### Misc changes

- Simplified missing options generation

## Version 2.0.0

### Rewritten custom annotations

The previous implementation we had about custom annotations was messy and nobody could ever
understand what is this/that for and why the hell does it need this/that.

The new implementation is pretty straightforward: custom annotations as of right now can only be
used for validation of a given annotated field's deserialized value.

#### Example usage

```java
// an annotation you have
public @interface MyAnnotation {}

// a class which implements AnnotationValidator
public class MyAnnotationValidator implements AnnotationValidator<MyAnnotation> {

  @Override
  public ValidationResponse validate(
      MyAnnotation annotation, Object value, CustomOptions options, Field field) {
    // value validation logic
  }
}

// then register the validator in the registry
CustomAnnotationRegistry.INSTANCE.register(MyAnnotation.class, new MyAnnotationValidator());
```

### Change FieldTypeResolver and `@TypeResolver` logic

The previous system was hard to understand because the FieldTypeResolver had some unnecessary stuff.
This new system is much better, trust me!

- The `@TypeResolver` annotation was removed and replaced by a `SerializerRegistry`.
- The `FieldTypeResolver` was renamed to `FieldTypeSerializer`, generalized and with methods
  changed to `serialize` and `deserialize`.

This means faster to type resolvers and much more control over what the library dumps in a config
file and what it resolves to.

Another cool thing is that the default serializer handles enums, maps and lists, and you won't need
to create specific serializers for them.

#### Example usage

```java
// an object you have
public class Location {

  private String world;
  private int x, y, z;

  public Location(String world, int x, int y, int z) {
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  // getters
}

// this is in your base config
// of course the annotations aren't mandatory
public class MyConfig {

  @Comment("Location of player")
  @Key("player-location")
  private Location location = new Location("world", 20, 2, 1);
}

// the serializer
public class LocationSerializer implements FieldTypeSerializer<Location> {

  @Override
  public Location deserialize(DataObject data, Field field) {
    return new Location(
        data.get("world").getAsString(),
        data.get("x").getAsInt(),
        data.get("y").getAsInt(),
        data.get("z").getAsInt());
  }

  @Override
  public DataObject serialize(Location value, Field field) {
    DataObject object = new DataObject();
    object.put("world", value.getWorld());
    object.put("x", value.getX());
    object.put("y", value.getY());
    object.put("z", value.getZ());
    return object;
  }
}

// this should be run before you load the config
SerializerRegistry.INSTANCE.registerSerializer(Location.class, new LocationSerializer());
```

Or, you can let the default config serializer handle serialization and deserialization. The default
implementation is pretty good and there's no additional need for annotations or special
constructors. All you need is a simple object with set fields and you're good to go!

### A way to implement new config types

In 1.0, if you wanted other than the already implemented config types, you'd have to dive into the
internal class `AnnotatedConfigResolver` and figure out how to implement it. Plus the thing that it
required implementation of the interface called `ValueWriter`. This was an inner interface for
`AnnotatedConfigResolver`, which made it even harder. No more!

In version 2.0.0 a proper API called `ConfigResolver` was added to interact with this internal
class. Also, the `ValueWriter` interface got moved out of `AnnotatedConfigResolver` to its own
class. Everything has been documented with lots of information, so you can more easily create them,
and so you don't do something wrong.

You can also add custom options to a `ConfigResolver` to be accessed by the config reader and
writer.

#### Example usage

```java
ConfigResolver resolver =
    ConfigResolver.newBuilder()
        .withValueReader(/* value reader */)
        .withValueWriter(/* value writer */)
        // the implemented config type's comment prefix
        .withCommentPrefix("# ")
        // some config types need the fields reversed, so that everything is in the same order
        // as in the config object we're reading from
        .shouldReverseFields(true)
        .build();
// do stuff with the resolver instance
```

### New Annotations

There are two new annotations - `@Min` and `@Max`. They can be applied on `String` values and
numbers. If applied on a number, they will validate the number; if on a `String`, they will
validate the string's length.

### Specific TOML changes

`mwanji/toml4j` has been replaced with `FasterXML/jackson-dataformats-text` (toml module), meaning
if you have used a custom `TomlWriter` from toml4j, it needs to be migrated to the `TomlMapper`
of jackson-dataformats-toml.

### Misc changes

Here are the changes that don't need too much attention, but are still important.

- `AnnotationType` is now an enum and has been moved to `annotationconfig.core.annotations.type`
- The `Retrieve` annotation was renamed to `Ignore` for more clearance
- Annotations are in a separate package, `annotationconfig.core.annotations`; the comment
  annotations are under `annotations.comment`
- The library now completely generates all kinds of config options if they are missing in an
  existing config.
- Fixed a special case bug where `@Key` annotations aren't respected for fields annotated
  with `@ConfigObject`.
- Handle fields which aren't annotated at all.
