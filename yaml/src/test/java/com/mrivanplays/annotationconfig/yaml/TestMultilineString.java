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
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMultilineString {

  static class Subject {

    @Key("control-field")
    public int controlField = -1;

    @Multiline('|')
    public String multiline =
"""
Look at ma!
I am a multiline string!
""";
  }

  static class FailSubject {

    @Multiline private int foo = 1;
  }

  @Test
  public void testFailSubject() {
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class,
        () -> {
          FailSubject subject = new FailSubject();
          YamlConfig.getConfigResolver().dump(subject, new StringWriter());
        });
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, writer);

    String expectedMultiline =
"""
  Look at ma!
  I am a multiline string!
""";

    String expected = "control-field: -1\n" + "\n" + "multiline: |\n" + expectedMultiline;

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  public void testLoad() {
    Subject config = new Subject();
    YamlConfig.getConfigResolver()
        .load(config, getClass().getClassLoader().getResourceAsStream("multiline-string.yml"));

    String expected = " Hello! \n" + " My name is Ivan! \n" + " I like to code! \n ";

    Assertions.assertEquals(expected, config.multiline);
    Assertions.assertEquals(-2, config.controlField);
  }
}
