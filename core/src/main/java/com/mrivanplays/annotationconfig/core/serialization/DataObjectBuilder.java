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

package com.mrivanplays.annotationconfig.core.serialization;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a builder of {@link DataObject}. Shall be used for multi value DataObject e.g. {@link
 * DataObject#isSingleValue()} returning {@code false}
 *
 * @since 2.1.0
 * @author Ivan Pekov
 */
@NullMarked
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
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
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
    Objects.requireNonNull(key, "key");
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
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
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
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(object, "object");
    this.object.putAll(key, object);
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
