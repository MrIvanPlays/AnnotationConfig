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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a {@link Setting} holder.
 *
 * @since 3.0.0
 * @author Ivan Pekov
 */
public final class Settings {

  private Map<String, Object> values = new ConcurrentHashMap<>();
  private final boolean immutable;

  public Settings() {
    this.immutable = false;
  }

  private Settings(boolean immutable, Map<String, Object> values) {
    this.immutable = immutable;
    this.values = values;
  }

  /**
   * Creates a mutable copy of the current settings object.
   *
   * @return copy
   */
  public Settings copy() {
    return new Settings(false, this.values);
  }

  /**
   * Creates a copy of the current settings object.
   *
   * @param immutable whether the copy to be immutable
   * @return copy
   */
  public Settings copy(boolean immutable) {
    return new Settings(immutable, this.values);
  }

  /**
   * Checks whether this settings instance stores a value for this setting
   *
   * @param setting setting
   * @return whether it has value or not
   */
  public boolean has(Setting<?> setting) {
    return this.values.containsKey(setting.key());
  }

  /**
   * Returns whether this settings instance is immutable.
   *
   * @return immutable or not
   */
  public boolean immutable() {
    return this.immutable;
  }

  /**
   * Puts the specified {@link Setting} {@code setting} {@code value} into this settings instance,
   * or if it already exists it replaces it.
   *
   * @param setting setting to modify
   * @param value value to put/replace
   * @return this settings instance for chaining
   * @throws IllegalArgumentException when the setting key is occupied by another value data type
   * @param <T> value type
   */
  public <T> Settings put(Setting<T> setting, T value) {
    if (immutable) {
      throw new UnsupportedOperationException("This settings instance is immutable");
    }
    if (this.values.containsKey(setting.key())) {
      Object val = this.values.get(setting.key());
      if (!setting.type().isAssignableFrom(val.getClass())) {
        throw new IllegalArgumentException(
            "key matches for 2 different data types: key '"
                + setting.key()
                + "' cannot store both "
                + val.getClass().getName()
                + "' and '"
                + setting.type().getName()
                + "' value types");
      }
      this.values.replace(setting.key(), value);
    } else {
      this.values.put(setting.key(), value);
    }
    return this;
  }

  /**
   * Combines the specified {@code Settings} with this {@code Settings} instance. If a setting, that
   * is present in the specified settings, is also present in this settings instance, then the value
   * of this settings instance is respected.
   *
   * @param settings settings to combine with
   * @return this instance for chaining
   */
  public Settings combine(Settings settings) {
    if (immutable) {
      throw new UnsupportedOperationException("This settings instance is immutable");
    }
    if (!settings.values.isEmpty()) {
      for (Map.Entry<String, Object> entry : settings.values.entrySet()) {
        if (!this.values.containsKey(entry.getKey())) {
          this.values.put(entry.getKey(), entry.getValue());
        }
      }
    }
    return this;
  }

  /**
   * Get the stored value in these settings of the specified {@link Setting}, if any.
   *
   * @param setting the setting needed to get
   * @return empty optional or optional fulfilled with value
   * @throws IllegalArgumentException if the submitted setting's type mismatches the setting type
   *     held.
   * @param <T> value type
   */
  public <T> Optional<T> get(Setting<T> setting) {
    if (!this.values.containsKey(setting.key())) {
      return Optional.empty();
    }
    Object val = this.values.get(setting.key());
    if (!setting.type().isAssignableFrom(val.getClass())) {
      throw new IllegalArgumentException(
          "tried to access '"
              + setting.key()
              + "' with value type '"
              + setting.type().getName()
              + "' while key holds '"
              + val.getClass().getName()
              + "'");
    }
    return Optional.of(setting.type().cast(val));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Settings settings = (Settings) o;

    return values.equals(settings.values);
  }

  @Override
  public int hashCode() {
    return values.hashCode();
  }
}
