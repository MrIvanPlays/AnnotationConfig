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

package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A <a href="https://json-schema.org/">JSON Schema</a> of a config's files, made by {@link
 * ConfigResolver#schema(Object)}. Point an editor at the file {@link #write(Path)} writes to get
 * completion and checks while a config is edited; with {@link
 * com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings#SCHEMA_REFERENCE} set,
 * a dump writes the reference into the config itself:
 *
 * <ul>
 *   <li>JSON: a {@code "$schema"} key, or {@code json.schemas} in the settings of VS Code
 *   <li>YAML: a {@code # yaml-language-server: $schema=config.schema.json} comment, with the YAML
 *       extension of VS Code
 *   <li>TOML: a {@code #:schema ./config.schema.json} comment, with Taplo (Even Better TOML)
 * </ul>
 *
 * @author Ivan Pekov
 * @since 3.1.1
 */
@NullMarked
public final class ConfigSchema {

  private final Map<String, Object> schema;

  /**
   * Wraps {@code schema}, which is copied.
   *
   * @param schema a JSON Schema: maps with {@code String} keys, lists, strings, numbers, booleans
   *     and {@code null}
   * @throws IllegalArgumentException if it holds anything else
   */
  public ConfigSchema(Map<String, Object> schema) {
    this.schema =
        MapUtils.unmodifiableDeepCopy(
            Objects.requireNonNull(schema, "schema"), ConfigSchema::requireJsonValue);
  }

  /** {@code value}, a value that is neither a map nor a list, if JSON can hold it. */
  private static @Nullable Object requireJsonValue(@Nullable Object value) {
    if (value == null
        || value instanceof String
        || value instanceof Boolean
        || value instanceof Number) {
      return value;
    }
    throw new IllegalArgumentException("Not a JSON value: " + value.getClass().getName());
  }

  /**
   * The schema as maps, lists, strings, numbers and booleans, in the order it is written.
   *
   * @return the schema; it can't be modified
   */
  public Map<String, Object> asMap() {
    return this.schema;
  }

  /**
   * The schema as JSON, indented by two spaces, ending with a line break.
   *
   * @return the JSON text
   */
  public String toJson() {
    StringBuilder json = new StringBuilder();
    writeValue(json, this.schema, "");
    return json.append('\n').toString();
  }

  /**
   * Writes {@link #toJson()} to {@code writer}, which is flushed but not closed.
   *
   * @param writer where to write
   * @throws UncheckedIOException if writing fails
   */
  public void write(Writer writer) {
    Objects.requireNonNull(writer, "writer");
    try {
      writer.write(this.toJson());
      writer.flush();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write the schema", e);
    }
  }

  /**
   * Writes {@link #toJson()} to {@code file} as UTF-8, creating missing directories and replacing
   * an existing file, as {@link #write(Path)} does.
   *
   * @param file the file to write, such as {@code config.schema.json}
   * @throws UncheckedIOException if writing fails
   */
  public void write(File file) {
    Objects.requireNonNull(file, "file");
    this.write(file.toPath());
  }

  /**
   * Writes {@link #toJson()} to {@code path} as UTF-8, creating missing directories and replacing
   * an existing file.
   *
   * @param path the file to write, such as {@code config.schema.json}
   * @throws UncheckedIOException if writing fails
   */
  public void write(Path path) {
    Objects.requireNonNull(path, "path");
    try {
      Path parent = path.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(path, this.toJson(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to write the schema to " + path, e);
    }
  }

  /**
   * Writes {@link #toJson()} to {@code file} if nothing exists there yet, as {@link
   * #writeIfMissing(Path)} does.
   *
   * @param file the file to write, such as {@code config.schema.json}
   * @throws UncheckedIOException if writing fails
   */
  public void writeIfMissing(File file) {
    Objects.requireNonNull(file, "file");
    if (file.exists()) {
      return;
    }
    this.write(file.toPath());
  }

  /**
   * Writes {@link #toJson()} to {@code path} as {@link #write(Path)} does, but only if nothing
   * exists there yet: an existing file, or a directory, is left as it is.
   *
   * <p>An existing schema is not updated, also when it is outdated: when the config class changes,
   * its schema changes too. To keep the schema in step with the class, use {@link #write(Path)},
   * which replaces it.
   *
   * @param path the file to write, such as {@code config.schema.json}
   * @throws UncheckedIOException if writing fails
   */
  public void writeIfMissing(Path path) {
    Objects.requireNonNull(path, "path");
    if (Files.exists(path)) {
      return;
    }
    this.write(path);
  }

  private static void writeValue(StringBuilder json, @Nullable Object value, String indent) {
    switch (value) {
      case null -> json.append("null");
      case String string -> writeString(json, string);
      case Boolean bool -> json.append(bool);
      // JSON has no NaN or infinities
      case Double d when !Double.isFinite(d) -> json.append("null");
      case Float f when !Float.isFinite(f) -> json.append("null");
      // the float's shortest decimal form: 0.1f is 0.1, not 0.10000000149011612
      case Float f -> json.append(new BigDecimal(Float.toString(f)));
      case Number number -> json.append(number);
      case Map<?, ?> map -> writeMap(json, map, indent);
      case List<?> list -> writeList(json, list, indent);
      default -> throw new IllegalStateException("Not a JSON value: " + value.getClass());
    }
  }

  private static void writeMap(StringBuilder json, Map<?, ?> map, String indent) {
    if (map.isEmpty()) {
      json.append("{}");
      return;
    }
    String inner = indent + "  ";
    json.append('{');
    boolean first = true;
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      json.append(first ? "\n" : ",\n").append(inner);
      first = false;
      writeString(json, (String) entry.getKey());
      json.append(": ");
      writeValue(json, entry.getValue(), inner);
    }
    json.append('\n').append(indent).append('}');
  }

  private static void writeList(StringBuilder json, List<?> list, String indent) {
    if (list.isEmpty()) {
      json.append("[]");
      return;
    }
    String inner = indent + "  ";
    json.append('[');
    boolean first = true;
    for (Object element : list) {
      json.append(first ? "\n" : ",\n").append(inner);
      first = false;
      writeValue(json, element, inner);
    }
    json.append('\n').append(indent).append(']');
  }

  private static void writeString(StringBuilder json, String string) {
    json.append('"');
    for (int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      switch (c) {
        case '"' -> json.append("\\\"");
        case '\\' -> json.append("\\\\");
        case '\n' -> json.append("\\n");
        case '\r' -> json.append("\\r");
        case '\t' -> json.append("\\t");
        case '\b' -> json.append("\\b");
        case '\f' -> json.append("\\f");
        default -> {
          if (c < 0x20) {
            json.append(String.format("\\u%04x", (int) c));
          } else {
            json.append(c);
          }
        }
      }
    }
    json.append('"');
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return this == o || (o instanceof ConfigSchema other && this.schema.equals(other.schema));
  }

  @Override
  public int hashCode() {
    return this.schema.hashCode();
  }

  /**
   * The schema as JSON, see {@link #toJson()}.
   *
   * @return the JSON text
   */
  @Override
  public String toString() {
    return this.toJson();
  }
}
