package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

class FloatSerializer implements FieldTypeSerializer<Float> {

  @Override
  public Float deserialize(ConfigDataObject data, Field field) throws Exception {
    return data.getAsFloat();
  }

  @Override
  public SerializedObject serialize(Float value, Field field) throws Exception {
    return SerializedObject.object(value);
  }
}
