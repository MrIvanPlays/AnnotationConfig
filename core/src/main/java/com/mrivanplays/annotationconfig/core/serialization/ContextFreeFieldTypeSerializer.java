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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link FieldTypeSerializer} that (de)serializes a value from the value alone, without the
 * {@link SerializationContext} and the {@link AnnotationAccessor}. Implement {@link
 * #deserializeSimple(DataObject)} and {@link #serializeSimple(Object)}; {@link
 * #deserialize(DataObject, SerializationContext, AnnotationAccessor)} and {@link #serialize(Object,
 * SerializationContext, AnnotationAccessor)} call them.
 *
 * @param <T> type for which this serializer is being registered.
 * @author Ivan Pekov
 * @since 3.1.1
 */
@NullMarked
public interface ContextFreeFieldTypeSerializer<T> extends FieldTypeSerializer<T> {

  /**
   * Deserializes the provided data.
   *
   * @param data the data we received from the config, never {@code null}
   * @return the deserialized value, or {@code null} for no value
   * @since 3.1.1
   */
  @Nullable T deserializeSimple(DataObject data);

  /**
   * Serializes the provided value.
   *
   * @param value the value to serialize
   * @return a serialized object which is useful for dumping into a configuration file, never {@code
   *     null}
   * @since 3.1.1
   */
  DataObject serializeSimple(T value);

  /**
   * Calls {@link #deserializeSimple(DataObject)} with the data, ignoring the context and the
   * annotations.
   *
   * @param data the data we received from the config
   * @param context serialization context, not used
   * @param annotations annotations of the object bound to the deserialized data, not used
   * @return what {@link #deserializeSimple(DataObject)} returns
   */
  @Override
  default @Nullable T deserialize(
      DataObject data, SerializationContext<T> context, AnnotationAccessor annotations) {
    return this.deserializeSimple(data);
  }

  /**
   * Calls {@link #serializeSimple(Object)} with the value, ignoring the context and the
   * annotations.
   *
   * @param value the value to serialize
   * @param context serialization context, not used
   * @param annotations annotations of the object bound to the serialized data, not used
   * @return what {@link #serializeSimple(Object)} returns
   */
  @Override
  default DataObject serialize(
      T value, SerializationContext<T> context, AnnotationAccessor annotations) {
    return this.serializeSimple(value);
  }

  /**
   * Returns {@code true}: this serializer doesn't use the context or the annotations.
   *
   * @return {@code true}
   */
  @Override
  default boolean isContextFree() {
    return true;
  }
}
