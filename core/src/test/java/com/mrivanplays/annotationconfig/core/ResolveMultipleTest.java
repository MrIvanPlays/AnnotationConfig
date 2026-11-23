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
import com.mrivanplays.annotationconfig.core.resolver.WritableObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResolveMultipleTest {

  static class Subject {

    int foo = 2000;
  }

  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  private Map<String, Subject> resolve(Path dir, Path defaultFile) {
    return resolver.resolveMultiple(dir, Subject::new, WritableObject.createFromPath(defaultFile));
  }

  private static String read(Path file) throws IOException {
    return Files.readString(file);
  }

  @Test
  void loadsEveryConfigSortedByFileName(@TempDir Path dir) throws IOException {
    for (int i = 2; i >= 0; i--) {
      Files.writeString(dir.resolve("test" + i + ".properties"), "foo=" + (1000 + i));
    }

    Map<String, Subject> loaded = resolve(dir, dir.resolve("default.properties"));

    Assertions.assertEquals(
        List.of("test0.properties", "test1.properties", "test2.properties"),
        List.copyOf(loaded.keySet()));
    Assertions.assertEquals(1000, loaded.get("test0.properties").foo);
    Assertions.assertEquals(1001, loaded.get("test1.properties").foo);
    Assertions.assertEquals(1002, loaded.get("test2.properties").foo);
    Assertions.assertTrue(Files.notExists(dir.resolve("default.properties")));
  }

  @Test
  void fileOverloadGivesTheSameResult(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("a.properties"), "foo=1");
    Map<String, Subject> loaded =
        resolver.resolveMultiple(
            dir.toFile(),
            Subject::new,
            WritableObject.createFromPath(dir.resolve("default.properties")));
    Assertions.assertEquals(List.of("a.properties"), List.copyOf(loaded.keySet()));
    Assertions.assertEquals(1, loaded.get("a.properties").foo);
  }

  @Test
  void missingDirectoryIsCreatedAndTheDefaultsAreDumped(@TempDir Path dir) throws IOException {
    Path configs = dir.resolve("configs");
    Path defaultFile = configs.resolve("default.properties");

    Map<String, Subject> loaded = resolve(configs, defaultFile);

    Assertions.assertTrue(loaded.isEmpty());
    Assertions.assertTrue(Files.isDirectory(configs));
    Assertions.assertEquals("foo=2000\n", read(defaultFile));
  }

  @Test
  void otherFilesAndDirectoriesAreIgnored(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("README.txt"), "not a config");
    Files.createDirectory(dir.resolve("nested.properties"));
    Path defaultFile = dir.resolve("default.properties");

    Map<String, Subject> loaded = resolve(dir, defaultFile);

    // no configs found, so the defaults are dumped as for an empty directory
    Assertions.assertTrue(loaded.isEmpty());
    Assertions.assertEquals("foo=2000\n", read(defaultFile));
  }

  @Test
  void fileInsteadOfDirectoryIsRejected(@TempDir Path dir) throws IOException {
    Path file = Files.writeString(dir.resolve("file.properties"), "foo=1");
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> resolve(file, dir.resolve("default.properties")));
  }
}
