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
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestYamlSectionMissing {

  @Comment("Comments should also be kept!")
  private static final class Subject1 {

    @Comment("This is test subject comment")
    @ConfigObject
    private TestSection test = new TestSection();

    public static final class TestSection {

      private String foo = "foo";
    }

    public TestSection sect() {
      return test;
    }
  }

  @Comment("Comments should also be kept!")
  private static final class Subject2 {

    @Comment("This is test subject comment")
    @ConfigObject
    private TestSection test = new TestSection();

    public static final class TestSection {

      // check core/TestMissingOptions to see the reasoning of change of the default value
      private String foo = "baz";

      private String bar = "bar";
    }

    public TestSection sect() {
      return test;
    }
  }

  private static final File file = new File("missing-opt.yml");
  private final ConfigResolver resolver = YamlConfig.getConfigResolver();

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testSubject1() {
    Subject1 config = new Subject1();
    resolver.dump(config, file);

    Assertions.assertEquals("foo", config.sect().foo);
  }

  @Test
  public void testSubject2() {
    Subject2 config = new Subject2();
    resolver.load(config, file);

    Assertions.assertEquals("foo", config.sect().foo);
    Assertions.assertEquals("bar", config.sect().bar);
  }
}
