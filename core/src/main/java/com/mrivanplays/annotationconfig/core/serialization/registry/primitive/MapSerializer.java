package com.mrivanplays.annotationconfig.core.serialization.registry.primitive;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;
import java.util.Map;

class MapSerializer implements FieldTypeSerializer<Map> {

  @Override
  public Map deserialize(ConfigDataObject data, Field field) {
    return (Map) data.getRawData();
  }

  @Override
  public SerializedObject serialize(Map value, Field field) {
    return SerializedObject.map(value);
  }
}
