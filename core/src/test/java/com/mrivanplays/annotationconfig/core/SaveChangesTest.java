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

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SaveChangesTest {

  static class Subject {

    String foo = "barbaba";
  }

  static File file;
  static ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @BeforeAll
  static void initialize() throws IOException {
    file = File.createTempFile("saveChangesTest", ".properties");
  }

  @AfterAll
  static void terminate() {
    file.delete();
    file = null;
  }

  @Test
  void perform() {
    Subject subject = new Subject();
    System.out.println("Before first dump: " + subject.foo);
    resolver.dump(subject, file);

    subject.foo = "www";
    System.out.println("Before second dump: " + subject.foo);
    resolver.dump(subject, file);

    // change value and force load
    subject.foo = "This should work";
    System.out.println("Before forced load: " + subject.foo);
    resolver.load(subject, file);
    System.out.println("After forced load: " + subject.foo);

    Assertions.assertEquals("www", subject.foo);
    System.out.println("Test completed successfully!");
  }
}
