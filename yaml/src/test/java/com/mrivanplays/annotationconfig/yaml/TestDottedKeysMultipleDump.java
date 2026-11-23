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
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDottedKeysMultipleDump {

  @Comment("Generated using AnnotationConfig v3.1.0")
  private static final class Subject {

    @ConfigObject private BarSection bar = new BarSection();

    @Comment("This is a test comment on top of a section")
    public static class BarSection {

      @Comment("Foo comment")
      @Key("baz.foo")
      private String foo = "bar";

      @Comment("Bar comment")
      @Key("baz.bar")
      private String bar = "foo";
    }

    @Comment("Foo comment")
    @Key("kappa.baz.foo")
    private String foo = "bar";

    @Comment("Bar comment")
    @Key("kappa.baz.bar")
    private String bar1 = "foo";

    @Comment("This is a control field")
    @Key("control-field")
    private int controlField = -1;
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter dumpTo = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, dumpTo);

    String expected =
        "# Generated using AnnotationConfig v3.1.0\n"
            + "\n"
            + "# This is a test comment on top of a section\n"
            + "bar:\n"
            + "  baz:\n"
            + "    # Foo comment\n"
            + "    foo: \"bar\"\n"
            + "    # Bar comment\n"
            + "    bar: \"foo\"\n"
            + "\n"
            + "kappa:\n"
            + "  baz:\n"
            + "    # Foo comment\n"
            + "    foo: \"bar\"\n"
            + "    # Bar comment\n"
            + "    bar: \"foo\"\n"
            + "\n"
            + "# This is a control field\n"
            + "control-field: -1\n";

    Assertions.assertEquals(expected, dumpTo.toString());
  }
}
