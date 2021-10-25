package com.mrivanplays.annotationconfig.core.resolver.options;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a class, containing options for reading or writing a file. This can contain literally
 * any information you want or need.
 *
 * @author MrIvanPlays
 * @since 2.0.0
 */
public final class CustomOptions {

  /**
   * Creates a new {@link CustomOptions} instance with the specified data.
   *
   * @param customData the data you want these options constructed with
   * @return populated mutable custom options instance
   */
  public static CustomOptions of(Map<String, Option> customData) {
    return new CustomOptions(customData);
  }

  /**
   * Creates a new empty {@link CustomOptions} instance.
   *
   * @return empty mutable custom options instance
   */
  public static CustomOptions empty() {
    return new CustomOptions();
  }

  private Map<String, Option> customData;

  private CustomOptions() {
    customData = new LinkedHashMap<>();
  }

  private CustomOptions(Map<String, Option> customData) {
    this.customData = customData;
  }

  /**
   * Returns whether this custom options instance has an {@link Option}, assigned to the specified
   * key.
   *
   * @param key the key of the option you want to check if exists
   * @return boolean value
   */
  public boolean has(String key) {
    return customData.containsKey(key);
  }

  /**
   * Returns whether the {@link Option}, contained or not, in this custom options instance, assigned
   * to the specified key, can be replaced. If there is an option assigned to the specified key,
   * this will return a populated {@link Optional}, otherwise empty optional.
   *
   * @param key the key of the option you want to check if it can be replaced
   * @return optional with a value, or empty optional
   */
  public Optional<Boolean> isReplaceable(String key) {
    Option value = customData.get(key);
    if (value == null) {
      return Optional.empty();
    }
    return Optional.of(value.replaceable());
  }

  /**
   * Returns the held {@link Option}, assigned to the specified key, as the specified type. If there
   * is no option assigned to this key, this will return an empty {@link Optional}, otherwise a
   * value if the option's value can be assigned to the specified type. If the option's value cannot
   * be assigned to the specified type, this method will throw an {@link IllegalArgumentException}
   *
   * @param key the key of the option you want to get
   * @param type the type you want it converted to
   * @param <T> type
   * @return filled optional or empty optional (check header)
   * @throws IllegalArgumentException if the value is not assignable to the specified type
   */
  public <T> Optional<T> getAs(String key, Class<T> type) {
    Option value = customData.get(key);
    if (value == null) {
      return Optional.empty();
    }
    if (value.value().getClass().isAssignableFrom(type)) {
      return Optional.of(type.cast(value));
    }
    throw new IllegalArgumentException(
        "Value " + value.value() + " cannot be assigned to " + type.getName());
  }

  /**
   * @param key the key of the option you want to get
   * @param type the type you want it converted to
   * @param def a default value
   * @param <T> type
   * @return always a usable value
   * @see #getAs(String, Class)
   */
  public <T> T getAsOr(String key, Class<T> type, T def) {
    return getAs(key, type).orElse(def);
  }

  /**
   * Assigns the specified {@link Option} to the specified key.
   *
   * @param key the key you want the specified option to be assigned to
   * @param value the option you want assigned to the specified key
   * @param <T> type
   */
  public <T> void put(String key, Option<T> value) {
    if (!customData.containsKey(key)) {
      customData.put(key, value);
    } else {
      Option current = customData.get(key);
      if (current == null) {
        customData.put(key, value);
        return;
      }
      if (current.replaceable()) {
        customData.replace(key, value);
        return;
      }
      throw new IllegalArgumentException(
          "There is a value at key " + key + "which is not replaceable");
    }
  }
}
