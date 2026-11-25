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

package com.mrivanplays.annotationconfig.scenarios.commentpaths;

import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comments are looked up by dotted path only in the maps that hold a commented key; the others are
 * written the same way without building the paths of their keys. YAML also looks up a key that
 * holds a map by its plain key, in every map: "pool" gets the comments of "pool" there, and nowhere
 * else. The expected outputs are the resources in {@code scenarios/comment-paths}.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_COMMENTED_SECTIONS)
class TestCommentPaths extends Scenario {

  TestCommentPaths(Format format) {
    super(format, "comment-paths");
  }

  private static Map<String, Object> map(Object... entries) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < entries.length; i += 2) {
      map.put((String) entries[i], entries[i + 1]);
    }
    return map;
  }

  @Test
  void commentsInSomeMapsOnly() throws IOException {
    final Map<String, Object> values =
        map(
            "top", 1,
            "plain", map("x", map("y", 2), "pool", map("z", 5)),
            "commented", map("inner", map("deep", 3), "flat", 4));
    Map<String, List<String>> comments = new LinkedHashMap<>();
    comments.put("top", List.of());
    comments.put("commented.inner.deep", List.of("deep comment"));
    comments.put("commented.flat", List.of("flat comment", "two\nlines"));
    comments.put("pool", List.of("pool comment"));

    StringWriter out = new StringWriter();
    try (PrintWriter writer = new PrintWriter(out)) {
      this.format.commentingWriter().write(values, comments, writer, new Settings());
    }
    assertOutput("output", out.toString());
    Assertions.assertEquals(
        numbersAsDecimals(values), numbersAsDecimals(this.format.parse(out.toString())));
  }
}
