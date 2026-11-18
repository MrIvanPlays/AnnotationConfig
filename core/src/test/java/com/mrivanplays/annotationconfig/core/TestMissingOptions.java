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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMissingOptions {

  @Comment("Comments should be kept even after regeneration!")
  private static final class Subject1 {

    @Comment("Foo comment kept")
    private String foo = "foo";
  }

  @Comment("Comments should be kept even after regeneration!")
  private static final class Subject2 {

    // emulate default value has changed from "foo" to "baz" and check if it will set the
    // field to the value in the config, which is "foo" at the time of loading this class
    @Comment("Foo comment kept")
    private String foo = "baz";

    @Comment("Bar comment kept")
    private String bar = "bar";
  }

  private static final File file = new File("missing-opt.properties");
  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testSubject1() {
    Subject1 config = new Subject1();
    resolver.dump(config, file);

    // have to load using properties to verify results
    Properties properties = new Properties();
    try (Reader reader = new FileReader(file)) {
      properties.load(reader);
    } catch (IOException e) {
      Assertions.fail(e);
      return;
    }

    Assertions.assertEquals("foo", properties.get("foo"));
    Assertions.assertNull(properties.get("bar"));
  }

  @Test
  public void testSubject2() {
    Subject2 config = new Subject2();
    resolver.load(config, file);

    Properties properties = new Properties();
    try (Reader reader = new FileReader(file)) {
      properties.load(reader);
    } catch (IOException e) {
      Assertions.fail(e);
      return;
    }

    Assertions.assertEquals("foo", properties.get("foo"));
    Assertions.assertEquals("bar", properties.get("bar"));
  }
}
