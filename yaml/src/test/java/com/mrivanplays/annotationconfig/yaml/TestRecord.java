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
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestRecord {

  private static final ConfigObj LOADED = new ConfigObj("cc", "ppp", "llll", "pong", 1, false, 6.9);
  private static final String PAYLOAD =
      "b:\n"
          + "  name: \"cc\"\n"
          + "  type: \"ppp\"\n"
          + "  value: \"llll\"\n"
          + "  foo: \"pong\"\n"
          + "  bar: 1\n"
          + "  perf: false\n"
          + "  aa: 6.9\n";

  @Test
  void testDump() {
    StringWriter w = new StringWriter();
    YamlConfig.getConfigResolver().dump(new Subj(), w);
    String expected =
        "b:\n"
            + "  name: \"aa\"\n"
            + "  type: \"bb\"\n"
            + "  value: \"cc\"\n"
            + "  foo: \"aaaa\"\n"
            + "  bar: 255\n"
            + "  perf: true\n"
            + "  aa: 2.5\n";
    Assertions.assertEquals(expected, w.toString());
  }

  @Test
  void testLoad() {
    Subj s = new Subj();
    YamlConfig.getConfigResolver().load(s, new StringReader(PAYLOAD));
    Assertions.assertEquals(LOADED, s.b);
  }

  @Test
  void testLoadNonAnnotated() {
    Subj2 s = new Subj2();
    YamlConfig.getConfigResolver().load(s, new StringReader(PAYLOAD));
    Assertions.assertEquals(LOADED, s.b);
  }

  @Test
  void testNullRead() {
    // value is missing, so USE_DEFAULT_VALUE keeps the default component ("cc")
    String payload = PAYLOAD.replace("  value: \"llll\"\n", "");
    Subj s = new Subj();
    YamlConfig.getConfigResolver()
        .load(
            s,
            new StringReader(payload),
            ACDefaultSettings.getDefault()
                .copy()
                .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.USE_DEFAULT_VALUE));
    Assertions.assertEquals(new ConfigObj("cc", "ppp", "cc", "pong", 1, false, 6.9), s.b);
  }

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

  // the same record in a field that is not annotated with @ConfigObject
  static class Subj2 {

    public ConfigObj b = new ConfigObj("aa", "bb", "cc", "aaaa", 255, true, 2.5);
  }
}
