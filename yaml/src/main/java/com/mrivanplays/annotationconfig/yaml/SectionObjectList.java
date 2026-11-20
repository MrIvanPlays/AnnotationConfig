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

package com.mrivanplays.annotationconfig.yaml;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a section list. A YAML example:
 *
 * <pre><code>
 * foo:
 *   bar:
 *     baz: aa
 *     lorem: ipsum
 *     dolor: sit
 *     amet: lorem
 *   ipsum:
 *     baz: bb
 *     lorem: dolor
 *     dolor: lorem
 *     amet: ipsum
 * </code></pre>
 *
 * <p>How to use: you have to register a {@link SectionObjectListSerializer} with the help of a
 * {@link com.mrivanplays.annotationconfig.core.utils.TypeToken} in order for this list to properly
 * (de)serialize.
 *
 * @author Ivan Pekov
 * @since v2.1.1
 */
@NullMarked
public final class SectionObjectList<T> implements Iterable<Entry<String, T>> {

  /**
   * Creates a new {@link SectionObjectListBuilder} with pre-defined {@link Class} type.
   *
   * @param type type
   * @param <T> object type
   * @return new builder
   */
  public static <T> SectionObjectListBuilder<T> newBuilderForType(Class<? extends T> type) {
    return new SectionObjectListBuilder<>(type);
  }

  private final Map<String, T> values;
  private final Class<? extends T> clazz;

  SectionObjectList(Class<? extends T> clazz, Map<String, T> values) {
    this.clazz = Objects.requireNonNull(clazz, "class");
    this.values = values;
  }

  /**
   * Returns the {@link Class} type of the value objects held in this section object list.
   *
   * @return objects type
   */
  public Class<? extends T> getObjectsType() {
    return clazz;
  }

  /**
   * Returns an unmodifiable representation of this {@code SectionObjectList} as a {@link Map}
   *
   * @return as map
   */
  public Map<String, T> getAsMap() {
    return Collections.unmodifiableMap(values);
  }

  /**
   * Returns an immutable {@link Iterator} representation of the keys and values held in this {@code
   * SectionObjectList}.
   *
   * @return iterator
   */
  @Override
  public Iterator<Entry<String, T>> iterator() {
    // immutable since 3.0.4
    Iterator<Entry<String, T>> original = this.values.entrySet().iterator();
    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return original.hasNext();
      }

      @Override
      public Entry<String, T> next() {
        return original.next();
      }
    };
  }

  /**
   * Check whether this {@code SectionObjectList} contains a specific key
   *
   * @param key key to check, cannot be null
   * @return true if contains, false otherwise
   * @since 3.0.4
   */
  public boolean contains(String key) {
    Objects.requireNonNull(key, "key");
    return this.values.containsKey(key);
  }

  /**
   * Update the specified {@code key} with a new value. Setting the value to {@code null} will
   * remove existing keys.
   *
   * @param key key to update, cannot be null
   * @param newValue new value to update with
   * @since 3.0.4
   */
  public void update(String key, @Nullable T newValue) {
    Objects.requireNonNull(key, "key");
    if (newValue == null) {
      this.values.remove(key);
    } else {
      this.values.put(key, newValue);
    }
  }

  /**
   * Check whether this {@code SectionObjectList} is empty.
   *
   * @return empty or not
   * @since 3.0.4
   */
  public boolean isEmpty() {
    return this.values.isEmpty();
  }

  /**
   * Represents a builder of {@link SectionObjectList}
   *
   * @param <T> object type
   * @author Ivan Pekov
   * @since v2.1.1
   */
  public static final class SectionObjectListBuilder<T> {

    private final Map<String, T> values = new LinkedHashMap<>();
    private final Class<? extends T> type;

    private SectionObjectListBuilder(Class<? extends T> type) {
      this.type = Objects.requireNonNull(type, "type");
      if (SectionObjectList.class.isAssignableFrom(type)) {
        throw new IllegalArgumentException("SectionObjectList<SectionObjectList>");
      }
      if (SectionObjectListBuilder.class.isAssignableFrom(type)) {
        throw new IllegalArgumentException("SectionObjectList<SectionObjectListBuilder>");
      }
    }

    /**
     * Specify a default value held by the created {@link SectionObjectList}
     *
     * @param key default value key
     * @param value default value
     * @return this instance for chaining
     */
    public SectionObjectListBuilder<T> defaultValue(String key, T value) {
      this.values.put(key, value);
      return this;
    }

    /**
     * Builds a new {@link SectionObjectList} from the given parameters.
     *
     * @return new section object list
     */
    public SectionObjectList<T> build() {
      if (values.isEmpty()) {
        throw new IllegalStateException(
            "No defaults for SectionObjectList<" + type.getSimpleName() + ">");
      }
      return new SectionObjectList<>(type, values);
    }
  }
}
