package com.mrivanplays.annotationconfig.core.serialization;

import java.util.Objects;

/**
 * Represents a builder of {@link DataObject}. Shall be used for multi value DataObject e.g. {@link
 * DataObject#isSingleValue()} returning {@code false}
 *
 * @since 2.1.0
 * @author MrIvanPlays
 */
public final class DataObjectBuilder {

  private final DataObject object;

  /** Creates a new {@link DataObjectBuilder} */
  public DataObjectBuilder() {
    this.object = new DataObject();
  }

  /**
   * Creates a copy of the specified {@link DataObjectBuilder} {@code other}
   *
   * @param other builder to copy
   */
  public DataObjectBuilder(DataObjectBuilder other) {
    this.object = Objects.requireNonNull(other, "other").object;
  }

  /**
   * Creates a new {@link DataObjectBuilder} from the specified {@link DataObject} {@code from}
   *
   * @param from the data object to build from
   */
  public DataObjectBuilder(DataObject from) {
    this.object = Objects.requireNonNull(from, "from");
  }

  /**
   * Creates a new copy of this {@link DataObjectBuilder}
   *
   * @return copy
   */
  public DataObjectBuilder copy() {
    return new DataObjectBuilder(this);
  }

  /**
   * Binds the specified {@code value} to the specified {@code key}
   *
   * @param key key to bind to
   * @param value value to bind
   * @return this instance for chaining
   */
  public DataObjectBuilder with(String key, String value) {
    object.put(key, value);
    return this;
  }

  /**
   * Binds the specified {@code value} to the specified {@code key}
   *
   * @param key key to bind to
   * @param value value to bind
   * @return this instance for chaining
   */
  public DataObjectBuilder with(String key, boolean value) {
    object.put(key, value);
    return this;
  }

  /**
   * Binds the specified {@code value} to the specified {@code key}
   *
   * @param key key to bind to
   * @param value value to bind
   * @return this instance for chaining
   */
  public DataObjectBuilder with(String key, Number value) {
    object.put(key, value);
    return this;
  }

  /**
   * Binds the specified {@link DataObjectBuilder}'s {@link DataObject} to the specified {@code key}
   *
   * @param key key to bind to
   * @param other value to bind
   * @return this instance for chaining
   */
  public DataObjectBuilder with(String key, DataObjectBuilder other) {
    return with(key, other.build());
  }

  /**
   * Binds the specified {@link DataObject} to the specified {@code key}.
   *
   * @param key key to bind to
   * @param object value to bind
   * @return this instance for chaining
   */
  public DataObjectBuilder with(String key, DataObject object) {
    object.putAll(key, object);
    return this;
  }

  /**
   * Builds this {@link DataObjectBuilder} into a {@link DataObject}
   *
   * @return built object
   */
  public DataObject build() {
    return object;
  }
}
