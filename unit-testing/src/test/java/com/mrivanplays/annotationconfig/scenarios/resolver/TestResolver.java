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

package com.mrivanplays.annotationconfig.scenarios.resolver;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Missing options, in every format with sections: added to the file with {@code
 * GENERATE_NEW_OPTIONS}, which TOML keeps off by default.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SECTIONS)
class TestResolver extends Scenario {

  TestResolver(Format format) {
    super(format, "resolver");
  }

  private static void assertLoaded(Version2 config) {
    // values in the file win over changed defaults, missing options get their default
    Assertions.assertEquals("foo", config.foo);
    Assertions.assertTrue(config.added);
    Assertions.assertEquals(1, config.section.a);
    Assertions.assertEquals(3, config.section.b);
  }

  // <editor-fold desc="Missing options">

  @Test
  void missingOptionsAreAddedWithGenerateNewOptions(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config." + this.format.extension());
    this.resolver.dump(new Version1(), file);

    Version2 config = new Version2();
    this.resolver.load(config, file, generateNewOptions());

    assertLoaded(config);
    assertOutput("missing-options-added", Files.readString(file));
  }

  @Test
  void missingOptionsByDefault(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config." + this.format.extension());
    this.resolver.dump(new Version1(), file);
    final String written = Files.readString(file);

    Version2 config = new Version2();
    this.resolver.load(config, file);

    assertLoaded(config);
    if (this.format.has(Format.Feature.GENERATES_NEW_OPTIONS)) {
      assertOutput("missing-options-added", Files.readString(file));
    } else {
      // GENERATE_NEW_OPTIONS is off: the file is left as it was
      Assertions.assertEquals(written, Files.readString(file));
    }
  }

  // </editor-fold>

  // <editor-fold desc="Missing options: subjects">

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

    @Comment("added comment")
    public boolean added = true;

    @ConfigObject public Section2 section = new Section2();

    public static class Section2 {

      public int a = 2;
      public int b = 3;
    }
  }

  // </editor-fold>
}
