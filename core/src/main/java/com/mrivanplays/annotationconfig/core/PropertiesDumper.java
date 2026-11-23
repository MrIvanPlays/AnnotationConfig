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
import java.util.regex.Pattern;

/**
 * Writes an ordered map as a .properties file. Per-key comments are kept, and keys and values are
 * escaped so that {@link java.util.Properties#load} reads back exactly what was written.
 *
 * @author claude
 * @hidden
 */
final class PropertiesDumper {

  // the same on every OS, as the other formats and the class comment are written
  private static final String EOL = "\n";
  private static final Pattern LINE_BREAK = Pattern.compile("\r\n|\r|\n");
  private static final char[] HEX = "0123456789ABCDEF".toCharArray();

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
    // The file is built first and written once, instead of several writes per entry through the
    // PrintWriter. An unsupported value therefore fails before anything is written.
    StringBuilder out = new StringBuilder(64 * values.size() + 16);
    boolean first = true;
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      if (!first) {
        out.append(EOL);
      }
      first = false;

      String key = entry.getKey();
      List<String> comments = fieldComments == null ? null : fieldComments.get(key);
      if (comments != null) {
        for (String comment : comments) {
          appendComment(out, comment);
        }
      }
      appendEscaped(out, key, true, false, escapeUnicode);
      out.append('=');
      appendValue(out, key, entry.getValue(), escapeUnicode);
      out.append(EOL);
    }
    writer.append(out);
    writer.flush();
  }

  private static void appendComment(StringBuilder out, String comment) {
    // nearly every comment is one line, which needs no regex and no array
    if (comment.indexOf('\n') < 0 && comment.indexOf('\r') < 0) {
      appendCommentLine(out, comment);
      return;
    }
    for (String line : LINE_BREAK.split(comment, -1)) {
      appendCommentLine(out, line);
    }
  }

  private static void appendCommentLine(StringBuilder out, String line) {
    out.append(line.isEmpty() ? "#" : "# ").append(line).append(EOL);
  }

  private static void appendValue(
      StringBuilder out, String key, Object value, boolean escapeUnicode) {
    switch (value) {
      case null -> {
        // .properties has no null; an empty value is the closest thing
      }
      case Map<?, ?> map ->
          throw new IllegalArgumentException("'" + key + "': .properties does not support maps");
      case List<?> objects ->
          throw new IllegalArgumentException("'" + key + "': .properties does not support lists");
      case MultilineString multilineString ->
          appendEscaped(out, multilineString.getString(), false, true, escapeUnicode);
      default -> {
        String text =
            value.getClass().isArray() ? arrayToString(key, value) : String.valueOf(value);
        appendEscaped(out, text, false, false, escapeUnicode);
      }
    }
  }

  /** Works for primitive and object arrays. Elements are joined with ", " (not escaped). */
  private static String arrayToString(String key, Object array) {
    int length = Array.getLength(array);
    StringBuilder joined = new StringBuilder(length * 8);
    for (int i = 0; i < length; i++) {
      Object element = Array.get(array, i);
      if (element != null
          && (element.getClass().isArray()
              || element instanceof Map<?, ?>
              || element instanceof Collection<?>)) {
        throw new IllegalArgumentException(
            "'" + key + "': .properties does not support nested arrays, maps or lists");
      }
      if (i > 0) {
        joined.append(", ");
      }
      joined.append(element);
    }
    return joined.toString();
  }

  /**
   * Appends text escaped for the key or value side of a .properties line.
   *
   * @param isKey keys also escape spaces and the separator/comment characters '=', ':', '#', '!'
   * @param multiline write line breaks as "\n" plus a continuation line instead of one long line
   */
  private static void appendEscaped(
      StringBuilder out, String s, boolean isKey, boolean multiline, boolean escapeUnicode) {
    // most keys and values need no escaping: append them as they are
    if (!needsEscaping(s, isKey, escapeUnicode)) {
      out.append(s);
      return;
    }
    // True while the next output char is the first one on a (continuation) line. Whitespace there
    // would be stripped on load, so it has to be escaped.
    boolean atLineStart = true;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\':
          out.append("\\\\");
          atLineStart = false;
          break;
        case '\n':
          if (multiline) {
            out.append("\\n\\").append(EOL);
            atLineStart = true;
          } else {
            out.append("\\n");
            atLineStart = false;
          }
          break;
        case '\r':
          out.append("\\r");
          atLineStart = false;
          break;
        case '\t':
          out.append("\\t");
          atLineStart = false;
          break;
        case '\f':
          out.append("\\f");
          atLineStart = false;
          break;
        case ' ':
          if (isKey || atLineStart) {
            out.append("\\ ");
          } else {
            out.append(' ');
          }
          break; // leading spaces keep atLineStart true so all of them get escaped
        case '=':
        case ':':
        case '#':
        case '!':
          if (isKey) {
            out.append('\\');
          }
          out.append(c);
          atLineStart = false;
          break;
        default:
          if (c < 0x20 || c == 0x7f || (escapeUnicode && c > 0x7e)) {
            out.append("\\u")
                .append(HEX[(c >> 12) & 0xF])
                .append(HEX[(c >> 8) & 0xF])
                .append(HEX[(c >> 4) & 0xF])
                .append(HEX[c & 0xF]);
          } else {
            out.append(c);
          }
          atLineStart = false;
      }
    }
  }

  /**
   * Whether {@link #appendEscaped} would change {@code s}: when this is false, every character is
   * written as it is.
   */
  private static boolean needsEscaping(String s, boolean isKey, boolean escapeUnicode) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\\' || c < 0x20 || c == 0x7f || (escapeUnicode && c > 0x7e)) {
        return true;
      }
      if (isKey) {
        if (c == ' ' || c == '=' || c == ':' || c == '#' || c == '!') {
          return true;
        }
      } else if (i == 0 && c == ' ') {
        // a value can only start a line at its first character: line breaks are escaped above
        return true;
      }
    }
    return false;
  }
}
