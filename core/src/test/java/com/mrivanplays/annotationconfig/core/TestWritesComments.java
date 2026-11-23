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

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** A value writer that doesn't write comments gets none; every other one gets all of them. */
class TestWritesComments {

  static class Config {

    @Comment("the name")
    String name = "x";

    @Comment("the section")
    @ConfigObject
    Section section = new Section();

    static class Section {

      @Comment("the port")
      int port = 1;
    }
  }

  /** Records the comments it gets. */
  static final class Recording implements ValueWriter {

    private final boolean writesComments;
    Map<String, List<String>> comments;

    Recording(boolean writesComments) {
      this.writesComments = writesComments;
    }

    @Override
    public void write(
        Map<String, Object> values,
        Map<String, List<String>> fieldComments,
        PrintWriter writer,
        Settings settings) {
      this.comments = fieldComments;
    }

    @Override
    public boolean writesComments() {
      return this.writesComments;
    }
  }

  private static Map<String, List<String>> dump(Recording writer) {
    ConfigResolver resolver =
        ConfigResolver.newBuilder()
            .withValueWriter(writer)
            .withValueReader((_, _) -> Collections.emptyMap())
            .withFileExtension(".test")
            .build();
    resolver.dump(new Config(), new StringWriter());
    return writer.comments;
  }

  @Test
  void aWriterWithCommentsGetsThem() {
    Assertions.assertEquals(
        Map.of(
            "name", List.of("the name"),
            "section", List.of("the section"),
            "section.port", List.of("the port")),
        dump(new Recording(true)));
  }

  @Test
  void aWriterWithoutCommentsGetsNone() {
    Assertions.assertEquals(Map.of(), dump(new Recording(false)));
  }
}
