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

import com.mrivanplays.annotationconfig.core.resolver.WritableObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResolveMultipleTest {

  static class Subject {

    int foo = 2000;

    Subject() {}

    Subject(int foo) {
      this.foo = foo;
    }
  }

  private static Path dir;

  @BeforeAll
  static void loadDifferentFiles() {
    try {
      dir = Files.createTempDirectory("resolveMultipleTest");
      for (int i = 0; i < 3; i++) {
        Path file = Files.createTempFile(dir, "test" + i, ".properties");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
          writer.println("foo=" + (1000 + i));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testResolveMultiple() {
    Map<String, Subject> loadedConfigs =
        PropertyConfig.getConfigResolver()
            .resolveMultiple(
                dir,
                Subject::new,
                WritableObject.createFromPath(dir.resolve("default.properties")));

    int firstFoo =
        loadedConfigs.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("test0"))
            .findFirst()
            .orElse(new AbstractMap.SimpleEntry<>("foo", new Subject(69420)))
            .getValue()
            .foo;
    Assertions.assertEquals(1000, firstFoo);
  }

  @AfterAll
  static void terminate() {
    try (Stream<Path> stream = Files.list(dir)) {
      Iterator<Path> iterator = stream.iterator();
      while (iterator.hasNext()) {
        Files.delete(iterator.next());
      }
      Files.delete(dir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
