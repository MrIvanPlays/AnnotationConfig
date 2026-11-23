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
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestMissingOptions {

  @Comment("Comments should be kept even after regeneration!")
  private static final class Subject1 {

    @Comment("Foo comment kept")
    private String foo = "foo";
  }

  // a newer version of Subject1: the default of foo changed and the option bar was added
  @Comment("Comments should be kept even after regeneration!")
  private static final class Subject2 {

    @Comment("Foo comment kept")
    private String foo = "baz";

    @Comment("Bar comment kept")
    private String bar = "bar";
  }

  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  private static String read(Path file) throws IOException {
    return Files.readString(file);
  }

  @Test
  public void missingOptionsAreAddedToTheFile(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("missing-opt.properties");
    resolver.dump(new Subject1(), file);
    Assertions.assertFalse(read(file).contains("bar"));

    Subject2 config = new Subject2();
    resolver.load(config, file);

    // the value in the file wins over the changed default, the missing option gets its default
    Assertions.assertEquals("foo", config.foo);
    Assertions.assertEquals("bar", config.bar);
    String expected =
        "# Comments should be kept even after regeneration!\n"
            + "\n"
            + "# Bar comment kept\n"
            + "bar=bar\n"
            + "\n"
            + "# Foo comment kept\n"
            + "foo=foo\n";
    Assertions.assertEquals(expected, read(file));
  }
}
