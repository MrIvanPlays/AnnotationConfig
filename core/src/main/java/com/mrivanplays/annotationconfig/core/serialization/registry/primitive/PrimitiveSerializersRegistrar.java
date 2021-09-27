package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.registry.SerializerRegistry;
import java.util.List;
import java.util.Map;

public final class PrimitiveSerializersRegistrar {

  private static boolean registered = false;

  public static void register() {
    if (registered) {
      return;
    }
    SerializerRegistry registry = SerializerRegistry.INSTANCE;

    registry.registerSerializer(String.class, new StringSerializer());

    IntSerializer integer = new IntSerializer();
    registry.registerSerializer(int.class, integer);
    registry.registerSerializer(Integer.class, integer);

    BooleanSerializer bool = new BooleanSerializer();
    registry.registerSerializer(boolean.class, bool);
    registry.registerSerializer(Boolean.class, bool);

    DoubleSerializer twice = new DoubleSerializer();
    registry.registerSerializer(double.class, twice);
    registry.registerSerializer(Double.class, twice);

    FloatSerializer whoa = new FloatSerializer();
    registry.registerSerializer(float.class, whoa);
    registry.registerSerializer(Float.class, whoa);

    ByteSerializer bite = new ByteSerializer();
    registry.registerSerializer(byte.class, bite);
    registry.registerSerializer(Byte.class, bite);

    registry.registerSerializer(List.class, new ListSerializer());
    registry.registerSerializer(Map.class, new MapSerializer());

    registered = true;
  }
}
