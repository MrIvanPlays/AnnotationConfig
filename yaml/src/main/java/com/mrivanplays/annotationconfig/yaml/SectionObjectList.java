package com.mrivanplays.annotationconfig.yaml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
 * @author MrIvanPlays
 * @since v2.1.1
 */
public final class SectionObjectList<T> {

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
    this.clazz = clazz;
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
   * Represents a builder of {@link SectionObjectList}
   *
   * @param <T> object type
   * @author MrIvanPlays
   * @since v2.1.1
   */
  public static final class SectionObjectListBuilder<T> {

    private final Map<String, T> values = new HashMap<>();
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
     * @param value default value value
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
