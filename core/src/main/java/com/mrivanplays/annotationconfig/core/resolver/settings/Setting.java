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

package com.mrivanplays.annotationconfig.core.resolver.settings;

import java.util.Objects;

/**
 * Represents a modifiable setting.
 *
 * @param <T> value type held
 * @since 3.0.0
 * @author MrIvanPlays
 */
public interface Setting<T> {

  /**
   * Create a new {@link Setting} instance.
   *
   * @param type class type representation of the setting
   * @return new setting instance
   * @param <T> value type held
   * @since 3.0.4
   */
  static <T> Setting<T> of(Class<T> type) {
    return of(null, type);
  }

  /**
   * Create a new {@link Setting} instance.
   *
   * @param key key with which this setting is stored
   * @param type class type representation of the setting
   * @return new setting instance
   * @param <T> value type held
   */
  static <T> Setting<T> of(String key, Class<T> type) {
    Objects.requireNonNull(type, "type");
    return new Setting<>() {
      @Override
      public String key() {
        return key == null ? type.getName() : key;
      }

      @Override
      public Class<T> type() {
        return type;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || getClass() != o.getClass()) {
          return false;
        }
        Setting setting = (Setting) o;
        if (!setting.key().equalsIgnoreCase(key)) {
          return false;
        }
        return type.isAssignableFrom(setting.type());
      }
    };
  }

  /**
   * Returns the key with which this setting will be associated with.
   *
   * @return setting key
   */
  String key();

  /**
   * Returns the {@link Class} type this setting shall hold.
   *
   * @return value type this setting holds
   */
  Class<T> type();

  /**
   * Compares 2 {@code Setting} objects to check whether they are equal.
   *
   * @param o setting object
   * @return whether equals
   */
  @Override
  boolean equals(Object o);
}
