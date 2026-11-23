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

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Keys and values are escaped so that java.util.Properties reads back exactly what was written. */
class TestPropertiesEscaping {

  private static String dump() {
    StringWriter writer = new StringWriter();
    PropertyConfig.getConfigResolver().dump(new Subject(), writer);
    return writer.toString();
  }

  @Test
  void escapedOutput() {
    // the properties resolver writes the fields in reverse order
    String expected =
        String.join(
            "\n",
            "multiline=a\\n\\",
            "b",
            "",
            "newline=line1\\nline2",
            "",
            "control=bell\\u0007",
            "",
            "tab=a\\tb",
            "",
            "unicode=caf\\u00E9 \\u2603",
            "",
            "backslash=C:\\\\dir",
            "",
            "leading=\\ \\ indented",
            "",
            "sep\\=\\:\\#\\!=b",
            "",
            "spaced\\ key=a",
            "",
            "# first line",
            "# second line",
            "plain=nothing to escape = : # !",
            "");
    Assertions.assertEquals(expected, dump());
  }

  @Test
  void javaPropertiesReadsTheValuesBack() throws IOException {
    Properties properties = new Properties();
    properties.load(new StringReader(dump()));
    Subject expected = new Subject();
    Assertions.assertEquals(expected.plain, properties.getProperty("plain"));
    Assertions.assertEquals(expected.spacedKey, properties.getProperty("spaced key"));
    Assertions.assertEquals(expected.separators, properties.getProperty("sep=:#!"));
    Assertions.assertEquals(expected.leading, properties.getProperty("leading"));
    Assertions.assertEquals(expected.backslash, properties.getProperty("backslash"));
    Assertions.assertEquals(expected.unicode, properties.getProperty("unicode"));
    Assertions.assertEquals(expected.tab, properties.getProperty("tab"));
    Assertions.assertEquals(expected.control, properties.getProperty("control"));
    Assertions.assertEquals(expected.newline, properties.getProperty("newline"));
    Assertions.assertEquals(expected.multiline, properties.getProperty("multiline"));
  }

  static class Subject {

    @Comment("first line\nsecond line")
    public String plain = "nothing to escape = : # !";

    @Key("spaced key")
    public String spacedKey = "a";

    @Key("sep=:#!")
    public String separators = "b";

    public String leading = "  indented";
    public String backslash = "C:\\dir";
    public String unicode = "caf\u00e9 \u2603";
    public String tab = "a\tb";
    public String control = "bell\u0007";
    public String newline = "line1\nline2";

    @Multiline public String multiline = "a\nb";
  }
}
