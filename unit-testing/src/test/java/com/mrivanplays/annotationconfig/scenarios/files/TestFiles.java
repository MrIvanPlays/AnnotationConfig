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

package com.mrivanplays.annotationconfig.scenarios.files;

import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.WritableObject;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Config files in every format: missing options are added, changes are saved, an empty file gets
 * the defaults, and {@code resolveMultiple} reads the files with the format's extension.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestFiles extends Scenario {

  TestFiles(Format format) {
    super(format, "files");
  }

  /** A file name with the format's extension. */
  private String file(String name) {
    return name + "." + this.format.extension();
  }

  /** The text of a config whose option foo is {@code value}. */
  private String foo(int value) {
    return text("foo-template").replace("%s", Integer.toString(value));
  }

  private Map<String, Subject> resolve(Path dir, Path defaultFile) {
    return this.resolver.resolveMultiple(
        dir, Subject::new, WritableObject.createFromPath(defaultFile));
  }

  // <editor-fold desc="Files">

  @Test
  void missingOptionsAreAddedToTheFile(@TempDir Path dir) throws IOException {
    Path file = dir.resolve(file("missing-options"));
    this.resolver.dump(new OlderSubject(), file);

    NewerSubject config = new NewerSubject();
    this.resolver.load(config, file, generateNewOptions());

    // the value in the file wins over the changed default, the missing option gets its default
    Assertions.assertEquals("foo", config.foo);
    Assertions.assertEquals("bar", config.bar);
    // the comments are kept
    assertOutput("missing-options-added", Files.readString(file));
  }

  @Test
  void dumpReplacesTheFileAndLoadReadsItBack(@TempDir Path dir) {
    Path file = dir.resolve(file("save-changes"));
    Subject subject = new Subject();
    this.resolver.dump(subject, file);

    subject.foo = 3;
    this.resolver.dump(subject, file);

    subject.foo = 4;
    this.resolver.load(subject, file);
    Assertions.assertEquals(3, subject.foo);
  }

  @Test
  void anEmptyFileGetsTheDefaults(@TempDir Path dir) throws IOException {
    Path file = Files.createFile(dir.resolve(file("empty")));
    this.resolver.loadOrDump(new Subject(), file, generateNewOptions());
    // left empty before: nothing was read, so nothing counted as missing
    assertOutput("defaults", Files.readString(file));
  }

  // </editor-fold>

  // <editor-fold desc="resolveMultiple">

  @Test
  void resolveMultipleLoadsEveryConfigSortedByFileName(@TempDir Path dir) throws IOException {
    for (int i = 2; i >= 0; i--) {
      Files.writeString(dir.resolve(file("test" + i)), foo(1000 + i));
    }

    Map<String, Subject> loaded = resolve(dir, dir.resolve(file("default")));

    Assertions.assertEquals(
        List.of(file("test0"), file("test1"), file("test2")), List.copyOf(loaded.keySet()));
    Assertions.assertEquals(1000, loaded.get(file("test0")).foo);
    Assertions.assertEquals(1001, loaded.get(file("test1")).foo);
    Assertions.assertEquals(1002, loaded.get(file("test2")).foo);
    Assertions.assertTrue(Files.notExists(dir.resolve(file("default"))));
  }

  @Test
  void resolveMultipleWithAFileGivesTheSameResult(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve(file("a")), foo(1));
    Map<String, Subject> loaded =
        this.resolver.resolveMultiple(
            dir.toFile(),
            Subject::new,
            WritableObject.createFromPath(dir.resolve(file("default"))));
    Assertions.assertEquals(List.of(file("a")), List.copyOf(loaded.keySet()));
    Assertions.assertEquals(1, loaded.get(file("a")).foo);
  }

  @Test
  void resolveMultipleCreatesAMissingDirectoryAndDumpsTheDefaults(@TempDir Path dir)
      throws IOException {
    Path configs = dir.resolve("configs");
    Path defaultFile = configs.resolve(file("default"));

    Map<String, Subject> loaded = resolve(configs, defaultFile);

    Assertions.assertTrue(loaded.isEmpty());
    Assertions.assertTrue(Files.isDirectory(configs));
    assertOutput("defaults", Files.readString(defaultFile));
  }

  @Test
  void resolveMultipleIgnoresOtherFilesAndDirectories(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("README.txt"), "not a config");
    Files.createDirectory(dir.resolve(file("nested")));
    Path defaultFile = dir.resolve(file("default"));

    Map<String, Subject> loaded = resolve(dir, defaultFile);

    // no configs found, so the defaults are dumped as for an empty directory
    Assertions.assertTrue(loaded.isEmpty());
    assertOutput("defaults", Files.readString(defaultFile));
  }

  @Test
  void resolveMultipleIgnoresTheCaseOfExtensions(@TempDir Path dir) throws IOException {
    String upper = "UPPER." + this.format.extension().toUpperCase(Locale.ROOT);
    Files.writeString(dir.resolve(upper), foo(2));
    Files.writeString(dir.resolve(file("lower")), foo(3));
    Files.writeString(dir.resolve("no" + this.format.extension()), foo(4));
    Map<String, Subject> read =
        this.resolver.resolveMultiple(
            dir, Subject::new, WritableObject.createFromWriter(new StringWriter()));
    // UPPER.PROPERTIES was not found before
    Assertions.assertEquals(Set.of(upper, file("lower")), read.keySet());
    Assertions.assertEquals(2, read.get(upper).foo);
  }

  @Test
  void resolveMultipleRejectsAFileInsteadOfADirectory(@TempDir Path dir) throws IOException {
    Path file = Files.writeString(dir.resolve(file("file")), foo(1));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> resolve(file, dir.resolve(file("default"))));
  }

  // </editor-fold>

  // <editor-fold desc="subjects">

  static class Subject {

    int foo = 2000;
  }

  @Comment("Comments should be kept even after regeneration!")
  static class OlderSubject {

    @Comment("Foo comment kept")
    String foo = "foo";
  }

  // a newer version of OlderSubject: the default of foo changed and the option bar was added
  @Comment("Comments should be kept even after regeneration!")
  static class NewerSubject {

    @Comment("Foo comment kept")
    String foo = "baz";

    @Comment("Bar comment kept")
    String bar = "bar";
  }

  // </editor-fold>
}
