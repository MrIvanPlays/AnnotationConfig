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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListEntryMapWithMapValueMapTest {

  static class Subject {

    private Map<String, List<Map<String, Object>>> values =
        Collections.singletonMap("values", getValues());

    public static List<Map<String, Object>> getValues() {
      List<Map<String, Object>> ret = new ArrayList<>();
      Map<String, Object> first = new HashMap<>();
      first.put("foo", 1);
      first.put("bar", 2);
      first.put("baz", Arrays.asList("baba", "yaga"));
      ret.add(first);

      Map<String, Object> second = new HashMap<>();
      second.put("foo", 3);
      second.put("bar", 4);
      Map<String, Object> bazMap = new HashMap<>();
      bazMap.put("baba", "yaga");
      bazMap.put("lorem", "ipsum");
      second.put("baz", bazMap);

      ret.add(second);
      return ret;
    }
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, writer);

    String expected =
        "values:\n"
            + "  values:\n"
            + "    - bar: 2\n"
            + "      foo: 1\n"
            + "      baz:\n"
            + "        - \"baba\"\n"
            + "        - \"yaga\"\n"
            + "    - bar: 4\n"
            + "      foo: 3\n"
            + "      baz:\n"
            + "        baba: \"yaga\"\n"
            + "        lorem: \"ipsum\"\n";

    Assertions.assertEquals(expected, writer.toString().replaceAll("\\R", "\n"));
  }
}
