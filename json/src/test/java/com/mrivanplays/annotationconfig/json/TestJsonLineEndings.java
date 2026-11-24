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

import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * JSON and JSON5 are written with '\n' on every OS. The class comment of JSON5 used the OS line
 * separator, so files written on Windows mixed CRLF and LF.
 */
class TestJsonLineEndings {

  // not normalized: the line endings are what is tested
  private static String dump(ConfigResolver resolver) {
    StringWriter writer = new StringWriter();
    resolver.dump(new Subject(), writer);
    return writer.toString();
  }

  @Test
  void jsonUsesLineFeeds() {
    Assertions.assertEquals(
        "{\n  \"a\": 1,\n  \"b\": 2\n}\n", dump(JsonConfig.getConfigResolver()));
  }

  @Test
  void json5UsesLineFeeds() {
    Assertions.assertEquals(
        "// Class comment\n\n{\n  // field comment\n  a: 1,\n  b: 2\n}\n",
        dump(JsonConfig.getJson5ConfigResolver()));
  }

  @Comment("Class comment")
  static class Subject {

    @Comment("field comment")
    public int a = 1;

    public int b = 2;
  }
}
