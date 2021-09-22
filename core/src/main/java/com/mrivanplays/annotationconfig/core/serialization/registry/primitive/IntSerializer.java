package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

class IntSerializer implements FieldTypeSerializer<Integer> {

  @Override
  public Integer deserialize(ConfigDataObject data, Field field) throws Exception {
    return data.getAsInt();
  }

  @Override
  public SerializedObject serialize(Integer value, Field field) throws Exception {
    return SerializedObject.object(value);
  }
}
