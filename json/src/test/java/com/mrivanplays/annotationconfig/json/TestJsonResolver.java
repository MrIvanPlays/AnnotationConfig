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

package com.mrivanplays.annotationconfig.json;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Resolver behaviour with JSON files: missing options, parent classes and static fields. */
public class TestJsonResolver {

  private static final ConfigResolver RESOLVER = JsonConfig.getConfigResolver();

  private static String dump(Object config, Settings settings) {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(config, writer, settings);
    return writer.toString();
  }

  // <editor-fold desc="Missing options">

  static class Version1 {

    public String foo = "foo";

    @ConfigObject public Section1 section = new Section1();

    public static class Section1 {

      public int a = 1;
    }
  }

  // Version1 with a changed default and options added, also inside the section
  static class Version2 {

    public String foo = "changed";
    public boolean added = true;

    @ConfigObject public Section2 section = new Section2();

    public static class Section2 {

      public int a = 2;
      public int b = 3;
    }
  }

  @Test
  public void missingOptionsAreAddedToTheFile(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config.json");
    RESOLVER.dump(new Version1(), file);

    Version2 config = new Version2();
    RESOLVER.load(config, file);

    // values in the file win over changed defaults, missing options get their default
    Assertions.assertEquals("foo", config.foo);
    Assertions.assertTrue(config.added);
    Assertions.assertEquals(1, config.section.a);
    Assertions.assertEquals(3, config.section.b);
    String expected =
        """
        {
          "foo": "foo",
          "added": true,
          "section": {
            "a": 1,
            "b": 3
          }
        }\
        """;
    Assertions.assertEquals(expected, Files.readString(file));
  }

  // </editor-fold>

  // <editor-fold desc="Parent and static fields">

  static class Base {

    public int base = 1;
  }

  static class Extended extends Base {

    public static int counter = 5;
    public String own = "own";
  }

  @Test
  public void parentFieldsOnlyWithFindParentFields() {
    Assertions.assertEquals("{\n  \"own\": \"own\"\n}", dump(new Extended(), new Settings()));
    Assertions.assertEquals(
        "{\n  \"own\": \"own\",\n  \"base\": 1\n}",
        dump(new Extended(), new Settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true)));
  }

  @Test
  public void staticFieldsOnlyWithFindStaticFields() {
    Settings findStatic = new Settings().put(ACDefaultSettings.FIND_STATIC_FIELDS, true);
    Assertions.assertEquals(
        "{\n  \"counter\": 5,\n  \"own\": \"own\"\n}", dump(new Extended(), findStatic));

    try {
      RESOLVER.load(new Extended(), new StringReader("{\"counter\": 9, \"own\": \"x\"}"));
      Assertions.assertEquals(5, Extended.counter, "loaded without FIND_STATIC_FIELDS");
      RESOLVER.load(
          new Extended(), new StringReader("{\"counter\": 9, \"own\": \"x\"}"), findStatic);
      Assertions.assertEquals(9, Extended.counter);
    } finally {
      Extended.counter = 5;
    }
  }

  // </editor-fold>
}
