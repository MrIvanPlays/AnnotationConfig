# Change log

## Version 2.0

### Removed custom annotation support

This means you will no longer be able to register custom annotations. The implementation this
library had about custom annotations was messy and nobody could ever understand what is this/that
for and why the hell does it need this/that.

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
  public Location deserialize(ConfigDataObject data, Field field) throws Exception {
    Map<String, Object> map = (Map<String, Object>) data.getRawData();
    return new Location(
        String.valueOf(map.get("world")),
        Integer.parseInt(String.valueOf(map.get("x"))),
        Integer.parseInt(String.valueOf(map.get("y"))),
        Integer.parseInt(String.valueOf(map.get("z"))));
  }

  @Override
  public SerializedObject serialize(Location value, Field field) throws Exception {
    Map<String, Object> map = new HashMap<>();
    map.put("world", value.getWorld());
    map.put("x", value.getX());
    map.put("y", value.getY());
    map.put("z", value.getZ());
    return SerializedObject.map(map);
  }
  
}

// this should be run before you load the config
SerializerRegistry.INSTANCE.registerSerializer(Location.class, new LocationSerializer());
```

### New Annotations
There are two new annotations - `@Min` and `@Max` . They can be applied on `String` values and numbers.
If applied on a number, they will validate the number ; if on a `String`, they will validate the string's length.

### Misc changes

Here are the changes that don't need too much attention, but are still important.

- AnnotationType is now an enum and has been moved to `annotationconfig.core.annotations.type`
- `Retrieve` annotation was renamed to `Ignore` for more clearance
- Annotations are in separate package, `annotationconfig.core.annotations`, the comment annotations are under `annotations.comment`
- `ValueWriter` interface got exposed from `AnnotatedConfigResolver` to its own class and has been documented with lots of information, so you can't do something wrong.
- The library now completely generates all kinds of config options if they are missing in an existing config.
- Fixed a special case bug where `@Key` annotations aren't respected for fields annotated with `@ConfigObject`. This needed changing the `Map<AnnotationHolder, List<AnnotationType>>` to a `Map<AnnotationHolder, Set<AnnotationType>>` so this is yet another breaking change for all custom-made config types.