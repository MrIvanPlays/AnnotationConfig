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

import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a value writer.
 *
 * <p>A value writer is a writer of values for a specified configuration type.
 *
 * @since 2.0.0 but heavily modified in 2.1.0
 * @author Ivan Pekov
 */
@FunctionalInterface
@NullMarked
public interface ValueWriter {

  /**
   * AnnotationConfig calls this method in order to write the specified {@code values} with the
   * specified {@code fieldComments} to the specified {@link PrintWriter} {@code writer}. The
   * written values or the method of writing could be altered via the provided {@link Settings}
   * {@code settings}.
   *
   * @param values the values needed to be written
   * @param fieldComments the comments of the options, under the dotted path of each option
   * @param writer the writer to write
   * @param settings settings
   * @throws IOException if an I/O error occurs
   */
  void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      Settings settings)
      throws IOException;

  /**
   * Returns whether this writer writes the comments of options. When it doesn't, AnnotationConfig
   * doesn't collect them and {@link #write(Map, Map, PrintWriter, Settings) write} gets an empty
   * {@code fieldComments} map. A format without comments, such as JSON, saves the work.
   *
   * @return whether {@code fieldComments} is used; {@code true} unless overridden
   * @since 3.1.0
   */
  default boolean writesComments() {
    return true;
  }
}
