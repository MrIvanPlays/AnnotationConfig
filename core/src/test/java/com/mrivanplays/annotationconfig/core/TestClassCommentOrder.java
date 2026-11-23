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

package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.internal.AnnotatedConfigResolver;
import com.mrivanplays.annotationconfig.core.internal.AnnotationHolder;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * With {@code FIND_PARENT_FIELDS}, the comments of every class in the hierarchy are written, the
 * class a config extends before the config's class, then the fields. The order of the holders is a
 * consistent {@link Comparable} order, not one that depends on how a {@code TreeSet} inserts.
 */
class TestClassCommentOrder {

  private static final ConfigResolver RESOLVER = PropertyConfig.getConfigResolver();

  private static Settings findParentFields() {
    return new Settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true);
  }

  private static List<Integer> values(Child child) {
    return List.of(child.g1, child.g2, child.p1, child.p2, child.c1, child.c2);
  }

  @Test
  void ancestorsCommentsComeFirst() {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(new Child(), writer, findParentFields());
    // .properties writes the fields in reverse order
    String expected =
        """
        # grandparent comment

        # parent comment

        # child comment

        g2=2

        g1=1

        p2=4

        p1=3

        c2=6

        c1=5
        """;
    Assertions.assertEquals(expected, writer.toString());

    Child read = new Child();
    read.g1 = read.p1 = read.c2 = 0;
    RESOLVER.load(read, new StringReader(writer.toString()), findParentFields());
    Assertions.assertEquals(List.of(1, 2, 3, 4, 5, 6), values(read));
  }

  @Test
  void holdersHaveAConsistentOrder() {
    Settings settings = ACDefaultSettings.getDefault().copyWithOverrides(findParentFields());
    List<AnnotationHolder> holders =
        List.copyOf(AnnotatedConfigResolver.resolveAnnotationHolders(new Child(), settings));
    Assertions.assertEquals(9, holders.size()); // 3 classes, 6 fields
    for (AnnotationHolder a : holders) {
      // a class holder compared as smaller than itself before
      Assertions.assertEquals(0, a.compareTo(a), a.toString());
      for (AnnotationHolder b : holders) {
        if (a != b) {
          Assertions.assertEquals(
              -Integer.signum(b.compareTo(a)), Integer.signum(a.compareTo(b)), a + " " + b);
        }
      }
    }
    Assertions.assertEquals(Grandparent.class, holders.get(0).getClazz());
    Assertions.assertEquals(Parent.class, holders.get(1).getClazz());
    Assertions.assertEquals(Child.class, holders.get(2).getClazz());
  }

  @Comment("grandparent comment")
  static class Grandparent {

    int g1 = 1;
    int g2 = 2;
  }

  @Comment("parent comment")
  static class Parent extends Grandparent {

    int p1 = 3;
    int p2 = 4;
  }

  @Comment("child comment")
  static class Child extends Parent {

    int c1 = 5;
    int c2 = 6;
  }
}
