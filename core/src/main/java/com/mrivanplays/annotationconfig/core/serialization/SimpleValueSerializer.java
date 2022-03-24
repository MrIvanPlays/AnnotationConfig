package com.mrivanplays.annotationconfig.core.serialization;

import java.util.Objects;

/**
 * Represents a utility class to help you call AnnotationConfig's default serializer for stuff which
 * do not need a custom serializer in a custom serializer of some kind.
 *
 * @author MrIvanPlays
 * @since 3.0.0
 */
public final class SimpleValueSerializer {

  public static <T> T deserialize(
      DataObject value, Class<? extends T> neededValueType, Object annotatedConfig) {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(neededValueType, "neededValueType");
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    return (T)
        DefaultSerializer.INSTANCE.deserialize(
            value,
            SerializationContext.of(neededValueType, neededValueType, annotatedConfig),
            AnnotationAccessor.EMPTY);
  }

  public static <T> DataObject serialize(
      T value, Class<? extends T> valueType, Object annotatedConfig) {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(valueType, "valueType");
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    return DefaultSerializer.INSTANCE.serialize(
        value,
        SerializationContext.of(valueType, valueType, annotatedConfig),
        AnnotationAccessor.EMPTY);
  }

  private SimpleValueSerializer() {}
}
