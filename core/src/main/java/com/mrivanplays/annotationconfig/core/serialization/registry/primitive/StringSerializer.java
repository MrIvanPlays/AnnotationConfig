package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

class StringSerializer implements FieldTypeSerializer<String> {

  @Override
  public String deserialize(ConfigDataObject data, Field field) {
    return data.getAsString();
  }

  @Override
  public SerializedObject serialize(String value, Field field) {
    return SerializedObject.object(value);
  }
}
