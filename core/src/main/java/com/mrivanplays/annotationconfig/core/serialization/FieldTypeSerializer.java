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

import com.mrivanplays.annotationconfig.core.utils.TriFunction;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Interface representing a custom serializer and deserializer of a field type. You should write a
 * custom one if you are not happy with how AnnotationConfig serializes and deserializes by default.
 * You will also need to register your newly created class, implementing this interface, through
 * {@link SerializerRegistry#registerSerializer(Class, FieldTypeSerializer)}
 *
 * @param <T> type for which this serializer is being registered.
 * @since 2.0.0
 * @author Ivan Pekov
 */
@NullMarked
public interface FieldTypeSerializer<T> {

  /**
   * Creates a new {@link FieldTypeSerializer} from 2 {@link TriFunction TriFunctions}
   *
   * @param deserialize deserialize method (see {@link FieldTypeSerializer#deserialize(DataObject,
   *     SerializationContext, AnnotationAccessor)})
   * @param serialize serialize method (see {@link FieldTypeSerializer#serialize(Object,
   *     SerializationContext, AnnotationAccessor)})
   * @return new field type serializer
   * @param <T> type for which this serializer is being registered.
   */
  static <T> FieldTypeSerializer<T> functional(
      TriFunction<DataObject, SerializationContext<T>, AnnotationAccessor, T> deserialize,
      TriFunction<T, SerializationContext<T>, AnnotationAccessor, DataObject> serialize) {
    Objects.requireNonNull(deserialize, "deserialize");
    Objects.requireNonNull(serialize, "serialize");
    return new FieldTypeSerializer<>() {
      @Override
      @Nullable
      public T deserialize(
          DataObject data, SerializationContext<T> context, AnnotationAccessor annotations) {
        return deserialize.apply(data, context, annotations);
      }

      @Override
      public TriFunction<DataObject, SerializationContext<T>, AnnotationAccessor, T>
          deserializeAsFunction() {
        return deserialize;
      }

      @Override
      public DataObject serialize(
          T value, SerializationContext<T> context, AnnotationAccessor annotations) {
        //noinspection DataFlowIssue
        return serialize.apply(value, context, annotations);
      }

      @Override
      public TriFunction<T, SerializationContext<T>, AnnotationAccessor, DataObject>
          serializeAsFunction() {
        return serialize;
      }
    };
  }

  /**
   * AnnotationConfig invokes this call-back method during deserialization when it encounters a
   * field of the specified type.
   *
   * @param data the data we received from the config
   * @param context serialization context
   * @param annotations a way to access annotations of the object bound to the deserialized data
   * @return the generic value, the implementation of this interface has specified
   * @see SerializationContext
   * @see AnnotationAccessor
   */
  @Nullable T deserialize(
      DataObject data, SerializationContext<T> context, AnnotationAccessor annotations);

  /**
   * Returns {@link #deserialize(DataObject, SerializationContext, AnnotationAccessor)} as a {@link
   * TriFunction}
   *
   * @return tri function of the deserialize method
   */
  default TriFunction<DataObject, SerializationContext<T>, AnnotationAccessor, T>
      deserializeAsFunction() {
    return this::deserialize;
  }

  /**
   * AnnotationConfig invokes this call-back method during serialization when it encounters a field
   * of the specified type.
   *
   * @param value the data we need serialized
   * @param context serialization context
   * @param annotations a way to access annotations of the object bound to the serialized data
   * @return a serialized object which is useful for dumping into a configuration file
   * @see SerializationContext
   * @see AnnotationAccessor
   */
  DataObject serialize(T value, SerializationContext<T> context, AnnotationAccessor annotations);

  /**
   * Returns {@link #serialize(Object, SerializationContext, AnnotationAccessor)} as a {@link
   * TriFunction}
   *
   * @return tri function of the serialize method
   */
  default TriFunction<T, SerializationContext<T>, AnnotationAccessor, DataObject>
      serializeAsFunction() {
    return this::serialize;
  }
}
