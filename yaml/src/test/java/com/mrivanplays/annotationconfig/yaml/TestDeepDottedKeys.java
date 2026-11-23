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

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Dotted keys with five parts that share their first four: both have to end up in one map. */
class TestDeepDottedKeys {

  private static final ConfigResolver RESOLVER = YamlConfig.getConfigResolver();
  private static final String EXPECTED =
      """
      a:
        b:
          c:
            d:
              x: 1
              y: 2
      """;

  @Test
  void bothKeysAreDumped() {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(new Subject(), writer);
    // only "y: 2" was written before
    Assertions.assertEquals(EXPECTED, writer.toString());
  }

  @Test
  void bothKeysAreLoaded() {
    Subject subject = new Subject();
    RESOLVER.load(
        subject,
        new StringReader(
            """
            a:
              b:
                c:
                  d:
                    x: 10
                    y: 20
            """));
    Assertions.assertEquals(10, subject.x);
    Assertions.assertEquals(20, subject.y);
  }

  @Test
  void whatIsDumpedLoadsBack() {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(new Subject(), writer);
    Subject subject = new Subject();
    subject.x = 0;
    subject.y = 0;
    RESOLVER.load(subject, new StringReader(writer.toString()));
    Assertions.assertEquals(1, subject.x);
    Assertions.assertEquals(2, subject.y);
  }

  static class Subject {

    @Key("a.b.c.d.x")
    int x = 1;

    @Key("a.b.c.d.y")
    int y = 2;
  }
}
