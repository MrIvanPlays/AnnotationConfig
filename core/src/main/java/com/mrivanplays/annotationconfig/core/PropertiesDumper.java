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

import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Writes an ordered map as a .properties file. Per-key comments are kept, and keys and values are
 * escaped so that {@link java.util.Properties#load} reads back exactly what was written.
 *
 * @author claude
 * @hidden
 */
final class PropertiesDumper {

  private static final String EOL = System.lineSeparator();

  private PropertiesDumper() {}

  /**
   * @param values entries are written in iteration order, so pass a LinkedHashMap to keep order
   * @param fieldComments comment lines per key; may be null. Comments that contain line breaks are
   *     split so that every line gets its own "#" prefix
   * @param escapeUnicode true: non-ASCII characters are written as unicode escapes, which is safe
   *     for Properties.load(InputStream) (ISO-8859-1). false: written as-is, so the file must be
   *     read back with a Reader using the same charset as the writer (e.g. UTF-8)
   */
  public static void dump(
      PrintWriter writer,
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      boolean escapeUnicode) {
    boolean first = true;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      String key = entry.getKey();
      // Render first, so an unsupported value fails before anything is written for this entry.
      String renderedValue = render(key, entry.getValue(), escapeUnicode);

      if (!first) {
        writer.println();
      }
      first = false;

      List<String> comments = fieldComments == null ? null : fieldComments.get(key);
      if (comments != null) {
        for (String comment : comments) {
          writeComment(writer, comment);
        }
      }
      writer.println(escape(key, true, false, escapeUnicode) + "=" + renderedValue);
    }
    writer.flush();
  }

  private static void writeComment(PrintWriter writer, String comment) {
    for (String line : comment.split("\r\n|\r|\n", -1)) {
      writer.println(line.isEmpty() ? "#" : "# " + line);
    }
  }

  private static String render(String key, Object value, boolean escapeUnicode) {
    switch (value) {
      case null -> {
        return ""; // .properties has no null; an empty value is the closest thing
      }
      case Map<?, ?> map ->
          throw new IllegalArgumentException("'" + key + "': .properties does not support maps");
      case List<?> objects ->
          throw new IllegalArgumentException("'" + key + "': .properties does not support lists");
      case MultilineString multilineString -> {
        return escape(multilineString.getString(), false, true, escapeUnicode);
      }
      default -> {}
    }
    if (value.getClass().isArray()) {
      return escape(arrayToString(key, value), false, false, escapeUnicode);
    }
    return escape(String.valueOf(value), false, false, escapeUnicode);
  }

  /** Works for primitive and object arrays. Elements are joined with ", " (not escaped). */
  private static String arrayToString(String key, Object array) {
    int length = Array.getLength(array);
    StringJoiner joiner = new StringJoiner(", ");
    for (int i = 0; i < length; i++) {
      Object element = Array.get(array, i);
      if (element != null
          && (element.getClass().isArray()
              || element instanceof Map<?, ?>
              || element instanceof Collection<?>)) {
        throw new IllegalArgumentException(
            "'" + key + "': .properties does not support nested arrays, maps or lists");
      }
      joiner.add(String.valueOf(element));
    }
    return joiner.toString();
  }

  /**
   * Escapes text for the key or value side of a .properties line.
   *
   * @param isKey keys also escape spaces and the separator/comment characters '=', ':', '#', '!'
   * @param multiline write line breaks as "\n" plus a continuation line instead of one long line
   */
  private static String escape(String s, boolean isKey, boolean multiline, boolean escapeUnicode) {
    StringBuilder sb = new StringBuilder(s.length() + 16);
    // True while the next output char is the first one on a (continuation) line. Whitespace there
    // would be stripped on load, so it has to be escaped.
    boolean atLineStart = true;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\':
          sb.append("\\\\");
          atLineStart = false;
          break;
        case '\n':
          if (multiline) {
            sb.append("\\n\\").append(EOL);
            atLineStart = true;
          } else {
            sb.append("\\n");
            atLineStart = false;
          }
          break;
        case '\r':
          sb.append("\\r");
          atLineStart = false;
          break;
        case '\t':
          sb.append("\\t");
          atLineStart = false;
          break;
        case '\f':
          sb.append("\\f");
          atLineStart = false;
          break;
        case ' ':
          if (isKey || atLineStart) {
            sb.append("\\ ");
          } else {
            sb.append(' ');
          }
          break; // leading spaces keep atLineStart true so all of them get escaped
        case '=':
        case ':':
        case '#':
        case '!':
          if (isKey) {
            sb.append('\\');
          }
          sb.append(c);
          atLineStart = false;
          break;
        default:
          if (c < 0x20 || c == 0x7f || (escapeUnicode && c > 0x7e)) {
            sb.append(String.format("\\u%04X", (int) c));
          } else {
            sb.append(c);
          }
          atLineStart = false;
      }
    }
    return sb.toString();
  }
}
