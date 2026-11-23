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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

/** Fields the compiler adds are not options, also not with {@code FIND_STATIC_FIELDS}. */
class TestSyntheticFields {

  private final ConfigResolver resolver = YamlConfig.getConfigResolver();

  static class InnerSectionConfig {

    String a = "y";

    @ConfigObject Inner inner = new Inner();

    // an inner class that uses its outer instance has the synthetic field this$0
    class Inner {

      String b = "x";

      String outer() {
        return a;
      }
    }
  }

  static class AssertConfig {

    String c = "z";

    // an assert statement adds the synthetic field $assertionsDisabled
    void check() {
      assert c != null;
    }
  }

  @Test
  void outerInstanceOfAnInnerSectionIsNotWritten() {
    StringWriter writer = new StringWriter();
    resolver.dump(new InnerSectionConfig(), writer);

    assertEquals(
        """
        a: "y"

        inner:
          b: "x"
        """,
        writer.toString());
  }

  @Test
  void outerInstanceOfAnInnerSectionIsNotRead() {
    InnerSectionConfig config = new InnerSectionConfig();
    InnerSectionConfig.Inner inner = config.inner;
    resolver.load(config, new StringReader("a: \"read\"\ninner:\n  b: \"read too\"\n"));

    assertSame(inner, config.inner);
    assertEquals("read too", config.inner.b);
    assertEquals("read", config.inner.outer());
  }

  @Test
  void assertionFlagIsNotWrittenWithStaticFields() {
    StringWriter writer = new StringWriter();
    resolver.dump(
        new AssertConfig(),
        writer,
        new Settings().put(ACDefaultSettings.FIND_STATIC_FIELDS, true));

    assertEquals("c: \"z\"\n", writer.toString());
  }
}
