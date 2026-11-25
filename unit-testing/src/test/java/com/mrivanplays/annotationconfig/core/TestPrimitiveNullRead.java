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

package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * No value: a field of a custom data type read as {@code null} or missing keeps its value, as an
 * option of a config does; an empty value is no value, which {@code SET_NULL} sets on fields that
 * can hold it, while primitive fields keep their value instead of failing to load. Record
 * components read as {@code null} follow the null read handler.
 */
class TestPrimitiveNullRead {

  private static final Settings SET_NULL =
      ACDefaultSettings.getDefault()
          .copy()
          .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.SET_NULL);

  private static final Settings USE_DEFAULT =
      ACDefaultSettings.getDefault()
          .copy()
          .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.USE_DEFAULT_VALUE);

  /** The map a format reads for an option written without a value, e.g. {@code a: ~} in YAML. */
  private static Map<String, Object> nulls(String... keys) {
    Map<String, Object> map = new HashMap<>();
    for (String key : keys) {
      map.put(key, null);
    }
    return map;
  }

  private static Object deserialize(Object def, Map<String, Object> data, Settings settings) {
    return SerializerRegistry.INSTANCE
        .getDefaultSerializer()
        .deserialize(
            new DataObject(data, true),
            SerializationContext.of(def, def.getClass(), def.getClass(), null, settings),
            AnnotationAccessor.EMPTY);
  }

  @Test
  void aDataTypeFieldReadAsNullKeepsItsValue() {
    Data data = new Data();
    Object read = deserialize(data, nulls("a", "b", "c"), SET_NULL);
    Assertions.assertSame(data, read);
    Assertions.assertEquals(1, data.a);
    Assertions.assertTrue(data.b);
    // as an option of a config read as null does; it was set to null with SET_NULL
    Assertions.assertEquals(2, data.c);
  }

  @Test
  void aDataTypeFieldReadAsAnEmptyValueIsSetToNull() {
    Data data = new Data();
    deserialize(data, Map.of("a", "", "b", "", "c", ""), SET_NULL);
    // an empty number is no value: SET_NULL sets it where the field can hold null
    Assertions.assertEquals(1, data.a);
    Assertions.assertTrue(data.b);
    Assertions.assertNull(data.c);
  }

  @Test
  void aDataTypeFieldReadAsNullKeepsItsValueWithUseDefaultValue() {
    Data data = new Data();
    deserialize(data, nulls("a", "b", "c"), USE_DEFAULT);
    Assertions.assertEquals(1, data.a);
    Assertions.assertTrue(data.b);
    Assertions.assertEquals(2, data.c);
  }

  @Test
  void aDataTypeFieldMissingFromTheFileKeepsItsValue() {
    Data data = new Data();
    deserialize(data, Map.of("a", 5), SET_NULL);
    Assertions.assertEquals(5, data.a);
    Assertions.assertTrue(data.b);
    // a missing option keeps its value, as for the fields of a config
    Assertions.assertEquals(2, data.c);
  }

  @Test
  void aPrimitiveRecordComponentReadAsNullTakesTheDefaultRecordsValue() {
    Object read = deserialize(new Rec(3, true, 4), nulls("a", "b", "c"), SET_NULL);
    // the record's constructor failed with an IllegalArgumentException before
    Assertions.assertEquals(new Rec(3, true, null), read);
  }

  @Test
  void aPrimitiveRecordComponentWithoutADefaultIsZero() {
    Object read =
        SerializerRegistry.INSTANCE
            .getDefaultSerializer()
            .deserialize(
                new DataObject(nulls("a", "b", "c"), true),
                SerializationContext.of(Rec.class, Rec.class, (Object) null, SET_NULL),
                AnnotationAccessor.EMPTY);
    Assertions.assertEquals(new Rec(0, false, null), read);
  }

  static class Data {

    int a = 1;
    boolean b = true;
    Integer c = 2;

    public Data() {}
  }

  record Rec(int a, boolean b, Integer c) {}
}
