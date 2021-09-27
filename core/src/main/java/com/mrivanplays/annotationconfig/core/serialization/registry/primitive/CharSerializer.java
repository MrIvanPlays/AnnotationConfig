package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

class CharSerializer implements FieldTypeSerializer<Character> {

  @Override
  public Character deserialize(ConfigDataObject data, Field field) {
    return data.getAsChar();
  }

  @Override
  public SerializedObject serialize(Character value, Field field) {
    return SerializedObject.object(value);
  }
}
