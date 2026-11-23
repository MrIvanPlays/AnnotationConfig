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
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDottedKeys {

  private static final class Subject {

    @Comment("Mashallah")
    @Key("dotted.key.test.mashallah")
    private String mashallah = "foo";
  }

  private final ConfigResolver resolver = YamlConfig.getConfigResolver();

  @Test
  public void testValueGet() {
    Subject config = new Subject();
    resolver.load(config, getClass().getClassLoader().getResourceAsStream("dotted-keys.yml"));
    Assertions.assertEquals("bar", config.mashallah);
  }

  @Test
  public void testGeneration() {
    StringWriter writer = new StringWriter();
    resolver.dump(new Subject(), writer);
    String expected =
        "dotted:\n"
            + "  key:\n"
            + "    test:\n"
            + "      # Mashallah\n"
            + "      mashallah: \"foo\"\n";
    Assertions.assertEquals(expected, writer.toString());
  }
}
