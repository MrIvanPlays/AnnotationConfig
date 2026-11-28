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

import java.util.function.Function;
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
   * Creates a new {@link ContextFreeFieldTypeSerializer} from 2 {@link Function functions}.
   *
   * @param deserialize deserialize method (see {@link
   *     ContextFreeFieldTypeSerializer#deserializeSimple(DataObject)})
   * @param serialize serialize method (see {@link
   *     ContextFreeFieldTypeSerializer#serializeSimple(Object)}
   * @param <T> type of which this serializer is being registered.
   * @return new context free field type serializer
   */
  static <T> ContextFreeFieldTypeSerializer<T> functional(
      Function<DataObject, @Nullable T> deserialize, Function<T, DataObject> serialize) {
    return new ContextFreeFieldTypeSerializer<T>() {
      @Override
      public @Nullable T deserializeSimple(DataObject data) {
        return deserialize.apply(data);
      }

      @Override
      public DataObject serializeSimple(T value) {
        return serialize.apply(value);
      }
    };
  }

  /**
   * Deserializes the provided data.
   *
   * @param data the data we received from the config, never {@code null}
   * @return the deserialized value, or {@code null} for no value
   * @since 3.1.1
   */
  @Nullable T deserializeSimple(DataObject data);

  /**
   * Returns {@link #deserializeSimple(DataObject)} as a {@link Function}.
   *
   * @return function of deserialize method
   */
  default Function<DataObject, @Nullable T> deserializeSimpleAsFunction() {
    return this::deserializeSimple;
  }

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
   * Returns {@link #serializeSimple(Object)} as a {@link Function}.
   *
   * @return function of serialize method
   */
  default Function<T, DataObject> serializeSimpleAsFunction() {
    return this::serializeSimple;
  }

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
      DataObject data,
      @Nullable SerializationContext<T> context,
      @Nullable AnnotationAccessor annotations) {
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
      T value,
      @Nullable SerializationContext<T> context,
      @Nullable AnnotationAccessor annotations) {
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
