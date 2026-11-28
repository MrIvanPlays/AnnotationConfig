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

package com.mrivanplays.annotationconfig.scenarios.schema;

import com.mrivanplays.annotationconfig.core.annotations.ConfigVersion;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code SCHEMA_REFERENCE} makes a dump reference a schema the way each format's editors read. */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestSchemaReference extends Scenario {

  TestSchemaReference(Format format) {
    super(format, "schema");
  }

  private static Settings reference(String schema) {
    return new Settings().put(ACDefaultSettings.SCHEMA_REFERENCE, schema);
  }

  // <editor-fold desc="Reference">

  @Test
  void theReferenceComesFirst() {
    // before the class comments and the version
    assertOutput("reference", dump(new Referenced(), reference("config.schema.json")));
  }

  @Test
  void withoutTheSettingThereIsNoReference() {
    assertOutput("no-reference", dump(new Referenced()));
  }

  @Test
  void loadingIgnoresTheReference() {
    Referenced written = new Referenced();
    written.port = 8080;
    Referenced read = loadText(new Referenced(), dump(written, reference("config.schema.json")));

    Assertions.assertEquals(8080, read.port);
  }

  @Test
  void aFileWrittenAgainKeepsTheReference(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config." + this.format.extension());
    Files.writeString(file, dump(new Older(), reference("config.schema.json")));

    // the file misses an option, so it is written again with it
    Settings settings =
        reference("config.schema.json").put(ACDefaultSettings.GENERATE_NEW_OPTIONS, true);
    this.resolver.load(new Referenced(), file, settings);

    Assertions.assertEquals(dump(new Referenced(), settings), Files.readString(file));
  }

  @Test
  void aReferenceWithALineBreakIsRejected() {
    IllegalArgumentException e =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> dump(new Referenced(), reference("a.json\n# injected")));

    Assertions.assertTrue(e.getMessage().contains("line break"), e.getMessage());
  }

  @Test
  void anOptionCantTakeTheKeyOfTheReference() {
    if (this.format != Format.JSON && this.format != Format.JSON5) {
      // the other formats reference the schema in a comment, which takes no key
      Assertions.assertDoesNotThrow(() -> dump(new SchemaKey(), reference("a.json")));
      return;
    }
    IllegalArgumentException e =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> dump(new SchemaKey(), reference("a.json")));

    Assertions.assertTrue(e.getMessage().contains("Duplicate key '$schema'"), e.getMessage());
  }

  // </editor-fold>

  // <editor-fold desc="Reference: subjects">

  @Comment("The server's settings")
  @ConfigVersion(1)
  static class Referenced {

    int port = 25565;

    String motd = "hi";
  }

  @Comment("The server's settings")
  @ConfigVersion(1)
  static class Older {

    int port = 25565;
  }

  static class SchemaKey {

    @Key("$schema")
    String schema = "mine";
  }

  // </editor-fold>
}
