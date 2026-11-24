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
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Config files on the file system: what dumping does to directories, permissions and links, empty
 * files, directories and file extensions.
 */
class TestConfigFiles {

  private static final ConfigResolver RESOLVER = PropertyConfig.getConfigResolver();

  private static boolean posix() {
    return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
  }

  @Test
  void missingDirectoriesAreCreated(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("plugins").resolve("example").resolve("config.properties");
    // failed with a NoSuchFileException before
    RESOLVER.dump(new Subject(), file);
    Assertions.assertEquals("value=1\n", Files.readString(file));
  }

  @Test
  void aDumpThatFailsLeavesTheOldFile(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config.properties");
    Files.writeString(file, "value=7\n");
    Assertions.assertThrows(RuntimeException.class, () -> RESOLVER.dump(new Broken(), file));
    Assertions.assertEquals("value=7\n", Files.readString(file));
    try (var files = Files.list(dir)) {
      Assertions.assertEquals(1, files.count(), "no temporary file is left");
    }
  }

  @Test
  void aDumpThatFailsCreatesNoFile(@TempDir Path dir) {
    Path file = dir.resolve("config.properties");
    Assertions.assertThrows(RuntimeException.class, () -> RESOLVER.dump(new Broken(), file));
    Assertions.assertTrue(Files.notExists(file));
  }

  @Test
  void anExistingFileKeepsItsPermissions(@TempDir Path dir) throws IOException {
    Assumptions.assumeTrue(posix(), "POSIX permissions");
    Path file = dir.resolve("config.properties");
    Files.writeString(file, "value=7\n");
    Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-rw-r--");
    Files.setPosixFilePermissions(file, permissions);
    RESOLVER.dump(new Subject(), file);
    // the file was replaced by a temporary file, readable by its owner only, before
    Assertions.assertEquals(permissions, Files.getPosixFilePermissions(file));
  }

  @Test
  void aNewFileGetsThePermissionsOfANewFile(@TempDir Path dir) throws IOException {
    Assumptions.assumeTrue(posix(), "POSIX permissions");
    Path plain = Files.createFile(dir.resolve("plain"));
    Path file = dir.resolve("config.properties");
    RESOLVER.dump(new Subject(), file);
    Assertions.assertEquals(
        Files.getPosixFilePermissions(plain), Files.getPosixFilePermissions(file));
  }

  @Test
  void aSymbolicLinkStaysALink(@TempDir Path dir) throws IOException {
    Path target = dir.resolve("real.properties");
    Files.writeString(target, "value=7\n");
    Path link = dir.resolve("config.properties");
    try {
      Files.createSymbolicLink(link, target.getFileName());
    } catch (IOException | UnsupportedOperationException e) {
      Assumptions.abort("symbolic links can't be created here: " + e);
    }
    RESOLVER.dump(new Subject(), link);
    // the link was replaced by a regular file before
    Assertions.assertTrue(Files.isSymbolicLink(link));
    Assertions.assertEquals("value=1\n", Files.readString(target));
  }

  @Test
  void anEmptyFileGetsTheDefaults(@TempDir Path dir) throws IOException {
    Path file = Files.createFile(dir.resolve("config.properties"));
    RESOLVER.loadOrDump(new Subject(), file);
    // left empty before: nothing was read, so nothing counted as missing
    Assertions.assertEquals("value=1\n", Files.readString(file));
  }

  @Test
  void loadingADirectoryFails(@TempDir Path dir) {
    // returned without loading anything before, while dump rejected a directory
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> RESOLVER.load(new Subject(), dir));
  }

  @Test
  void resolveMultipleIgnoresTheCaseOfExtensions(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("UPPER.PROPERTIES"), "value=2\n");
    Files.writeString(dir.resolve("lower.properties"), "value=3\n");
    Files.writeString(dir.resolve("notproperties"), "value=4\n");
    Map<String, Subject> read =
        RESOLVER.resolveMultiple(
            dir, Subject::new, WritableObject.createFromWriter(new StringWriter()));
    // UPPER.PROPERTIES was not found before
    Assertions.assertEquals(Set.of("UPPER.PROPERTIES", "lower.properties"), read.keySet());
    Assertions.assertEquals(2, read.get("UPPER.PROPERTIES").value);
  }

  static class Subject {

    int value = 1;
  }

  static class Broken {

    // dump requires every field to have a value
    String value = null;
  }
}
