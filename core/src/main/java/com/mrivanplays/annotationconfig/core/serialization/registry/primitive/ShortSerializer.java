package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

class ShortSerializer implements FieldTypeSerializer<Short> {

  @Override
  public Short deserialize(ConfigDataObject data, Field field) {
    return data.getAsShort();
  }

  @Override
  public SerializedObject serialize(Short value, Field field) {
    return SerializedObject.object(value);
  }
}
