package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;

class DoubleSerializer implements FieldTypeSerializer<Double> {

  @Override
  public Double deserialize(ConfigDataObject data, Field field) throws Exception {
    return data.getAsDouble();
  }

  @Override
  public SerializedObject serialize(Double value, Field field) throws Exception {
    return SerializedObject.object(value);
  }
}
