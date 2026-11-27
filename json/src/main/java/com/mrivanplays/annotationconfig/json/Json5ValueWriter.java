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

package com.mrivanplays.annotationconfig.json;

import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.utils.LinesHelper;
import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Writes values as JSON5 (<a href="https://spec.json5.org">spec.json5.org</a>), which unlike JSON
 * supports comments.
 *
 * <p>How values are written:
 *
 * <ul>
 *   <li>The comments of an option are written as {@code //} line comments above its key, also for
 *       options inside config objects. Comments of keys inside arrays are not written.
 *   <li>Keys that are identifiers ({@code [A-Za-z_$][A-Za-z0-9_$]*}) are written unquoted, any
 *       other key in double quotes.
 *   <li>Strings are double-quoted. {@code @Multiline} strings are written on one line with {@code
 *       \n} escapes, not split with JSON5's line continuations (a backslash before a line break):
 *       JSON5 drops the line break, but Gson keeps it in the value, so such a string would not read
 *       back the same.
 *   <li>Infinity and NaN are written as {@code Infinity}, {@code -Infinity} and {@code NaN}.
 *   <li>The layout is the one of pretty printed JSON (two spaces per level, one array element per
 *       line), without trailing commas. A {@code null} entry of an object is left out, a {@code
 *       null} array element is written as {@code null}.
 * </ul>
 *
 * <p>The output uses only those JSON5 extensions of JSON, so a lenient JSON reader such as Gson's
 * reads it.
 *
 * @author claude, Ivan Pekov
 * @since 3.1.0
 */
@NullMarked
public final class Json5ValueWriter implements ValueWriter {

  private static final char[] HEX = "0123456789ABCDEF".toCharArray();
  private static final String INDENT = "  ";

  private static void appendObject(
      StringBuilder out,
      Map<?, ?> object,
      int depth,
      @Nullable String objectPath,
      Comments comments) {
    // null when nothing below is commented
    String path =
        objectPath != null && !objectPath.isEmpty() && !comments.sections().contains(objectPath)
            ? null
            : objectPath;
    boolean first = true;
    for (Map.Entry<?, ?> entry : object.entrySet()) {
      Object value = unwrap(entry.getValue());
      if (value == null) {
        // as Gson does for JSON: a null entry is left out
        continue;
      }
      out.append(first ? "{\n" : ",\n");
      first = false;
      String key = String.valueOf(entry.getKey());
      String childPath = path == null ? null : path.isEmpty() ? key : path + '.' + key;
      if (childPath != null) {
        List<String> lines = comments.byPath().get(childPath);
        if (lines != null) {
          appendComments(out, lines, depth + 1);
        }
      }
      indent(out, depth + 1);
      appendKey(out, key);
      out.append(": ");
      appendValue(out, value, depth + 1, childPath, comments);
    }
    if (first) {
      out.append("{}");
      return;
    }
    out.append('\n');
    indent(out, depth);
    out.append('}');
  }

  private static void appendArray(
      StringBuilder out, Collection<?> elements, int depth, Comments comments) {
    if (elements.isEmpty()) {
      out.append("[]");
      return;
    }
    out.append("[\n");
    boolean first = true;
    for (Object element : elements) {
      if (!first) {
        out.append(",\n");
      }
      first = false;
      indent(out, depth + 1);
      // no comments inside arrays
      appendValue(out, unwrap(element), depth + 1, null, comments);
    }
    out.append('\n');
    indent(out, depth);
    out.append(']');
  }

  private static void appendValue(
      StringBuilder out,
      @Nullable Object value,
      int depth,
      @Nullable String path,
      Comments comments) {
    switch (value) {
      case null -> out.append("null");
      case MultilineString multiline -> appendString(out, multiline.getString());
      case String string -> appendString(out, string);
      case Character character -> appendString(out, String.valueOf(character.charValue()));
      case Boolean bool -> out.append(bool.booleanValue());
      case Double number -> appendDouble(out, number);
      // not widened to double, which would write 3.11f as 3.109999895095825
      case Float number -> appendFloat(out, number);
      // integers and BigDecimal write themselves as JSON5 numbers
      case Number number -> out.append(number);
      case Map<?, ?> map -> appendObject(out, map, depth, path, comments);
      case Collection<?> collection -> appendArray(out, collection, depth, comments);
      default -> {
        if (value.getClass().isArray()) {
          int length = Array.getLength(value);
          List<Object> elements = new ArrayList<>(length);
          for (int i = 0; i < length; i++) {
            elements.add(Array.get(value, i));
          }
          appendArray(out, elements, depth, comments);
        } else {
          // enums arrive as their name, anything else is written as its text
          appendString(out, value.toString());
        }
      }
    }
  }

  private static @Nullable Object unwrap(@Nullable Object value) {
    if (value instanceof DataObject data) {
      return data.isSingleValue() ? data.getAsObject() : data.getAsMap();
    }
    return value;
  }

  private static void appendComments(StringBuilder out, List<String> comments, int depth) {
    for (String comment : comments) {
      for (String line : LinesHelper.split(comment)) {
        indent(out, depth);
        out.append(line.isEmpty() ? "//" : "// ").append(line).append('\n');
      }
    }
  }

  private static void appendDouble(StringBuilder out, double value) {
    if (Double.isNaN(value)) {
      out.append("NaN");
    } else if (Double.isInfinite(value)) {
      out.append(value > 0 ? "Infinity" : "-Infinity");
    } else {
      out.append(value);
    }
  }

  private static void appendFloat(StringBuilder out, float value) {
    if (Float.isNaN(value)) {
      out.append("NaN");
    } else if (Float.isInfinite(value)) {
      out.append(value > 0 ? "Infinity" : "-Infinity");
    } else {
      out.append(value);
    }
  }

  private static void appendKey(StringBuilder out, String key) {
    if (isIdentifier(key)) {
      out.append(key);
    } else {
      appendString(out, key);
    }
  }

  /** ECMAScript identifier names, limited to ASCII. */
  private static boolean isIdentifier(String key) {
    if (key.isEmpty()) {
      return false;
    }
    for (int i = 0; i < key.length(); i++) {
      char c = key.charAt(i);
      boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
      if (!letter && !(i > 0 && c >= '0' && c <= '9')) {
        return false;
      }
    }
    return true;
  }

  private static void appendString(StringBuilder out, String value) {
    out.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        case '\b' -> out.append("\\b");
        case '\f' -> out.append("\\f");
        default -> {
          // line and paragraph separators end a line in JavaScript, so they are escaped too
          if (c < 0x20 || c == 0x7f || c == 0x2028 || c == 0x2029) {
            out.append("\\u")
                .append(HEX[(c >> 12) & 0xF])
                .append(HEX[(c >> 8) & 0xF])
                .append(HEX[(c >> 4) & 0xF])
                .append(HEX[c & 0xF]);
          } else {
            out.append(c);
          }
        }
      }
    }
    out.append('"');
  }

  private static void indent(StringBuilder out, int depth) {
    out.repeat(INDENT, depth);
  }

  /** As in JSON: a {@code "$schema"} key. */
  @Override
  public boolean writesSchemaKey() {
    return true;
  }

  @Override
  public void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      Settings settings) {
    StringBuilder out = new StringBuilder(256);
    Comments comments = new Comments(fieldComments);
    // without comments there is nothing to look up, so no dotted paths are built
    appendObject(out, values, 0, comments.byPath().isEmpty() ? null : "", comments);
    // a text file ends with a line break, as the files of the other formats do
    out.append('\n');
    writer.append(out);
  }

  /**
   * The comments to write.
   *
   * @param byPath the comments of each option, under its dotted path
   * @param sections the dotted paths of the objects that hold a commented option, at any depth:
   *     {@code a} and {@code a.b} for {@code a.b.c}. Inside other objects no paths are built.
   */
  private record Comments(Map<String, List<String>> byPath, Set<String> sections) {

    Comments(Map<String, List<String>> byPath) {
      // options without comments have an empty list, which MapUtils#parentPaths skips
      this(byPath, MapUtils.parentPaths(byPath));
    }
  }
}
