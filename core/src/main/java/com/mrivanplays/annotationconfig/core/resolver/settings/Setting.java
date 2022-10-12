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
}
