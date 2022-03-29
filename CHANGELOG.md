# Change log

This file summarises changes between major versions.

## Version 3.0.0
This version is all focused on serialization.

### Introducing "SimpleValueSerializer"
This is a utility class to help you with serializing objects you do not need a custom serializer for
in your own custom serializer of some kind. See the docs for more.

### FieldTypeSerializer now is not exactly "Field"
Instead of passing a Field, AnnotationConfig now passes a "SerializationContext", because
AnnotationConfig can not always get a Field instance. We don't want to lie with another Field, 
instead, we are now focused on accurate data - what is it exactly (de)serializing.
The other thing new we have is the "AnnotationAccessor" - this is a way of accessing a (perhaps)
field's annotations in a more controllable manner.

### Beefed up default serializer
- The default serializer now recognizes BigInteger and BigDecimal as "primitives" and (de)serializes
them. 
- Fixed a few type mistakes on list (de)serialization, which caused everything to go to the 
default serializer rather than a proper serializer, if such is registered.
- The default serializer now recognizes `@Key` and `@Ignore` annotations whenever (de)serializing
an object.
- The default serializer now properly serializes arrays.

### Misc changes
- Removed deprecated methods
- Fixed `DataObjectBuilder#with(String, DataObject)`
- Removed `SerializerRegistry#registerSerializer(Class, BiFunction, BiFunction)`. Replaced by 
`SerializerRegistry#registerSimpleSerializer(Class, Function, Function)`
- Quality-of-life code changes
- Added a class called ReflectionUtils
- Optimisations to MapUtils
- Better preserve dump order

## Version 2.1.0

### Comments inside sections
Now you are able to do comments inside sections. This required breaking the ValueWriter, but
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
  public ValidationResponse validate(MyAnnotation annotation, Object value, CustomOptions options,
      Field field) {
    // value validation logic
  }

}

// then register the validator in the registry
CustomAnnotationRegistry.INSTANCE.register(MyAnnotation.class, new MyAnnotationValidator());
```

### Change FieldTypeResolver and `@TypeResolver` logic

The previous system was hard to understand because the FieldTypeResolver had some unnecessary stuff.
This new system is much better, trust me!

The `@TypeResolver` annotation was removed and replaced by a `SerializerRegistry`.
<br>
The `FieldTypeResolver` was renamed to `FieldTypeSerializer`, generalized and with methods changed
to `serialize` and `deserialize`.
<br>
This means faster to type resolvers and much more control over what the library dumps in a config
file and what it resolves to.
<br>
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
  @Comment("Location of player")
  @Key("player-location")
  private Location location = new Location("world", 20, 2, 1);

// the serializer
public class LocationSerializer implements FieldTypeSerializer<Location> {

  @Override
  public Location deserialize(DataObject data, Field field) {
    return new Location(
        data.get("world").getAsString(),
        data.get("x").getAsInt(),
        data.get("y").getAsInt(),
        data.get("z").getAsInt()
    );
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
SerializerRegistry.INSTANCE.registerSerializer(Location.class,new LocationSerializer());
```

Or, you can let the default config serializer handle serialization and deserialization. The default
implementation is pretty good and there's no additional need for annotations or special
constructors. All you need is a simple object with set fields and you're good to go!

### A way to implement new config types

In 1.0, if you wanted other than the already implemented config types, you'd have to dive into the
internal class AnnotatedConfigResolver and figure out how to implement it. Plus the thing that it
required implementation of the interface called `ValueWriter`. This was an inner interface for
AnnotatedConfigResolver, which made it even harder. No more!
<br><br>In version 2.0.0 it was added a proper API called `ConfigResolver` to interact with this
internal class. Also, `ValueWriter` interface got exposed from `AnnotatedConfigResolver` to its own
class. Everything has been documented with lots of information, so you can more easily create them,
and so you don't do something wrong.
<br><br>You can also add custom options to a `ConfigResolver` to be accessed by the config reader
and writer.

#### Example usage

```java
    ConfigResolver resolver =
        ConfigResolver.newBuilder()
            .withValueReader(/* value reader */)
            .withValueWriter(/* value writer */)
            .withCommentPrefix("# " /* the implemented config type's comment prefix */)
            .shouldReverseFields(
                true /* in some config types it is needed to reverse the fields so everything is in order with the config object we're reading from */)
            .build();
    // do stuff with the resolver instance
```

### New Annotations

There are two new annotations - `@Min` and `@Max` . They can be applied on `String` values and
numbers. If applied on a number, they will validate the number ; if on a `String`, they will
validate the string's length.

### Specific TOML changes

`mwanji/toml4j` has been replaced with `FasterXML/jackson-dataformats-text` (toml module), meaning
if you have used a custom `TomlWriter` from toml4j, it needs to be migrated to the `TomlMapper`
of jackson-dataformats-toml.

### Misc changes

Here are the changes that don't need too much attention, but are still important.

- AnnotationType is now an enum and has been moved to `annotationconfig.core.annotations.type`
- `Retrieve` annotation was renamed to `Ignore` for more clearance
- Annotations are in separate package, `annotationconfig.core.annotations`, the comment annotations
  are under `annotations.comment`
- The library now completely generates all kinds of config options if they are missing in an
  existing config.
- Fixed a special case bug where `@Key` annotations aren't respected for fields annotated
  with `@ConfigObject`.
- Handle fields which aren't annotated at all.