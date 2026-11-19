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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MapUtilsTest {

  private Map<String, Object> map1;
  private Map<String, Object> map2;

  @BeforeEach
  public void initialize() {
    map1 = new HashMap<>();
    map2 = new HashMap<>();

    Map<String, Object> baz = new HashMap<>();
    baz.put("bar", "aa");
    Map<String, Object> foo = new HashMap<>();
    foo.put("baz", baz);
    map1.put("foo", foo);

    Map<String, Object> baz1 = new HashMap<>();
    baz1.put("eee", "rrr");
    Map<String, Object> foo1 = new HashMap<>();
    foo1.put("baz", baz1);
    map2.put("foo", foo1);
  }

  @AfterEach
  public void terminate() {
    map1 = null;
    map2 = null;
  }

  @Test
  public void testMapUtils() {
    MapUtils.populateFirst(map1, map2);

    Assertions.assertNotEquals(map1, map2);
  }
}
