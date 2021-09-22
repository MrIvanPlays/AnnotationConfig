package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

class BooleanSerializer implements FieldTypeSerializer<Boolean> {

  @Override
  public Boolean deserialize(ConfigDataObject data, Field field) throws Exception {
    return data.getAsBoolean();
  }

  @Override
  public SerializedObject serialize(Boolean value, Field field) throws Exception {
    return SerializedObject.object(value);
  }
}
