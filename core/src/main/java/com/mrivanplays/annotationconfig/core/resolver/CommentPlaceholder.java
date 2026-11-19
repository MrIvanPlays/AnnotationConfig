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

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a placeholder for comments.
 *
 * @author MrIvanPlays
 * @since 3.0.4
 */
public interface CommentPlaceholder extends Function<String, String> {

  /**
   * Create a simple comment placeholder.
   *
   * @param toReplace placeholder to replace
   * @param replacement replacement
   * @return comment placeholder instance
   */
  static CommentPlaceholder simple(String toReplace, String replacement) {
    Objects.requireNonNull(toReplace, "toReplace");
    Objects.requireNonNull(replacement, "replacement");
    return new CommentPlaceholder() {
      @Override
      public String replaces() {
        return toReplace;
      }

      @Override
      public String apply(String s) {
        return s.replace(toReplace, replacement);
      }
    };
  }

  /**
   * Shall get what this {@code CommentPlaceholder} instance replaces (e.g. the placeholder).
   *
   * @return replaces what
   */
  String replaces();

  /**
   * Check whether the passed {@code CommentPlaceholder} instance matches/equals this instance. <br>
   * AnnotationConfig uses this method for unregistering placeholder objects in the {@link
   * CommentPlaceholderRegistry}. If you do not intent to unregister placeholders, or the default
   * implementation works for you, you may not override this method.
   *
   * @param other to check
   * @return true if matches, false otherwise
   */
  default boolean matches(CommentPlaceholder other) {
    return other.replaces().equals(this.replaces());
  }
}
