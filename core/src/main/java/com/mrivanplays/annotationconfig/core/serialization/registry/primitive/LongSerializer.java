package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

class LongSerializer implements FieldTypeSerializer<Long> {

  @Override
  public Long deserialize(ConfigDataObject data, Field field) {
    return data.getAsLong();
  }

  @Override
  public SerializedObject serialize(Long value, Field field) {
    return SerializedObject.object(value);
  }
}
