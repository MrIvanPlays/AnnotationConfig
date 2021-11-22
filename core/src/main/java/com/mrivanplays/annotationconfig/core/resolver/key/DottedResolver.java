package com.mrivanplays.annotationconfig.core.resolver.key;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a resolver of dotted keys.
 *
 * <p>A dotted key is a key which is of type "a.b.c". This key resolver boxes it in a {@link Map} as
 * such:
 *
 * <pre>{@code
 * Map<String, Object> values = // ...
 * String key = "foo.bar.baz"; // say this is your key
 * Object val = // ...
 *
 * Map<String, Object> bazMap = new HashMap<>();
 * bazMap.put("baz", val);
 * Map<String, Object> barMap = new HashMap<>();
 * barMap.put("bar", bazMap);
 *
 * values.put("foo", barMap);
 * }</pre>
 *
 * of course by using clever tricks.
 *
 * <p>This key resolver also handles non-dotted keys (e.g. keys with no ".") with the same handling
 * as that {@link KeyResolver#DEFAULT} applies.
 *
 * <p>This is a singleton class ; obtain instance using {@link #getInstance()}.
 *
 * @author MrIvanPlays
 * @since 2.0.1
 */
public final class DottedResolver implements KeyResolver {

  private static DottedResolver instance;

  /**
   * Returns the instance of the {@link DottedResolver}
   *
   * @return instance
   */
  public static DottedResolver getInstance() {
    if (instance == null) {
      instance = new DottedResolver();
    }
    return instance;
  }

  private DottedResolver() {}

  /** {@inheritDoc} */
  @Override
  public Object unbox(String key, Map<String, Object> values) {
    if (!key.contains(".")) {
      return values.get(key);
    }

    String[] parts = key.split("\\.");
    int partsLength = parts.length;
    Map<String, Object> currentMap = null;
    for (int i = 0; i < partsLength; i++) {
      String currentPart = parts[i];
      if (currentMap == null) {
        if (!values.containsKey(currentPart)) {
          return null;
        }
        currentMap = getCurrentMap(values, currentPart, key, values);
      } else {
        if (!currentMap.containsKey(currentPart)) {
          return null;
        }
        Object next = currentMap.get(currentPart);
        if ((i + 1) == partsLength) {
          return next;
        } else {
          if (!(next instanceof Map)) {
            throw new IllegalArgumentException(
                "Invalid unbox ; either key is invalid or the values map ; "
                    + key
                    + " ; "
                    + values);
          }
          currentMap = (Map<String, Object>) next;
        }
      }
    }
    return null;
  }

  private Map<String, Object> getCurrentMap(
      Map<String, Object> current, String part, String key, Map<String, Object> values) {
    Object curr = current.get(part);
    if (!(curr instanceof Map)) {
      throw new IllegalArgumentException(
          "Invalid unbox ; either key is invalid or the values map ; " + key + " ; " + values);
    }
    return (Map<String, Object>) curr;
  }

  /** {@inheritDoc} */
  @Override
  public void boxTo(String key, Object value, Map<String, Object> values) {
    if (!key.contains(".")) {
      values.put(key, value);
      return;
    }

    String[] parts = key.split("\\.");
    int partsLength = parts.length;
    Map<String, Object> currentMap = null;
    for (int i = (partsLength - 1); i > 0; i--) {
      if (currentMap == null) {
        currentMap = getCurrentMap(parts, i, values);
        if (currentMap.containsKey(parts[i])) {
          throw new IllegalArgumentException("Illegal key to box '" + key + "'");
        }
        currentMap.put(parts[i], value);
      } else {
        Map<String, Object> newMap = getCurrentMap(parts, i, values);
        newMap.put(parts[i], currentMap);
        currentMap = newMap;
      }
    }
    values.put(parts[0], currentMap);
  }

  private Map<String, Object> getCurrentMap(
      String[] keyParts, int currentPart, Map<String, Object> target) {
    Map<String, Object> currentMap = target;
    for (int i = 0; i < keyParts.length; i++) {
      String part = keyParts[i];
      if (currentMap.containsKey(part)) {
        Object val = currentMap.get(part);
        if (!(val instanceof Map)) {
          throw new IllegalArgumentException("Illegal dotted key syntax.");
        }
        currentMap = (Map<String, Object>) val;
      } else {
        currentMap = new LinkedHashMap<>();
        break;
      }
      if (i == (currentPart - 1)) {
        break;
      }
    }
    return currentMap;
  }
}
