package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
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
  public SectionObjectList<T> deserialize(
      DataObject data,
      SerializationContext<SectionObjectList<T>> context,
      AnnotationAccessor annotations) {
    SectionObjectList<T> def = context.getDefaultValue().orElse(null);
    if (def == null) {
      throw new IllegalArgumentException(
          "Illegal field to deserialize: null default SectionObjectList");
    }
    Map<String, Object> map = data.getAsMap();
    Map<String, T> deserialized = new HashMap<>();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!(entry.getValue() instanceof Map)) {
        throw new IllegalArgumentException("Illegal SectionObjectList inputted: " + data);
      }
      FieldTypeSerializer serializer =
          SerializerRegistry.INSTANCE
              .getSerializer(def.getObjectsType())
              .orElse(SerializerRegistry.INSTANCE.getDefaultSerializer());
      deserialized.put(
          entry.getKey(),
          (T)
              serializer.deserialize(
                  new DataObject(entry.getValue()),
                  SerializationContext.of(
                      null,
                      null,
                      def.getObjectsType(),
                      def.getObjectsType(),
                      context.getAnnotatedConfig()),
                  AnnotationAccessor.EMPTY));
    }
    return new SectionObjectList<>(def.getObjectsType(), deserialized);
  }

  /** {@inheritDoc} */
  @Override
  public DataObject serialize(
      SectionObjectList<T> value,
      SerializationContext<SectionObjectList<T>> context,
      AnnotationAccessor annotations) {
    DataObject ret = new DataObject();
    value
        .getAsMap()
        .forEach(
            (k, v) -> {
              FieldTypeSerializer serializer =
                  SerializerRegistry.INSTANCE
                      .getSerializer(value.getObjectsType())
                      .orElse(SerializerRegistry.INSTANCE.getDefaultSerializer());
              ret.putAll(
                  k,
                  serializer.serialize(
                      value.getObjectsType().cast(v),
                      SerializationContext.of(
                          null,
                          value.getObjectsType().cast(v),
                          value.getObjectsType(),
                          value.getObjectsType(),
                          context.getAnnotatedConfig()),
                      AnnotationAccessor.EMPTY));
            });
    return ret;
  }
}
