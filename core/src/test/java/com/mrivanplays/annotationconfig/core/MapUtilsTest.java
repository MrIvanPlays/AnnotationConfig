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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MapUtilsTest {

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

  @Test
  public void populateFirstMergesTheSecondMapIntoTheFirst() {
    Map<String, Object> first = nested("foo", "baz", "bar", "aa");
    Map<String, Object> second = nested("foo", "baz", "eee", "rrr");

    MapUtils.populateFirst(first, second);

    Assertions.assertEquals(Map.of("foo", Map.of("baz", Map.of("bar", "aa", "eee", "rrr"))), first);
    Assertions.assertEquals(Map.of("foo", Map.of("baz", Map.of("eee", "rrr"))), second);
  }
}
