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

/**
 * Represents a utility class to help you call AnnotationConfig's default serializer for stuff which
 * do not need a custom serializer in a custom serializer of some kind.
 *
 * @author MrIvanPlays
 * @since 3.0.0
 */
@SuppressWarnings("unchecked")
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
