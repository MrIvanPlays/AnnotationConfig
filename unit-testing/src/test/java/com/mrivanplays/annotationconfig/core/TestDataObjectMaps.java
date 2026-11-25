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

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * An immutable data object reads the map it holds only when it is accessed. It has to behave as one
 * that wrapped the whole map up front, as mutable data objects still do.
 */
class TestDataObjectMaps {

  private static Map<Object, Object> source() {
    Map<Object, Object> inner = new LinkedHashMap<>();
    inner.put("b", 2);
    inner.put(3, "number key");
    Map<Object, Object> source = new LinkedHashMap<>();
    source.put("a", inner);
    source.put("list", List.of(1, 2));
    source.put(1, "one");
    return source;
  }

  @Test
  void immutableAndMutableDataObjectsAreEqual() {
    DataObject immutable = new DataObject(source(), true);
    DataObject mutable = new DataObject(source(), false);
    Assertions.assertEquals(mutable, immutable);
    Assertions.assertEquals(immutable, mutable);
    Assertions.assertEquals(mutable.hashCode(), immutable.hashCode());
    Assertions.assertEquals(mutable.getAsMap(), immutable.getAsMap());
    Assertions.assertEquals(mutable.toString().replace("false", "true"), immutable.toString());
  }

  @Test
  void keysAreReadAsText() {
    DataObject data = new DataObject(source(), true);
    Assertions.assertEquals("one", data.get("1").getAsObject());
    Assertions.assertTrue(data.has("1"));
    Assertions.assertEquals("number key", data.get("a").get("3").getAsObject());
    Map<String, Object> map = data.getAsMap();
    Assertions.assertEquals(Map.of("b", 2, "3", "number key"), map.get("a"));
    Assertions.assertEquals("one", map.get("1"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void getAsMapIsACopy() {
    Map<Object, Object> source = source();
    DataObject data = new DataObject(source, true);
    Map<String, Object> map = data.getAsMap();
    map.put("added", true);
    ((Map<String, Object>) map.get("a")).put("added", true);
    Assertions.assertFalse(source.containsKey("added"));
    Assertions.assertFalse(((Map<?, ?>) source.get("a")).containsKey("added"));
    Assertions.assertFalse(data.has("added"));
    Assertions.assertEquals(source(), new LinkedHashMap<>(source));
  }

  @Test
  void emptyAndSingleValues() {
    DataObject empty = new DataObject(Map.of(), true);
    Assertions.assertFalse(empty.isSingleValue());
    Assertions.assertTrue(empty.isEmpty());
    Assertions.assertEquals(Map.of(), empty.getAsMap());

    DataObject map = new DataObject(Map.of("a", 1), true);
    Assertions.assertFalse(map.isSingleValue());
    Assertions.assertFalse(map.isEmpty());

    DataObject single = new DataObject("value", true);
    Assertions.assertTrue(single.isSingleValue());
    Assertions.assertFalse(single.isEmpty());
    Assertions.assertEquals(Map.of(), single.getAsMap());
  }

  @Test
  void immutableDataObjectsCannotBeChanged() {
    DataObject data = new DataObject(source(), true);
    Assertions.assertThrows(UnsupportedOperationException.class, () -> data.put("x", "y"));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> data.remove("a"));
    Assertions.assertThrows(UnsupportedOperationException.class, () -> data.get("a").put("x", 1));
  }

  @Test
  void getAsMapKeepsBigNumbers() {
    // getAsMap() turned a BigDecimal into a double and a BigInteger into an int, which threw
    // beyond the int range
    BigDecimal decimal = new BigDecimal("0.12345678901234567890");
    BigInteger integer = new BigInteger("123456789012345678901234567890");
    Map<String, Object> values = Map.of("decimal", decimal, "integer", integer);
    Map<String, Object> map = new DataObject(values).getAsMap();
    Assertions.assertEquals(decimal, map.get("decimal"));
    Assertions.assertEquals(integer, map.get("integer"));
  }
}
