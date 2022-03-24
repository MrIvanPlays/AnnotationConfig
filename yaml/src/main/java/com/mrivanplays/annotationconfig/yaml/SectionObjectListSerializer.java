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
@SuppressWarnings({"unchecked", "rawtypes"})
public final class SectionObjectListSerializer<T>
    implements FieldTypeSerializer<SectionObjectList<T>> {

  /** {@inheritDoc} */
  @Override
  public SectionObjectList<T> deserialize(
      DataObject data,
      SerializationContext<SectionObjectList<T>> context,
      AnnotationAccessor annotations) {
    SectionObjectList<T> def =
        context
            .getDefaultValue()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Illegal field to deserialize: null default SectionObjectList"));
    Map<String, Object> map = data.getAsMap();
    Map<String, T> deserialized = new HashMap<>();
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    FieldTypeSerializer serializer =
        serializerRegistry
            .getSerializer(def.getObjectsType())
            .orElse(serializerRegistry.getDefaultSerializer());
    SerializationContext serializationContext =
        SerializationContext.of(
            def.getObjectsType(), def.getObjectsType(), context.getAnnotatedConfig());
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!(entry.getValue() instanceof Map)) {
        throw new IllegalArgumentException("Illegal SectionObjectList inputted: " + data);
      }
      deserialized.put(
          entry.getKey(),
          (T)
              serializer.deserialize(
                  new DataObject(entry.getValue()),
                  serializationContext,
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
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    FieldTypeSerializer serializer =
        serializerRegistry
            .getSerializer(value.getObjectsType())
            .orElse(serializerRegistry.getDefaultSerializer());
    value
        .getAsMap()
        .forEach(
            (k, v) ->
                ret.putAll(
                    k,
                    serializer.serialize(
                        value.getObjectsType().cast(v),
                        SerializationContext.of(
                            value.getObjectsType().cast(v),
                            value.getObjectsType(),
                            value.getObjectsType(),
                            context.getAnnotatedConfig()),
                        AnnotationAccessor.EMPTY)));
    return ret;
  }
}
