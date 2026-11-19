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
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ExtendedConfigurationTest {

  @Comment("Test comment foo")
  static class Subject {

    int foo = 1;
  }

  static class ExtendedSubject extends Subject {
    String bar = "baz";
  }

  @BeforeAll
  static void updateSettings() {
    PropertyConfig.getConfigResolver().settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true);
  }

  @Test
  void testDump() {
    ExtendedSubject subject = new ExtendedSubject();
    StringWriter writer = new StringWriter();
    PropertyConfig.getConfigResolver().dump(subject, writer);

    String expected = "# Test comment foo\n\nfoo=1\n\nbar=baz\n";

    Assertions.assertEquals(expected, writer.toString().replaceAll("\\R", "\n"));
  }

  @Test
  void testParse() {
    ExtendedSubject subject = new ExtendedSubject();
    StringReader reader = new StringReader("foo=2\n\nbar=aaa\n");
    PropertyConfig.getConfigResolver().load(subject, reader);

    Assertions.assertEquals(2, subject.foo);
    Assertions.assertEquals("aaa", subject.bar);
  }
}
