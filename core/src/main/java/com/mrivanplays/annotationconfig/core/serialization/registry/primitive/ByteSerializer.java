package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

class ByteSerializer implements FieldTypeSerializer<Byte> {

  @Override
  public Byte deserialize(ConfigDataObject data, Field field) throws Exception {
    return data.getAsByte();
  }

  @Override
  public SerializedObject serialize(Byte value, Field field) throws Exception {
    return SerializedObject.object(value);
  }
}
