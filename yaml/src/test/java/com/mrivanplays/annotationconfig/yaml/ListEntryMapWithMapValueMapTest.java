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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** A map holding a list of maps, whose values are a list in one entry and a map in the other. */
public class ListEntryMapWithMapValueMapTest {

  static class Subject {

    private Map<String, List<Map<String, Object>>> values = Map.of("entries", entries());

    // linked maps, so that the written order is the insertion order
    private static List<Map<String, Object>> entries() {
      Map<String, Object> first = new LinkedHashMap<>();
      first.put("foo", 1);
      first.put("bar", 2);
      first.put("baz", List.of("baba", "yaga"));

      Map<String, Object> bazMap = new LinkedHashMap<>();
      bazMap.put("baba", "yaga");
      bazMap.put("lorem", "ipsum");
      Map<String, Object> second = new LinkedHashMap<>();
      second.put("foo", 3);
      second.put("bar", 4);
      second.put("baz", bazMap);

      List<Map<String, Object>> entries = new ArrayList<>();
      entries.add(first);
      entries.add(second);
      return entries;
    }
  }

  @Test
  public void testDump() {
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(new Subject(), writer);
    String expected =
        "values:\n"
            + "  entries:\n"
            + "    - foo: 1\n"
            + "      bar: 2\n"
            + "      baz:\n"
            + "        - \"baba\"\n"
            + "        - \"yaga\"\n"
            + "    - foo: 3\n"
            + "      bar: 4\n"
            + "      baz:\n"
            + "        baba: \"yaga\"\n"
            + "        lorem: \"ipsum\"\n";
    Assertions.assertEquals(expected, writer.toString());
  }
}
