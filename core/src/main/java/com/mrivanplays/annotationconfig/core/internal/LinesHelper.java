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

package com.mrivanplays.annotationconfig.core.internal;

import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * Splits comments into lines, for the writers that write a comment marker on every line (YAML,
 * JSON5, TOML). {@code .properties} only treats {@code \r} and {@code \n} as line breaks, so its
 * writer splits on its own.
 *
 * @hidden
 */
@NullMarked
public final class LinesHelper {

  private static final Pattern LINE_BREAK = Pattern.compile("\\R");

  private LinesHelper() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * The lines of {@code text}, split at every line break {@code \R} matches. Nearly every comment
   * is one line, which is returned as it is, without a regex.
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
