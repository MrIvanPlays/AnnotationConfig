package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a section object list serializer.
 *
 * @param <T> section object list object type
 * @author MrIvanPlays
 * @since v2.1.1
 * @see SectionObjectList
 */
public final class SectionObjectListSerializer<T>
    implements FieldTypeSerializer<SectionObjectList<T>> {

  /** {@inheritDoc} */
  @Override
  public SectionObjectList<T> deserialize(DataObject data, Field field, Object annotatedConfig) {
    try {
      SectionObjectList<T> def = (SectionObjectList<T>) field.get(annotatedConfig);
      Map<String, Object> map = data.getAsMap();
      Map<String, T> deserialized = new HashMap<>();
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        if (!(entry.getValue() instanceof Map)) {
          throw new IllegalArgumentException("Illegal SectionObjectList inputted: " + data);
        }
        FieldTypeSerializer serializer =
            SerializerRegistry.INSTANCE.getSerializer(def.getObjectsType()).orElse(null);
        deserialized.put(
            entry.getKey(),
            serializer != null
                ? (T)
                    serializer.deserialize(new DataObject(entry.getValue()), field, annotatedConfig)
                : (T)
                    SerializerRegistry.INSTANCE.tryDeserialize(
                        new DataObject(entry.getValue()),
                        field,
                        annotatedConfig,
                        def.getObjectsType(),
                        def.getObjectsType()));
      }
      return new SectionObjectList<>(def.getObjectsType(), deserialized);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public DataObject serialize(SectionObjectList<T> value, Field field) {
    DataObject ret = new DataObject();
    value
        .getAsMap()
        .forEach(
            (k, v) -> {
              FieldTypeSerializer serializer =
                  SerializerRegistry.INSTANCE
                      .getSerializer(value.getObjectsType())
                      .orElse(SerializerRegistry.INSTANCE.getDefaultSerializer());
              ret.putAll(k, serializer.serialize(value.getObjectsType().cast(v), field));
            });
    return ret;
  }
}
