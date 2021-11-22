package com.mrivanplays.annotationconfig.core.resolver.key;

import java.util.Map;

/**
 * Represents a resolver of config keys, which boxes and unboxes them.
 *
 * @author MrIvanPlays
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
