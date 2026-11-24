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
import com.mrivanplays.annotationconfig.core.internal.LinesHelper;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.utils.MapUtils;
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
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents the default toml value writer. It writes TOML 1.0:
 *
 * <ul>
 *   <li>Every top-level option is separated by an empty line, and its comments are written above
 *       it. Config objects and other objects are written as tables ({@code [server]}), with the
 *       comments of the object above the header and one line per value below it, each with its own
 *       comments. Objects inside them become sub-tables ({@code [server.limits]}). A table with
 *       neither values nor comments of its own gets no header: {@code [a.b]} defines {@code a}
 *       already.
 *   <li>TOML ends a table only where the next one starts, so options that are not tables are
 *       written first, and the tables after them, both in declaration order. Empty objects are
 *       written as {@code {}}, in place.
 *   <li>Strings are written as literal strings ({@code 'text'}) when they contain nothing that
 *       needs escaping, as basic strings ({@code "te\"xt"}) otherwise, and {@code @Multiline}
 *       strings as multi-line basic strings ({@code """}).
 *   <li>Floats use {@code inf} and {@code nan}. Integers that do not fit in 64 bits, which TOML
 *       does not allow, are written as strings.
 *   <li>Date and time values ({@link OffsetDateTime}, {@link LocalDateTime}, {@link LocalDate},
 *       {@link LocalTime}) are written as TOML date-times, and so are {@link ZonedDateTime} (with
 *       its offset, without the zone ID) and {@link Instant} (in UTC).
 *   <li>Arrays and lists are written as arrays, objects inside them as inline tables.
 * </ul>
 *
 * <p>Lines end with '\n' and never with {@code println()}: the line separator of the OS would mix
 * with the '\n' of the class comment.
 *
 * @author claude, Ivan Pekov
 * @since 2.1.0
 */
@NullMarked
public final class TomlValueWriter implements ValueWriter {

  private static final char[] HEX = "0123456789ABCDEF".toCharArray();

  /** Creates a new value writer. */
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

  /**
   * Writes one table: its header, the values that are not tables, then each table inside it.
   *
   * <p>The header is left out when the table has neither values nor comments of its own, as {@code
   * [a.b]} defines {@code a} already; an {@code [a]} above it would be empty.
   *
   * @param path the dotted key of the table as the comments are stored under, also used in error
   *     messages; {@code null} for the top level, which has no header
   * @param tomlKey the same key as written in the header, quoted where TOML requires it
   * @param written whether anything was written before, so an empty line has to come first
   * @return whether anything has been written now
   */
  private static boolean writeTable(
      StringBuilder out,
      @Nullable String path,
      @Nullable String tomlKey,
      Map<?, ?> values,
      Comments allComments,
      boolean written) {
    boolean top = path == null;
    // comments are looked up by the exact dotted path, so "port" never gets the comments of
    // "portal" and a value inside a table gets its own comments
    List<String> comments = top ? null : allComments.get(path);
    // only a table with commented values builds the paths of its values to look them up
    boolean valueComments = top || allComments.sections().contains(path);
    // the header is written before the first value, or for the comments if there is no value
    boolean header = top;
    // the tables are collected so the values are only unwrapped once
    List<Map.Entry<String, Map<?, ?>>> tables = null;
    for (Map.Entry<?, ?> entry : values.entrySet()) {
      String key = String.valueOf(entry.getKey());
      Object value = unwrap(entry.getValue());
      if (value instanceof Map<?, ?> map && !map.isEmpty()) {
        if (tables == null) {
          tables = new ArrayList<>();
        }
        tables.add(Map.entry(key, map));
        continue;
      }
      if (!header) {
        //noinspection DataFlowIssue: only the top level has no key, and it has no header
        writeHeader(out, tomlKey, comments, written);
        header = true;
      } else if (top && written) {
        // top-level options are separated by an empty line, the values of a table are not
        out.append('\n');
      }
      written = true;
      if (valueComments) {
        List<String> lines = allComments.get(top ? key : path + '.' + key);
        if (lines != null) {
          writeComments(out, lines);
        }
      }
      appendKey(out, key);
      out.append(" = ");
      appendValue(out, value, path, key);
      out.append('\n');
    }
    if (!header && comments != null) {
      //noinspection DataFlowIssue: only the top level has no key, and it has no comments
      writeHeader(out, tomlKey, comments, written);
      written = true;
    }
    if (tables == null) {
      return written;
    }
    for (Map.Entry<String, Map<?, ?>> table : tables) {
      String key = table.getKey();
      written =
          writeTable(
              out,
              top ? key : path + '.' + key,
              top ? formatKey(key) : tomlKey + '.' + formatKey(key),
              table.getValue(),
              allComments,
              written);
    }
    return written;
  }

  private static void writeHeader(
      StringBuilder out, String tomlKey, @Nullable List<String> comments, boolean written) {
    if (written) {
      out.append('\n');
    }
    if (comments != null) {
      writeComments(out, comments);
    }
    out.append('[').append(tomlKey).append("]\n");
  }

  private static @Nullable Object unwrap(@Nullable Object value) {
    if (value instanceof DataObject data) {
      return data.isSingleValue() ? data.getAsObject() : data.getAsMap();
    }
    return value;
  }

  private static void writeComments(StringBuilder out, List<String> comments) {
    for (String comment : comments) {
      for (String line : LinesHelper.split(comment)) {
        out.append(line.isEmpty() ? "#" : "# ").append(line).append('\n');
      }
    }
  }

  /** The dotted path of {@code key} in {@code table} ({@code null} at the top level). */
  private static String path(@Nullable String table, String key) {
    return table == null ? key : table + '.' + key;
  }

  // <editor-fold desc="Values">

  /**
   * Appends {@code value} in TOML syntax.
   *
   * @param table the table of the option the value belongs to ({@code null} at the top level) and
   * @param key its key: only used in error messages, so the dotted path is only built for one
   */
  private static void appendValue(
      StringBuilder out, @Nullable Object value, @Nullable String table, String key) {
    switch (value) {
      case null -> throw noNull(path(table, key));
      case DataObject data -> appendValue(out, unwrap(data), table, key);
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
      case OffsetDateTime dateTime ->
          DateTimeFormatter.ISO_OFFSET_DATE_TIME.formatTo(dateTime, out);
      case ZonedDateTime dateTime -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.formatTo(dateTime, out);
      case Instant instant -> DateTimeFormatter.ISO_INSTANT.formatTo(instant, out);
      case LocalDateTime dateTime -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.formatTo(dateTime, out);
      case LocalDate date -> DateTimeFormatter.ISO_LOCAL_DATE.formatTo(date, out);
      case LocalTime time -> DateTimeFormatter.ISO_LOCAL_TIME.formatTo(time, out);
      case Map<?, ?> map -> appendInlineTable(out, map, table, key);
      case Collection<?> collection -> appendCollection(out, collection, table, key);
      default -> appendArray(out, value, table, key);
    }
  }

  private static void appendInlineTable(
      StringBuilder out, Map<?, ?> inline, @Nullable String table, String key) {
    out.append('{');
    boolean first = true;
    for (Map.Entry<?, ?> entry : inline.entrySet()) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      String entryKey = String.valueOf(entry.getKey());
      Object value = entry.getValue();
      // the path is only built for an error
      if (value == null) {
        throw noNull(path(table, key) + '.' + entryKey);
      }
      appendKey(out, entryKey);
      out.append(" = ");
      // errors inside name the entry, not only the inline table
      appendValue(out, value, path(table, key), entryKey);
    }
    out.append('}');
  }

  private static void appendCollection(
      StringBuilder out, Collection<?> elements, @Nullable String table, String key) {
    out.append('[');
    boolean first = true;
    for (Object element : elements) {
      if (!first) {
        out.append(", ");
      }
      first = false;
      appendValue(out, element, table, key);
    }
    out.append(']');
  }

  private static void appendArray(
      StringBuilder out, Object array, @Nullable String table, String key) {
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
                  + path(table, key)
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
          appendValue(out, Array.get(array, i), table, key);
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
   * A multi-line basic string. The line break after the opening delimiter is not part of the value.
   * Line breaks are written as a line feed ('\n') and a carriage return as the {@code \r} escape,
   * so the value reads back the same on every platform.
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

  @Override
  public void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      Settings settings) {
    // The file is built first and written once, as the other writers do: many small writes through
    // the PrintWriter each take its lock. A value that can't be written fails before anything is.
    StringBuilder out = new StringBuilder(256);
    writeTable(out, null, null, values, new Comments(fieldComments), false);
    writer.append(out);
  }

  /**
   * The comments to write.
   *
   * @param byPath the comments of each option, under its dotted path
   * @param sections the dotted paths of the tables that hold a commented option, at any depth:
   *     {@code a} and {@code a.b} for {@code a.b.c}. In other tables the values' paths aren't
   *     built, as nothing is looked up under them.
   */
  private record Comments(Map<String, List<String>> byPath, Set<String> sections) {

    Comments(Map<String, List<String>> byPath) {
      // options without comments have an empty list, which MapUtils#parentPaths skips
      this(byPath, MapUtils.parentPaths(byPath));
    }

    /** The comments of {@code path}, or {@code null}: options without comments have none. */
    @Nullable List<String> get(String path) {
      if (this.byPath.isEmpty()) {
        return null;
      }
      List<String> comments = this.byPath.get(path);
      return comments == null || comments.isEmpty() ? null : comments;
    }
  }

}
