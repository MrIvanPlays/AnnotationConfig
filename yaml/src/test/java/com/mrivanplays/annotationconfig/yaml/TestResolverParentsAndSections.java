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
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestResolverParentsAndSections {

  private static final ConfigResolver RESOLVER = YamlConfig.getConfigResolver();

  private static Settings findParents() {
    return new Settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true);
  }

  @Comment("parent comment")
  public static class SectionParent {

    public int parentValue = 1;
  }

  @Comment("child comment")
  public static class Section extends SectionParent {

    public int childValue = 2;
  }

  public static class WithSection {

    @ConfigObject public Section section = new Section();
  }

  public static class NullSection {

    @ConfigObject public Section section = null;
  }

  private static int occurrences(String text, String part) {
    return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
  }

  @Test
  public void sectionKeepsParentClassComments() {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(new WithSection(), writer, findParents());
    String dumped = writer.toString();
    // the parent's comment used to be replaced by a second copy of the child's
    Assertions.assertEquals(1, occurrences(dumped, "child comment"), dumped);
    Assertions.assertEquals(1, occurrences(dumped, "parent comment"), dumped);
    Assertions.assertTrue(dumped.contains("parentValue: 1"), dumped);
  }

  @Test
  public void nullSectionDumpNamesTheField() {
    IllegalArgumentException e =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> RESOLVER.dump(new NullSection(), new StringWriter()));
    Assertions.assertTrue(e.getMessage().contains("'section'"), e.getMessage());
  }

  @Test
  public void localClassHierarchyWithParentFields() {
    class LocalBase {

      public int a = 1;
    }
    class Local extends LocalBase {

      public int b = 2;
    }
    // local classes have no canonical name, which used to throw a NullPointerException
    StringWriter writer = new StringWriter();
    RESOLVER.dump(new Local(), writer, findParents());
    String dumped = writer.toString();
    Assertions.assertTrue(dumped.contains("a: 1") && dumped.contains("b: 2"), dumped);
  }
}
