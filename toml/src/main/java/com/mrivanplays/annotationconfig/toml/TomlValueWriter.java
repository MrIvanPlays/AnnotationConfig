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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

  private static final char[] HEX = "0123456789ABCDEF".toCharArray();

  private static final Pattern LINE_BREAK = Pattern.compile("\\R");

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
    // one buffer for every line: values are appended to it instead of building strings per part
    StringBuilder line = new StringBuilder(256);
    boolean first = true;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      // '\n' and not println(): the line separator of the OS would mix with the '\n' of the class
      // comment
      if (!first) {
        writer.append('\n');
      }
      first = false;
      String key = entry.getKey();
      writeEntry(writer, line, key, formatKey(key), entry.getValue(), fieldComments);
    }
  }

  /**
   * Writes one option, or one line per value of a section.
   *
   * @param path the dotted key as the comments are stored under, also used in error messages
   * @param tomlKey the same key, quoted where TOML requires it
   */
  private static void writeEntry(
      PrintWriter writer,
      StringBuilder line,
      String path,
      String tomlKey,
      @Nullable Object value,
      Map<String, List<String>> fieldComments) {
    // comments are looked up by the exact dotted path, so "port" never gets the comments of
    // "portal" and a value inside a section gets its own comments
    if (!fieldComments.isEmpty()) {
      List<String> comments = fieldComments.get(path);
      if (comments != null) {
        writeComments(writer, comments);
      }
    }
    if (value instanceof DataObject data) {
      value = data.isSingleValue() ? data.getAsObject() : data.getAsMap();
    }
    if (value instanceof Map<?, ?> map && !map.isEmpty()) {
      for (Map.Entry<?, ?> child : map.entrySet()) {
        String key = String.valueOf(child.getKey());
        writeEntry(
            writer,
            line,
            path + '.' + key,
            tomlKey + '.' + formatKey(key),
            child.getValue(),
            fieldComments);
      }
      return;
    }
    line.setLength(0);
    line.append(tomlKey).append(" = ");
    appendValue(line, value, path);
    writer.append(line);
    writer.append('\n');
  }

  private static void writeComments(PrintWriter writer, List<String> comments) {
    for (String comment : comments) {
      for (String part : LINE_BREAK.split(comment, -1)) {
        writer.append(part.isEmpty() ? "#" : "# " + part).append('\n');
      }
    }
  }

  // <editor-fold desc="Values">

  /**
   * Appends {@code value} in TOML syntax.
   *
   * @param path the option the value belongs to, only used in error messages
   */
  private static void appendValue(StringBuilder out, @Nullable Object value, String path) {
    switch (value) {
      case null -> throw noNull(path);
      case DataObject data ->
          appendValue(out, data.isSingleValue() ? data.getAsObject() : data.getAsMap(), path);
      case MultilineString multiline -> appendMultilineString(out, multiline.getString());
      case String string -> appendString(out, string);
      case Character character -> appendString(out, String.valueOf(character.charValue()));
      case Boolean bool -> out.append(bool.booleanValue());
      case Integer number -> out.append(number.intValue());
      case Long number -> out.append(number.longValue());
      case Short number -> out.append(number.intValue());
      case Byte number -> out.append(number.intValue());
      case Double number -> appendDouble(out, number);
      case Float number -> appendFloat(out, number);
      // TOML floats only need an integer part followed by a fraction or exponent, which is what
      // BigDecimal#toString writes; a scale of 0 is written as an integer
      case BigDecimal number -> out.append(number);
      case BigInteger number -> {
        if (number.bitLength() < Long.SIZE) {
          out.append(number.longValue());
        } else {
          appendString(out, number.toString());
        }
      }
      case OffsetDateTime dateTime -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.formatTo(dateTime, out);
      case ZonedDateTime dateTime -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.formatTo(dateTime, out);
      case Instant instant -> DateTimeFormatter.ISO_INSTANT.formatTo(instant, out);
      case LocalDateTime dateTime -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.formatTo(dateTime, out);
      case LocalDate date -> DateTimeFormatter.ISO_LOCAL_DATE.formatTo(date, out);
      case LocalTime time -> DateTimeFormatter.ISO_LOCAL_TIME.formatTo(time, out);
      case Map<?, ?> map -> appendInlineTable(out, map, path);
      case Collection<?> collection -> appendCollection(out, collection, path);
      default -> appendArray(out, value, path);
    }
  }

  private static void appendInlineTable(StringBuilder out, Map<?, ?> table, String path) {
    out.append('{');
    boolean first = true;
    for (Map.Entry<?, ?> entry : table.entrySet()) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      String key = String.valueOf(entry.getKey());
      Object value = entry.getValue();
      if (value == null) {
        // the path is only built for the error
        throw noNull(path + '.' + key);
      }
      appendKey(out, key);
      out.append(" = ");
      appendValue(out, value, path);
    }
    out.append('}');
  }

  private static void appendCollection(StringBuilder out, Collection<?> elements, String path) {
    out.append('[');
    boolean first = true;
    for (Object element : elements) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      appendValue(out, element, path);
    }
    out.append(']');
  }

  private static void appendArray(StringBuilder out, Object array, String path) {
    // the common primitive arrays are written without boxing every element
    switch (array) {
      case int[] numbers -> {
        out.append('[');
        for (int i = 0; i < numbers.length; i++) {
          if (i > 0) {
            out.append(", ");
          }
          out.append(numbers[i]);
        }
        out.append(']');
      }
      case long[] numbers -> {
        out.append('[');
        for (int i = 0; i < numbers.length; i++) {
          if (i > 0) {
            out.append(", ");
          }
          out.append(numbers[i]);
        }
        out.append(']');
      }
      case double[] numbers -> {
        out.append('[');
        for (int i = 0; i < numbers.length; i++) {
          if (i > 0) {
            out.append(", ");
          }
          appendDouble(out, numbers[i]);
        }
        out.append(']');
      }
      default -> {
        if (!array.getClass().isArray()) {
          throw new IllegalArgumentException(
              "'"
                  + path
                  + "': cannot write a value of type "
                  + array.getClass().getName()
                  + " as TOML");
        }
        out.append('[');
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
          if (i > 0) {
            out.append(", ");
          }
          appendValue(out, Array.get(array, i), path);
        }
        out.append(']');
      }
    }
  }


  private static void appendDouble(StringBuilder out, double value) {
    if (Double.isNaN(value)) {
      out.append("nan");
    } else if (Double.isInfinite(value)) {
      out.append(value > 0 ? "inf" : "-inf");
    } else {
      out.append(value);
    }
  }

  private static void appendFloat(StringBuilder out, float value) {
    if (Float.isNaN(value)) {
      out.append("nan");
    } else if (Float.isInfinite(value)) {
      out.append(value > 0 ? "inf" : "-inf");
    } else {
      out.append(value);
    }
  }

  private static IllegalArgumentException noNull(String path) {
    return new IllegalArgumentException("'" + path + "': TOML has no null value");
  }

  // </editor-fold>

  // <editor-fold desc="Keys and strings">

  /** The key as written in TOML: bare when it can be, quoted otherwise. */
  private static String formatKey(String key) {
    if (isBareKey(key)) {
      return key;
    }
    StringBuilder out = new StringBuilder(key.length() + 2);
    appendString(out, key);
    return out.toString();
  }

  private static void appendKey(StringBuilder out, String key) {
    if (isBareKey(key)) {
      out.append(key);
    } else {
      appendString(out, key);
    }
  }

  /** Bare keys may only contain A-Z, a-z, 0-9, '_' and '-'. */
  private static boolean isBareKey(String key) {
    if (key.isEmpty()) {
      return false;
    }
    for (int i = 0; i < key.length(); i++) {
      char c = key.charAt(i);
      if (!((c >= 'a' && c <= 'z')
          || (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || c == '_'
          || c == '-')) {
        return false;
      }
    }
    return true;
  }

  /** A literal string when nothing needs escaping, a basic string otherwise. */
  private static void appendString(StringBuilder out, String value) {
    boolean literal = true;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '\'' || isControl(c, true)) {
        literal = false;
        break;
      }
    }
    if (literal) {
      out.append('\'').append(value).append('\'');
      return;
    }
    out.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        default -> appendChar(out, c);
      }
    }
    out.append('"');
  }

  /**
   * A multi-line basic string. The line break after the opening delimiter is not part of the
   * value, and line breaks are written as {@code \n} so the value reads back the same on every
   * platform.
   */
  private static void appendMultilineString(StringBuilder out, String value) {
    out.append("\"\"\"\n");
    int quotes = 0;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c == '"') {
        // at most two quotes in a row, a third would close the string
        quotes++;
        if (quotes == 3) {
          out.append("\\\"");
          quotes = 0;
        } else {
          out.append('"');
        }
        continue;
      }
      quotes = 0;
      switch (c) {
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append('\n');
        default -> appendChar(out, c);
      }
    }
    out.append("\"\"\"");
  }

  private static void appendChar(StringBuilder out, char c) {
    switch (c) {
      case '\b' -> out.append("\\b");
      case '\t' -> out.append("\\t");
      case '\f' -> out.append("\\f");
      case '\r' -> out.append("\\r");
      default -> {
        if (isControl(c, false)) {
          out.append("\\u00").append(HEX[(c >> 4) & 0xF]).append(HEX[c & 0xF]);
        } else {
          out.append(c);
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

  // </editor-fold>
}
