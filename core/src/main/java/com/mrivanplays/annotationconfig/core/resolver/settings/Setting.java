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

  static <T> Setting<T> of(String key, Class<T> type) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(type, "type");
    return new Setting<T>() {
      @Override
      public String key() {
        return key;
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
