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

package com.mrivanplays.annotationconfig.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the default toml value writer. It writes TOML 1.0:
 *
 * <ul>
 *   <li>Every top-level option is separated by an empty line, and its comments are written above
 *       it. Config objects and other objects are written as dotted keys ({@code server.ip = ...}),
 *       one line per value, each with its own comments.
 *   <li>Strings are written as literal strings ({@code 'text'}) when they contain nothing that
 *       needs escaping, as basic strings ({@code "te\"xt"}) otherwise, and {@code @Multiline}
 *       strings as multi-line basic strings ({@code """}).
 *   <li>Floats use {@code inf} and {@code nan}. Integers that do not fit in 64 bits, which TOML
 *       does not allow, are written as strings.
 *   <li>Date and time values ({@link OffsetDateTime}, {@link LocalDateTime}, {@link LocalDate},
 *       {@link LocalTime}) are written as TOML date-times.
 *   <li>Arrays and lists are written as arrays, objects inside them as inline tables.
 * </ul>
 *
 * @author claude, Ivan Pekov
 * @since 2.1.0
 */
@NullMarked
public final class TomlValueWriter implements ValueWriter {

  private static final Pattern BARE_KEY = Pattern.compile("[A-Za-z0-9_-]+");

  // Jackson's TOML parser (2.22) reads integers with 19 digits that fit in a long without their
  // first 9 digits (9223372036854775807 is read as 6854775807). Such values are written as strings,
  // which the default serializer reads back exactly.
  private static final long JACKSON_SAFE_LIMIT = 1_000_000_000_000_000_000L;

  public TomlValueWriter() {}

  /**
   * Creates a new value writer.
   *
   * @param defaultMapper not used: values are written by this writer, the mapper is only used for
   *     reading
   * @deprecated use {@link #TomlValueWriter()}
   */
  @Deprecated(since = "3.1.0")
  public TomlValueWriter(TomlMapper defaultMapper) {
    Objects.requireNonNull(defaultMapper, "defaultMapper");
  }

  @Override
  public void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      Settings settings) {
    boolean first = true;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      if (!first) {
        writer.println();
      }
      first = false;
      List<String> path = new ArrayList<>();
      path.add(entry.getKey());
      writeEntry(writer, path, entry.getValue(), fieldComments);
    }
  }

  private void writeEntry(
      PrintWriter writer,
      List<String> path,
      @Nullable Object value,
      Map<String, List<String>> fieldComments) {
    // comments are looked up by the exact dotted path, so "port" never gets the comments of
    // "portal" and a value inside a section gets its own comments
    List<String> comments = fieldComments.get(String.join(".", path));
    if (comments != null) {
      for (String comment : comments) {
        for (String line : comment.split("\\R", -1)) {
          writer.println(line.isEmpty() ? "#" : "# " + line);
        }
      }
    }
    if (value instanceof DataObject data) {
      value = data.isSingleValue() ? data.getAsObject() : data.getAsMap();
    }
    if (value instanceof Map<?, ?> map && !map.isEmpty()) {
      for (Map.Entry<?, ?> child : map.entrySet()) {
        List<String> childPath = new ArrayList<>(path);
        childPath.add(String.valueOf(child.getKey()));
        writeEntry(writer, childPath, child.getValue(), fieldComments);
      }
      return;
    }
    StringJoiner key = new StringJoiner(".");
    for (String segment : path) {
      key.add(formatKey(segment));
    }
    writer.println(key + " = " + formatValue(path, value));
  }

  private static String formatKey(String key) {
    if (BARE_KEY.matcher(key).matches()) {
      return key;
    }
    return formatString(key);
  }

  private static String formatValue(List<String> path, @Nullable Object value) {
    return switch (value) {
      case null ->
          throw new IllegalArgumentException(
              "'" + String.join(".", path) + "': TOML has no null value");
      case DataObject data ->
          formatValue(path, data.isSingleValue() ? data.getAsObject() : data.getAsMap());
      case MultilineString multiline -> formatMultilineString(multiline.getString());
      case String string -> formatString(string);
      case Character character -> formatString(String.valueOf(character));
      case Boolean bool -> bool.toString();
      case Byte _, Short _, Integer _ -> value.toString();
      case Long number -> formatLong(number);
      case BigInteger number ->
          number.bitLength() < Long.SIZE
              ? formatLong(number.longValue())
              : formatString(number.toString());
      case Double number -> formatDouble(number);
      case Float number -> formatFloat(number);
      // TOML floats only need an integer part followed by a fraction or exponent, which is what
      // BigDecimal#toString writes; a scale of 0 is written as an integer
      case BigDecimal number -> number.toString();
      case OffsetDateTime dateTime -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
      case ZonedDateTime dateTime -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime);
      case Instant instant -> DateTimeFormatter.ISO_INSTANT.format(instant);
      case LocalDateTime dateTime -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
      case LocalDate date -> DateTimeFormatter.ISO_LOCAL_DATE.format(date);
      case LocalTime time -> DateTimeFormatter.ISO_LOCAL_TIME.format(time);
      case Map<?, ?> map -> formatInlineTable(path, map);
      case Collection<?> collection -> formatArray(path, collection);
      default -> {
        if (value.getClass().isArray()) {
          int length = Array.getLength(value);
          List<Object> elements = new ArrayList<>(length);
          for (int i = 0; i < length; i++) {
            elements.add(Array.get(value, i));
          }
          yield formatArray(path, elements);
        }
        throw new IllegalArgumentException(
            "'"
                + String.join(".", path)
                + "': cannot write a value of type "
                + value.getClass().getName()
                + " as TOML");
      }
    };
  }

  private static String formatArray(List<String> path, Collection<?> elements) {
    StringJoiner joiner = new StringJoiner(", ", "[", "]");
    for (Object element : elements) {
      joiner.add(formatValue(path, element));
    }
    return joiner.toString();
  }

  private static String formatInlineTable(List<String> path, Map<?, ?> table) {
    if (table.isEmpty()) {
      return "{}";
    }
    StringJoiner joiner = new StringJoiner(", ", "{", "}");
    for (Map.Entry<?, ?> entry : table.entrySet()) {
      String key = String.valueOf(entry.getKey());
      List<String> entryPath = new ArrayList<>(path);
      entryPath.add(key);
      joiner.add(formatKey(key) + " = " + formatValue(entryPath, entry.getValue()));
    }
    return joiner.toString();
  }

  private static String formatLong(long value) {
    if (value >= JACKSON_SAFE_LIMIT || value <= -JACKSON_SAFE_LIMIT) {
      return formatString(Long.toString(value));
    }
    return Long.toString(value);
  }

  private static String formatDouble(double value) {
    if (Double.isNaN(value)) {
      return "nan";
    }
    if (Double.isInfinite(value)) {
      return value > 0 ? "inf" : "-inf";
    }
    return Double.toString(value);
  }

  private static String formatFloat(float value) {
    if (Float.isNaN(value)) {
      return "nan";
    }
    if (Float.isInfinite(value)) {
      return value > 0 ? "inf" : "-inf";
    }
    return Float.toString(value);
  }

  /** A literal string when nothing needs escaping, a basic string otherwise. */
  private static String formatString(String value) {
    boolean literal = true;
    for (int i = 0; i < value.length() && literal; i++) {
      char c = value.charAt(i);
      literal = c != '\'' && !isControl(c, true);
    }
    if (literal) {
      return "'" + value + "'";
    }
    StringBuilder builder = new StringBuilder(value.length() + 8).append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> builder.append("\\\"");
        case '\\' -> builder.append("\\\\");
        case '\n' -> builder.append("\\n");
        default -> appendChar(builder, c);
      }
    }
    return builder.append('"').toString();
  }

  /**
   * A multi-line basic string. The line break after the opening delimiter is not part of the value,
   * and line breaks are written as {@code \n} so the value reads back the same on every platform.
   */
  private static String formatMultilineString(String value) {
    StringBuilder builder = new StringBuilder(value.length() + 8).append("\"\"\"\n");
    int quotes = 0;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '"') {
        // at most two quotes in a row, a third would close the string
        quotes++;
        builder.append(quotes == 3 ? "\\\"" : "\"");
        if (quotes == 3) {
          quotes = 0;
        }
        continue;
      }
      quotes = 0;
      switch (c) {
        case '\\' -> builder.append("\\\\");
        case '\n' -> builder.append('\n');
        default -> appendChar(builder, c);
      }
    }
    return builder.append("\"\"\"").toString();
  }

  private static void appendChar(StringBuilder builder, char c) {
    switch (c) {
      case '\b' -> builder.append("\\b");
      case '\t' -> builder.append("\\t");
      case '\f' -> builder.append("\\f");
      case '\r' -> builder.append("\\r");
      default -> {
        if (isControl(c, false)) {
          builder.append(String.format("\\u%04X", (int) c));
        } else {
          builder.append(c);
        }
      }
    }
  }

  /** Control characters may not appear unescaped in TOML strings; tab is allowed if requested. */
  private static boolean isControl(char c, boolean tabAllowed) {
    if (c == '\t') {
      return !tabAllowed;
    }
    return c < 0x20 || c == 0x7f;
  }
}
