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

package com.mrivanplays.annotationconfig.core.resolver.key;

import java.util.Map;

/**
 * Represents a resolver of config keys, which boxes and unboxes them.
 *
 * @author Ivan Pekov
 * @since 2.0.1
 */
public interface KeyResolver {

  /** Returns the default key resolver used by AnnotationConfig. */
  KeyResolver DEFAULT =
      new KeyResolver() {
        @Override
        public Object unbox(String key, Map<String, Object> values) {
          return values.get(key);
        }

        @Override
        public void boxTo(String key, Object value, Map<String, Object> values) {
          values.put(key, value);
        }
      };

  /**
   * AnnotationConfig calls this method whenever it needs the value of the specified {@code key}.
   *
   * <p>Keep in mind just because modification of the inputted {@link Map} {@code values} won't bug
   * you it does not mean it goes without consequences. It is strongly not recommended as it may
   * alter the set values.
   *
   * @param key the key of the value
   * @param values the values to unbox it from
   * @return value or null
   */
  Object unbox(String key, Map<String, Object> values);

  /**
   * AnnotationConfig calls this method whenever it needs to populate the specified {@link Map}
   * {@code values} with the specified {@code value} and specified {@code key}.
   *
   * @param key the key of the value
   * @param value the value to box
   * @param values the place where the value shall be put
   */
  void boxTo(String key, Object value, Map<String, Object> values);
}
