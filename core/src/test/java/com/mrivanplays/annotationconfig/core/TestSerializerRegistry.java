package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import com.mrivanplays.annotationconfig.core.serialization.registry.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.serialization.registry.primitive.PrimitiveSerializersRegistrar;
import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;

public class TestSerializerRegistry {

  @Test
  public void testRegisteringASerializerOnce() {
    SerializerRegistry registry = SerializerRegistry.INSTANCE;
    // make sure primitives are registered, otherwise the test will fail
    PrimitiveSerializersRegistrar.register();
    try {
      registry.registerSerializer(Byte.class, new DummyByteSerializer());
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(true);
    }
  }

  private static final class DummyByteSerializer implements FieldTypeSerializer<Byte> {

    @Override
    public Byte deserialize(ConfigDataObject data, Field field) {
      return null;
    }

    @Override
    public SerializedObject serialize(Byte value, Field field) {
      return null;
    }
  }

}
