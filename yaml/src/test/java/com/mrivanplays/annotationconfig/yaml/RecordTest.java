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

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RecordTest {

  record ConfigObj(
      String name, String type, String value, String foo, int bar, boolean perf, double aa) {

    // simulate having more than 1 ctor
    ConfigObj(String name) {
      this(name, null, null, null, -2, false, -3.5);
    }
  }

  static class Subj {

    @ConfigObject public ConfigObj b = new ConfigObj("aa", "bb", "cc", "aaaa", 255, true, 2.5);
  }

  // try non annotated
  static class Subj2 {
    public ConfigObj b = new ConfigObj("aa", "bb", "cc", "aaaa", 255, true, 2.5);
  }

  @Test
  void testDump() {
    StringWriter w = new StringWriter();
    Subj s = new Subj();
    YamlConfig.getConfigResolver().dump(s, w);

    String expected =
        "b:\n"
            + "  name: \"aa\"\n"
            + "  type: \"bb\"\n"
            + "  value: \"cc\"\n"
            + "  foo: \"aaaa\"\n"
            + "  bar: 255\n"
            + "  perf: true\n"
            + "  aa: 2.5\n\n";

    Assertions.assertEquals(expected, w.toString().replaceAll("\\R", "\n"));
  }

  @Test
  void testLoad() {
    String payload =
        "b:\n"
            + "  name: \"cc\"\n"
            + "  type: \"ppp\"\n"
            + "  value: \"llll\"\n"
            + "  foo: \"pong\"\n"
            + "  bar: 1\n"
            + "  perf: false\n"
            + "  aa: 6.9\n";

    StringReader r = new StringReader(payload);
    Subj s = new Subj();
    YamlConfig.getConfigResolver().load(s, r);

    Assertions.assertEquals("cc", s.b.name);
    Assertions.assertEquals("ppp", s.b.type);
    Assertions.assertEquals("llll", s.b.value);
    Assertions.assertEquals("pong", s.b.foo);
    Assertions.assertEquals(1, s.b.bar);
    Assertions.assertFalse(s.b.perf);
    Assertions.assertEquals(6.9, s.b.aa);
  }

  @Test
  void testLoadNonAnnotated() {
    String payload =
        "b:\n"
            + "  name: \"cc\"\n"
            + "  type: \"ppp\"\n"
            + "  value: \"llll\"\n"
            + "  foo: \"pong\"\n"
            + "  bar: 1\n"
            + "  perf: false\n"
            + "  aa: 6.9\n";

    StringReader r = new StringReader(payload);
    Subj2 s = new Subj2();
    YamlConfig.getConfigResolver().load(s, r);

    Assertions.assertEquals("cc", s.b.name);
    Assertions.assertEquals("ppp", s.b.type);
    Assertions.assertEquals("llll", s.b.value);
    Assertions.assertEquals("pong", s.b.foo);
    Assertions.assertEquals(1, s.b.bar);
    Assertions.assertFalse(s.b.perf);
    Assertions.assertEquals(6.9, s.b.aa);
  }
}
