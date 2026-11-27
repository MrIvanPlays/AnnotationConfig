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

import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestMapUtils {

  /** {@code {outer: {inner: {key: value}}}}, built from mutable maps. */
  private static Map<String, Object> nested(String outer, String inner, String key, Object value) {
    Map<String, Object> leaf = new HashMap<>();
    leaf.put(key, value);
    Map<String, Object> middle = new HashMap<>();
    middle.put(inner, leaf);
    Map<String, Object> root = new HashMap<>();
    root.put(outer, middle);
    return root;
  }

  /** {@code {keys[0]: {keys[1]: ... {keys[n-1]: value}}}}, built from mutable maps. */
  private static Map<String, Object> chain(Object value, String... keys) {
    Object current = value;
    for (int i = keys.length - 1; i >= 0; i--) {
      Map<String, Object> map = new HashMap<>();
      map.put(keys[i], current);
      current = map;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> root = (Map<String, Object>) current;
    return root;
  }

  @Test
  void populateFirstMergesTheSecondMapIntoTheFirst() {
    Map<String, Object> first = nested("foo", "baz", "bar", "aa");
    Map<String, Object> second = nested("foo", "baz", "eee", "rrr");

    MapUtils.populateFirst(first, second);

    Assertions.assertEquals(Map.of("foo", Map.of("baz", Map.of("bar", "aa", "eee", "rrr"))), first);
    Assertions.assertEquals(Map.of("foo", Map.of("baz", Map.of("eee", "rrr"))), second);
  }

  @Test
  void populateFirstMergesAtAnyDepth() {
    Map<String, Object> first = chain(1, "a", "b", "c", "d", "x");
    MapUtils.populateFirst(first, chain(2, "a", "b", "c", "d", "y"));
    // the deeper map of the second replaced the first's before
    Assertions.assertEquals(
        Map.of("a", Map.of("b", Map.of("c", Map.of("d", Map.of("x", 1, "y", 2))))), first);
  }

  @Test
  void populateFirstKeepsTheValuesOfTheFirstMap() {
    Map<String, Object> first = chain(1, "a", "x");
    first.put("only", "first");
    Map<String, Object> second = chain(2, "a", "x");
    second.put("only", Map.of("in", "second"));
    second.put("new", 3);

    MapUtils.populateFirst(first, second);

    Assertions.assertEquals(Map.of("a", Map.of("x", 1), "only", "first", "new", 3), first);
  }

  @Test
  void unmodifiableDeepCopyCopiesEveryMapAndList() {
    List<Object> list = new ArrayList<>(List.of("a"));
    Map<String, Object> inner = new LinkedHashMap<>();
    inner.put("list", list);
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("inner", inner);
    map.put("none", null);

    // taken before the originals change
    final Map<String, Object> copy = MapUtils.unmodifiableDeepCopy(map, value -> value);
    list.add("b");
    inner.put("added", 1);

    Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("inner", Map.of("list", List.of("a")));
    expected.put("none", null);
    Assertions.assertEquals(expected, copy);
    Map<?, ?> copiedInner = (Map<?, ?>) copy.get("inner");
    Assertions.assertThrows(UnsupportedOperationException.class, () -> copy.put("b", 2));
    Assertions.assertThrows(UnsupportedOperationException.class, copiedInner::clear);
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> ((List<?>) copiedInner.get("list")).clear());
  }

  @Test
  void unmodifiableDeepCopyReplacesTheOtherValues() {
    Map<String, Object> copy =
        MapUtils.unmodifiableDeepCopy(
            Map.of("a", List.of(1, Map.of("b", 2))),
            value -> value instanceof Integer i ? i * 10 : value);

    Assertions.assertEquals(Map.of("a", List.of(10, Map.of("b", 20))), copy);
    Assertions.assertThrows(
        IllegalStateException.class,
        () ->
            MapUtils.unmodifiableDeepCopy(
                Map.of("a", 1),
                _ -> {
                  throw new IllegalStateException("rejected");
                }));
  }

  @Test
  void unmodifiableDeepCopyRejectsKeysThatAreNotStrings() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> MapUtils.unmodifiableDeepCopy(Map.of("a", Map.of(1, "b")), value -> value));
  }
}
