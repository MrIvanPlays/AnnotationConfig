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

package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a section object list serializer.
 *
 * @param <T> section object list object type
 * @author Ivan Pekov
 * @since 2.1.1
 * @see SectionObjectList
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@NullMarked
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
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Illegal field to deserialize: null default SectionObjectList"));
    // a copy of the read maps, so they can be read without being copied again
    Map<String, Object> map = data.getAsMap();
    Map<String, T> deserialized = new LinkedHashMap<>();
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    FieldTypeSerializer serializer =
        serializerRegistry
            .getSerializer(def.getObjectsType())
            .orElse(serializerRegistry.getDefaultSerializer());
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (!(entry.getValue() instanceof Map)) {
        throw new IllegalArgumentException("Invalid SectionObjectList: " + data);
      }
      Object obj =
          serializer.deserialize(
              new DataObject(entry.getValue(), true),
              SerializationContext.of(
                  null,
                  def.get(entry.getKey()),
                  def.getObjectsType(),
                  def.getObjectsType(),
                  def.getObjectsType().isRecord() ? null : context.getAnnotatedConfig(),
                  context.getSettings()),
              AnnotationAccessor.EMPTY);
      if (obj != null) {
        deserialized.put(entry.getKey(), (T) obj);
      }
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
    if (value.isEmpty()) {
      return ret;
    }
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    FieldTypeSerializer serializer =
        serializerRegistry
            .getSerializer(value.getObjectsType())
            .orElse(serializerRegistry.getDefaultSerializer());
    for (Map.Entry<String, T> entry : value) {
      T object = value.getObjectsType().cast(entry.getValue());
      DataObject serialized =
          serializer.serialize(
              object,
              SerializationContext.of(
                  object,
                  value.getObjectsType(),
                  value.getObjectsType(),
                  context.getAnnotatedConfig(),
                  context.getSettings()),
              AnnotationAccessor.EMPTY);
      if (serialized == null) {
        throw new NullPointerException(
            "Expected DataObject, but got null ; SectionObjectList key: " + entry.getKey());
      }
      ret.putAll(entry.getKey(), serialized);
    }
    return ret;
  }
}
