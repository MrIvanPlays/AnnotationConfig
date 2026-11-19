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
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Illegal field to deserialize: null default SectionObjectList"));
    Map<String, Object> map = data.getAsMap();
    Map<String, T> deserialized = new LinkedHashMap<>();
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
    for (Map.Entry<String, T> entry : value) {
      ret.putAll(
          entry.getKey(),
          serializer.serialize(
              value.getObjectsType().cast(entry.getValue()),
              SerializationContext.of(
                  value.getObjectsType().cast(entry.getValue()),
                  value.getObjectsType(),
                  value.getObjectsType(),
                  context.getAnnotatedConfig()),
              AnnotationAccessor.EMPTY));
    }
    return ret;
  }
}
