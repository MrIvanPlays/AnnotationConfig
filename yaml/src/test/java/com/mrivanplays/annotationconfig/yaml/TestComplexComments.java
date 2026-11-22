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

public class TestComplexComments {

  @Comment("Class comment")
  static class Subject {

    @Comment("sectionOne comment")
    @ConfigObject
    public Section1 sectionOne = new Section1();

    public static class Section1 {

      @Comment("foo inside sectionOne")
      public String foo = "bb";
    }

    @Comment("sectionTwo comment")
    @ConfigObject
    public Section2 sectionTwo = new Section2();

    public static class Section2 {

      @Comment("foo inside sectionTwo")
      public String foo = "cc";
    }

    @Comment("foo outside config object")
    public String foo = "aa";
  }

  @Test
  public void testComplexComments() {
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(new Subject(), writer);
    String expected =
        "# Class comment\n"
            + "\n"
            + "# sectionOne comment\n"
            + "sectionOne:\n"
            + "  # foo inside sectionOne\n"
            + "  foo: \"bb\"\n"
            + "\n"
            + "# sectionTwo comment\n"
            + "sectionTwo:\n"
            + "  # foo inside sectionTwo\n"
            + "  foo: \"cc\"\n"
            + "\n"
            + "# foo outside config object\n"
            + "foo: \"aa\"\n";
    Assertions.assertEquals(expected, writer.toString().replaceAll("\\R", "\n"));
  }
}
