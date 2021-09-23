package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;
import java.util.List;

class ListSerializer implements FieldTypeSerializer<List> {

  @Override
  public List deserialize(ConfigDataObject data, Field field) {
    return (List) data.getRawData();
  }

  @Override
  public SerializedObject serialize(List value, Field field) {
    return SerializedObject.list(value);
  }
}
