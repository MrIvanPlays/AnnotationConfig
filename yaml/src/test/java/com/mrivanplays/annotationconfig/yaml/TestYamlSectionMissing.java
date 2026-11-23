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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestYamlSectionMissing {

  @Comment("Comments should also be kept!")
  private static final class Subject1 {

    @Comment("This is test subject comment")
    @ConfigObject
    private TestSection test = new TestSection();

    public static final class TestSection {

      private String foo = "foo";
    }
  }

  // a newer version of Subject1: the default of foo changed and the option bar was added
  @Comment("Comments should also be kept!")
  private static final class Subject2 {

    @Comment("This is test subject comment")
    @ConfigObject
    private TestSection test = new TestSection();

    public static final class TestSection {

      private String foo = "baz";
      private String bar = "bar";
    }
  }

  private final ConfigResolver resolver = YamlConfig.getConfigResolver();

  @Test
  public void missingSectionOptionsAreAddedToTheFile(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("missing-opt.yml");
    resolver.dump(new Subject1(), file);

    Subject2 config = new Subject2();
    resolver.load(config, file);

    // the value in the file wins over the changed default, the missing option gets its default
    Assertions.assertEquals("foo", config.test.foo);
    Assertions.assertEquals("bar", config.test.bar);
    String expected =
        "# Comments should also be kept!\n"
            + "\n"
            + "# This is test subject comment\n"
            + "test:\n"
            + "  foo: \"foo\"\n"
            + "  bar: \"bar\"\n";
    Assertions.assertEquals(expected, Files.readString(file));
  }
}
