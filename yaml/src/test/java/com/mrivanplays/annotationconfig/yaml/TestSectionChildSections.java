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
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// when a section has a child a section
class TestSectionChildSections {

  @Test
  void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();

    YamlConfig.getConfigResolver().dump(config, writer);
    String expected =
        "# Subject comment\n"
            + "\n"
            + "# This is top section comment\n"
            + "foo:\n"
            + "  # Control field comment\n"
            + "  control: \"asd\"\n"
            + "  # Child section comment\n"
            + "  barSect:\n"
            + "    # Bar comment\n"
            + "    bar: \"aabb\"\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  void testLoad() {
    Subject config = new Subject();
    YamlConfig.getConfigResolver()
        .load(
            config, getClass().getClassLoader().getResourceAsStream("section-child-sections.yml"));

    Assertions.assertEquals("foo", config.foo.barSect.bar);
    Assertions.assertEquals("fff", config.foo.control);
  }

  @Comment("Subject comment")
  static class Subject {

    @ConfigObject private Section foo = new Section();

    @Comment("This is top section comment")
    static class Section {

      @Comment("Control field comment")
      private String control = "asd";

      @ConfigObject private ChildSection barSect = new ChildSection();

      @Comment("Child section comment")
      static class ChildSection {

        @Comment("Bar comment")
        private String bar = "aabb";
      }
    }
  }
}
