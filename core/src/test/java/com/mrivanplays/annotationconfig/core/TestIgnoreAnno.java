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

import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestIgnoreAnno {

  @Comment("This is on the top")
  static class IgnoreAnnoTestSubject {

    @Ignore private boolean b = false;

    @Comment("Hello my name is Ivan")
    private String name = "Ivan";
  }

  private static final File file = new File("ignore-anno.properties");

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testIgnoreWorks() {
    IgnoreAnnoTestSubject config = new IgnoreAnnoTestSubject();
    PropertyConfig.getConfigResolver().dump(config, file);

    // now to make sure this works we have to read the file
    Properties properties = new Properties();
    try (Reader reader = new FileReader(file)) {
      properties.load(reader);
    } catch (IOException e) {
      Assertions.fail(e);
      return;
    }
    Assertions.assertFalse(properties.containsKey("b"));
    Assertions.assertTrue(properties.containsKey("name"));
  }
}
