/*
 * Copyright (c) Ivan Pekov 2026
 * Copyright (c) Contributors 2026
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.mrivanplays.annotationconfig.core.serialization;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Calls AnnotationConfig's default serializer, for use inside a custom serializer for values that
 * need no custom serializer of their own.
 *
 * @author Ivan Pekov
 * @since 3.0.0
 */
@SuppressWarnings("unchecked")
@NullMarked
public final class SimpleValueSerializer {

  private SimpleValueSerializer() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * Deserializes {@code value} with the default serializer.
   *
   * @param value the data to deserialize
   * @param neededValueType the type to deserialize to
   * @param annotatedConfig the annotated config the value belongs to, if any
   * @param <T> the type to deserialize to
   * @return the deserialized value, or {@code null}
   * @see FieldTypeSerializer#deserialize(DataObject, SerializationContext, AnnotationAccessor)
   */
  public static <T> @Nullable T deserialize(
      DataObject value, Class<? extends T> neededValueType, @Nullable Object annotatedConfig) {
    return deserialize(value, neededValueType, annotatedConfig, AnnotationAccessor.EMPTY);
  }

  /**
   * Deserializes {@code value} with the default serializer.
   *
   * @param value the data to deserialize
   * @param neededValueType the type to deserialize to
   * @param annotatedConfig the annotated config the value belongs to, if any
   * @param annotationAccessor the annotations of the value
   * @param <T> the type to deserialize to
   * @return the deserialized value, or {@code null}
   * @see FieldTypeSerializer#deserialize(DataObject, SerializationContext, AnnotationAccessor)
   * @since 3.1.0
   */
  public static <T> @Nullable T deserialize(
      DataObject value,
      Class<? extends T> neededValueType,
      @Nullable Object annotatedConfig,
      AnnotationAccessor annotationAccessor) {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(neededValueType, "neededValueType");
    Objects.requireNonNull(annotationAccessor, "annotationAccessor");
    return (T)
        DefaultSerializer.INSTANCE.deserialize(
            value,
            SerializationContext.of(neededValueType, neededValueType, annotatedConfig),
            annotationAccessor);
  }

  /**
   * Serializes {@code value} with the default serializer.
   *
   * @param value the value to serialize
   * @param valueType the type of the value
   * @param annotatedConfig the annotated config the value belongs to, if any
   * @param <T> the type of the value
   * @return the serialized value
   * @see FieldTypeSerializer#serialize(Object, SerializationContext, AnnotationAccessor)
   */
  public static <T> DataObject serialize(
      T value, Class<? extends T> valueType, @Nullable Object annotatedConfig) {
    return serialize(value, valueType, annotatedConfig, AnnotationAccessor.EMPTY);
  }

  /**
   * Serializes {@code value} with the default serializer.
   *
   * @param value the value to serialize
   * @param valueType the type of the value
   * @param annotatedConfig the annotated config the value belongs to, if any
   * @param annotationAccessor the annotations of the value
   * @param <T> the type of the value
   * @return the serialized value
   * @see FieldTypeSerializer#serialize(Object, SerializationContext, AnnotationAccessor)
   * @since 3.1.0
   */
  public static <T> DataObject serialize(
      T value,
      Class<? extends T> valueType,
      @Nullable Object annotatedConfig,
      AnnotationAccessor annotationAccessor) {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(valueType, "valueType");
    Objects.requireNonNull(annotationAccessor, "annotationAccessor");
    return DefaultSerializer.INSTANCE.serialize(
        value, SerializationContext.of(valueType, valueType, annotatedConfig), annotationAccessor);
  }
}
