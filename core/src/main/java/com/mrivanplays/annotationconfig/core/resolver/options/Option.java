package com.mrivanplays.annotationconfig.core.resolver.options;

import java.util.Objects;

/**
 * Represents an option, which is holding a non-null value and information whether it can be
 * replaced or not.
 *
 * @param <T> type
 * @author MrIvanPlays
 * @since 2.0.0
 */
public final class Option<T> {

  /**
   * Creates a new {@link Option} instance.
   *
   * @param value a non-null value to be held by this option
   * @param <T> type
   * @return option instance
   */
  public static <T> Option<T> of(T value) {
    return new Option<>(value);
  }

  private final T value;
  private boolean replaceable = false;

  private Option(T value) {
    this.value = Objects.requireNonNull(value, "value");
  }

  /**
   * Returns whether this option is replaceable in a {@link CustomOptions} context. By default, this
   * will return a {@code false} boolean value.
   *
   * @return replaceable or not
   */
  public boolean replaceable() {
    return replaceable;
  }

  /**
   * Marks this option instance as replaceable.
   *
   * @return this instance for chaining
   */
  public Option<T> markReplaceable() {
    this.replaceable = true;
    return this;
  }

  /**
   * Returns the value held by this option.
   *
   * @return value
   */
  public T value() {
    return value;
  }
}
