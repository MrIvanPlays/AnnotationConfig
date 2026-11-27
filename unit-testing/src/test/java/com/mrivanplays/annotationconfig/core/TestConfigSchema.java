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

import com.mrivanplays.annotationconfig.core.resolver.ConfigSchema;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link ConfigSchema} writes JSON by hand; what a generated schema holds is in the scenarios. */
class TestConfigSchema {

  private static final ConfigSchema SCHEMA = new ConfigSchema(Map.of("type", "object"));

  private static final ConfigSchema OTHER = new ConfigSchema(Map.of("type", "string"));

  private static Map<String, Object> map(Object... entries) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < entries.length; i += 2) {
      map.put((String) entries[i], entries[i + 1]);
    }
    return map;
  }

  @Test
  void stringsAreEscaped() {
    ConfigSchema schema = new ConfigSchema(map("a\"b", "q\" b\\ n\n r\r t\t \u0001 \u00e9"));

    Assertions.assertEquals(
        "{\n  \"a\\\"b\": \"q\\\" b\\\\ n\\n r\\r t\\t \\u0001 \u00e9\"\n}\n", schema.toJson());
  }

  @Test
  void numbersAreWrittenExactly() {
    ConfigSchema schema =
        new ConfigSchema(
            map(
                "int", 1,
                "float", 0.1f,
                "double", 2.5,
                "decimal", new BigDecimal("0.10000000000000000001"),
                "big", new BigInteger("123456789012345678901234567890")));

    Assertions.assertEquals(
        """
        {
          "int": 1,
          "float": 0.1,
          "double": 2.5,
          "decimal": 0.10000000000000000001,
          "big": 123456789012345678901234567890
        }
        """,
        schema.toJson());
  }

  @Test
  void numbersJsonCantHoldAreNull() {
    ConfigSchema schema = new ConfigSchema(map("nan", Double.NaN, "inf", Float.POSITIVE_INFINITY));

    Assertions.assertEquals("{\n  \"nan\": null,\n  \"inf\": null\n}\n", schema.toJson());
  }

  @Test
  void emptyContainersStayOnOneLine() {
    ConfigSchema schema =
        new ConfigSchema(map("map", Map.of(), "list", List.of(), "nested", List.of(List.of())));

    Assertions.assertEquals(
        """
        {
          "map": {},
          "list": [],
          "nested": [
            []
          ]
        }
        """,
        schema.toJson());
    Assertions.assertEquals("{}\n", new ConfigSchema(Map.of()).toJson());
  }

  @Test
  void valuesJsonCantHoldAreRejected() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> new ConfigSchema(map("a", new Object())));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> new ConfigSchema(map("a", Map.of(1, "b"))));
  }

  @Test
  void theSchemaIsACopyThatCantBeModified() {
    List<Object> list = new ArrayList<>(List.of("a"));
    ConfigSchema schema = new ConfigSchema(map("list", list));
    list.add("b");

    Assertions.assertEquals(List.of("a"), schema.asMap().get("list"));
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> schema.asMap().put("b", "c"));
    Assertions.assertEquals(new ConfigSchema(map("list", List.of("a"))), schema);
  }

  // <editor-fold desc="Files">

  @Test
  void writeCreatesTheDirectoriesAndReplacesAFile(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("a").resolve("b").resolve("config.schema.json");
    SCHEMA.write(file);

    Assertions.assertEquals(SCHEMA.toJson(), Files.readString(file));
    OTHER.write(file);
    Assertions.assertEquals(OTHER.toJson(), Files.readString(file));
  }

  @Test
  void writeToAFileWritesAsToItsPath(@TempDir Path dir) throws IOException {
    File file = dir.resolve("a").resolve("config.schema.json").toFile();
    SCHEMA.write(file);

    Assertions.assertEquals(SCHEMA.toJson(), Files.readString(file.toPath()));
    OTHER.write(file);
    Assertions.assertEquals(OTHER.toJson(), Files.readString(file.toPath()));
  }

  @Test
  void writeIfMissingWritesAMissingFile(@TempDir Path dir) throws IOException {
    Path path = dir.resolve("a").resolve("path.schema.json");
    File file = dir.resolve("b").resolve("file.schema.json").toFile();
    SCHEMA.writeIfMissing(path);
    SCHEMA.writeIfMissing(file);

    Assertions.assertEquals(SCHEMA.toJson(), Files.readString(path));
    Assertions.assertEquals(SCHEMA.toJson(), Files.readString(file.toPath()));
  }

  @Test
  void writeIfMissingKeepsAnExistingFile(@TempDir Path dir) throws IOException {
    Path path = dir.resolve("path.schema.json");
    File file = dir.resolve("file.schema.json").toFile();
    SCHEMA.write(path);
    SCHEMA.write(file);

    // also when it is outdated
    OTHER.writeIfMissing(path);
    OTHER.writeIfMissing(file);

    Assertions.assertEquals(SCHEMA.toJson(), Files.readString(path));
    Assertions.assertEquals(SCHEMA.toJson(), Files.readString(file.toPath()));
  }

  @Test
  void aDirectoryIsLeftByWriteIfMissingAndRejectedByWrite(@TempDir Path dir) throws IOException {
    Path directory = Files.createDirectory(dir.resolve("config.schema.json"));

    SCHEMA.writeIfMissing(directory);
    SCHEMA.writeIfMissing(directory.toFile());
    Assertions.assertTrue(Files.isDirectory(directory));
    Assertions.assertThrows(UncheckedIOException.class, () -> SCHEMA.write(directory));
    Assertions.assertThrows(UncheckedIOException.class, () -> SCHEMA.write(directory.toFile()));
  }

  // </editor-fold>

  @Test
  void writingToAWriterWritesTheJson() {
    ConfigSchema schema = new ConfigSchema(map("a", true));
    StringWriter writer = new StringWriter();
    schema.write(writer);

    Assertions.assertEquals("{\n  \"a\": true\n}\n", writer.toString());
    Assertions.assertEquals(schema.toJson(), schema.toString());
  }
}
