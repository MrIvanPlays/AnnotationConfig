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

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
 * @author Ivan Pekov
 * @since 2.0.1
 */
@SuppressWarnings("unchecked")
@NullMarked
public final class DottedResolver implements KeyResolver {

  @Nullable private static DottedResolver instance;

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
  @Nullable
  public Object unbox(String key, Map<String, Object> values) {
    if (!key.contains(".")) {
      return values.get(key);
    }

    Map<String, Object> currentMap = values;
    int start = 0;
    int end;

    while ((end = key.indexOf('.', start)) != -1) {
      String part = key.substring(start, end);

      if (!currentMap.containsKey(part)) {
        return null;
      }

      Object next = currentMap.get(part);

      if (!(next instanceof Map)) {
        throw new IllegalArgumentException(
            "Invalid unbox ; either key is invalid or the values map ; " + key + " ; " + values);
      }

      currentMap = (Map<String, Object>) next;
      start = end + 1;
    }

    return currentMap.get(key.substring(start));
  }

  /** {@inheritDoc} */
  @Override
  public void boxTo(String key, Object value, Map<String, Object> values) {
    if (!key.contains(".")) {
      values.put(key, value);
      return;
    }

    Map<String, Object> currentMap = values;
    int start = 0;
    int end;

    while ((end = key.indexOf('.', start)) != -1) {
      String part = key.substring(start, end);
      Object next = currentMap.get(part);

      if (next == null) {
        Map<String, Object> newMap = new LinkedHashMap<>();
        currentMap.put(part, newMap);
        currentMap = newMap;
      } else if (next instanceof Map) {
        currentMap = (Map<String, Object>) next;
      } else {
        throw new IllegalArgumentException("Illegal dotted key syntax.");
      }

      start = end + 1;
    }

    String lastPart = key.substring(start);

    if (currentMap.containsKey(lastPart)) {
      throw new IllegalArgumentException("Illegal key to box '" + key + "'");
    }

    currentMap.put(lastPart, value);
  }
}
