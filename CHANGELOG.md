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
constructor.

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
Configs with values that only passed because of the truncation now fail to load.

#### Unknown enum values are reported

Loading an enum value that matches no constant now throws an `IllegalArgumentException` that names
the enum and lists its constants. It used to fail later with an unrelated error.

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

#### `ReflectionUtils` will be removed

AnnotationConfig no longer uses `ReflectionUtils` internally. It is deprecated for removal and will
be removed in a future release.

### New features

#### Support for records

AnnotationConfig now serializes and deserializes records:

```java
public class MyConfig {

  private MyRecord record = new MyRecord("foo", "bar", 3);

  record MyRecord(@Key("my-super-cool-key") String key, String value, int foo) {}
}
```

`@Ignore` and `@RawConfig` are not supported on record components. AnnotationConfig prints a
warning when it finds them.

#### New `json` module

The new `json` module adds a `JsonConfig` implementation backed by Google's Gson library. It does
not support comments, as JSON has none.

#### `FIND_STATIC_FIELDS` setting

`ACDefaultSettings.FIND_STATIC_FIELDS` (default `false`) controls whether static fields are config
options. See [Static fields are no longer config options](#static-fields-are-no-longer-config-options).

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

### `.properties` writer

`.properties` files are now written by a dedicated writer whose output `java.util.Properties`
reads back exactly:

- Keys and values are escaped: backslashes, line breaks, tabs, leading spaces, control characters,
  and in keys also spaces and `=`, `:`, `#` and `!`. Non-ASCII characters are written as `\uXXXX`.
- Comments that contain line breaks get a `#` on every line.
- `@Multiline` strings are written over several lines using `\` continuation lines.
- `null` values are written as an empty value.
- Arrays are written as comma separated values. Nested arrays, maps and lists are rejected with an
  error that names the key.
- When reading, empty trailing elements of comma separated values are kept (`a,b,` is read as
  three elements).

### Default serializer fixes

- `List<Long>`, `List<Short>` and similar lists hold the declared element type. Numbers used to be
  added as the type the format read (e.g. `Integer`), which caused a `ClassCastException` later.
- Lists of lists, wildcard lists (`List<? extends Number>`) and lists of generic types no longer
  fail with a `ClassCastException`.
- `null` elements in lists are kept instead of throwing a `NullPointerException`.
- Arrays of custom data types are created with their declared component type (e.g. `Item[]`
  instead of `Object[]`), also for nested classes.
- Reading an empty list into an array field creates an empty array of the field's type.
- `BigInteger` and `BigDecimal` are read exactly. They used to be parsed through `long` and
  `double`, which lost precision and failed past 2^63.
- `List<BigInteger>` values larger than `Integer.MAX_VALUE` no longer throw when written; they are
  written as a `long`, or as a string when they do not fit in one.
- `char` fields accept one character strings, which is how text formats store them.
- Enums are written with `Enum#name()`. Constants with a body used to fall back to `toString()`.
  Enum values are read case-insensitively.
- Primitive values that already have the right type are no longer converted through a `String`,
  and type checks are cached per class.

### Config resolver fixes

- With `FIND_PARENT_FIELDS`, a `@ConfigObject` section whose class extends a commented class
  repeated its own comment instead of writing the parent's comment.
- `FIND_PARENT_FIELDS` no longer throws a `NullPointerException` for local and anonymous classes.
- Dumping a `@ConfigObject` field that is `null` throws an `IllegalArgumentException` that names
  the field, instead of a `NullPointerException`.
- With the `SET_NULL` null read handler, a value that is read as `null` on a field with `@Min` or
  `@Max` no longer fails with "annotation placed on invalid field type".
- Dumping to a file writes a temporary file first and moves it into place, so a failed dump no
  longer leaves a half written config behind.
- Errors about duplicate keys and invalid `@Min` / `@Max` values name the key.

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

### Changes in `AnnotationUtils`

`AnnotationUtils` now exposes the formerly internal way of handling comments.
`AnnotationUtils#getComments(Field)` is deprecated, as it did not apply placeholders.

### Misc changes

There are many smaller quality-of-code changes.

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
