package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class LocationNoConfigObjectSerializer
    implements FieldTypeSerializer<LocationNoConfigObject> {

  @Override
  public LocationNoConfigObject deserialize(ConfigDataObject data, Field field) {
    Map<String, Object> map = (Map<String, Object>) data.getRawData();
    return new LocationNoConfigObject(
        String.valueOf(map.get("world")),
        Integer.parseInt(String.valueOf(map.get("x"))),
        Integer.parseInt(String.valueOf(map.get("y"))),
        Integer.parseInt(String.valueOf(map.get("z"))));
  }

  @Override
  public SerializedObject serialize(LocationNoConfigObject value, Field field) {
    Map<String, Object> map = new HashMap<>();
    map.put("world", value.getWorld());
    map.put("x", value.getX());
    map.put("y", value.getY());
    map.put("z", value.getZ());
    return SerializedObject.map(map);
  }
}
