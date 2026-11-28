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

package com.mrivanplays.annotationconfig.core.utils;

import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * Splits text, such as a comment, into its lines. Useful for a {@link
 * com.mrivanplays.annotationconfig.core.resolver.ValueWriter} that writes a comment marker in front
 * of every line of a comment, as the YAML, JSON5 and TOML writers do.
 *
 * <p>A line break is anything the regex {@code \R} matches: {@code \r\n}, {@code \n}, {@code \r},
 * vertical tab, form feed, next line ({@code U+0085}) and the line and paragraph separators ({@code
 * U+2028}, {@code U+2029}). A writer for a format that knows fewer line breaks, such as {@code
 * .properties}, which only knows {@code \r} and {@code \n}, splits on its own.
 *
 * @author Ivan Pekov
 * @since 3.1.0
 */
@NullMarked
public final class LinesHelper {

  private static final Pattern LINE_BREAK = Pattern.compile("\\R");

  private LinesHelper() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * Splits {@code text} into its lines, at every line break (see {@link LinesHelper}). The line
   * breaks are not part of the lines, and {@code \r\n} is one line break.
   *
   * <p>Empty lines are kept, also at the end: {@code "a\n\nb\n"} gives {@code ["a", "", "b", ""]}.
   * Text without a line break, as most comments are, gives a one element array of {@code text}
   * itself, without running a regex. An empty {@code text} gives {@code [""]}.
   *
   * @param text the text to split
   * @return the lines of {@code text}, in a new array that the caller may modify
   */
  public static String[] split(String text) {
    return hasLineBreak(text) ? LINE_BREAK.split(text, -1) : new String[] {text};
  }

  /** Whether {@code text} has a line break as {@code \R} finds one. */
  private static boolean hasLineBreak(String text) {
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      // line feed, vertical tab, form feed, carriage return, next line (NEL) and the Unicode
      // line and paragraph separators
      if ((c >= '\n' && c <= '\r') || c == 0x85 || c == 0x2028 || c == 0x2029) {
        return true;
      }
    }
    return false;
  }
}
