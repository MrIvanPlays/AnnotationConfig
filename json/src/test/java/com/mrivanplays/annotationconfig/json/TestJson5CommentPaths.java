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

package com.mrivanplays.annotationconfig.json;

import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.ToNumberPolicy;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Comments are looked up by dotted path only in objects that hold a commented value; the others are
 * written the same way without building the paths of their keys.
 */
class TestJson5CommentPaths {

  private static Map<String, Object> map(Object... entries) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < entries.length; i += 2) {
      map.put((String) entries[i], entries[i + 1]);
    }
    return map;
  }

  @Test
  void commentsInSomeObjectsOnly() {
    final Map<String, Object> values =
        map(
            "top", 1,
            "plain", map("x", map("y", 2), "pool", map("z", 5)),
            "commented", map("inner", map("deep", 3), "flat", 4));
    Map<String, List<String>> comments = new LinkedHashMap<>();
    comments.put("top", List.of());
    comments.put("commented.inner.deep", List.of("deep comment"));
    comments.put("commented.flat", List.of("flat comment", "two\nlines"));
    comments.put("pool", List.of("pool comment")); // not a dotted path: JSON5 doesn't use it

    StringWriter out = new StringWriter();
    try (PrintWriter writer = new PrintWriter(out)) {
      new Json5ValueWriter().write(values, comments, writer, new Settings());
    }
    String expected =
        """
        {
          top: 1,
          plain: {
            x: {
              y: 2
            },
            pool: {
              z: 5
            }
          },
          commented: {
            inner: {
              // deep comment
              deep: 3
            },
            // flat comment
            // two
            // lines
            flat: 4
          }
        }\
        """;
    Assertions.assertEquals(expected, out.toString());
    Map<?, ?> read =
        new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
            .create()
            .fromJson(out.toString(), Map.class);
    Map<?, ?> inner = (Map<?, ?>) ((Map<?, ?>) read.get("commented")).get("inner");
    Assertions.assertEquals(new BigDecimal("3"), inner.get("deep"));
  }
}
