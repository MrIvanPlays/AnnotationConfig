package com.mrivanplays.annotationconfig.core.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents load settings. They are used upon annotated config load.
 *
 * @author MrIvanPlays
 * @since 2.0.0
 */
public final class LoadSettings {

  /**
   * Creates a new empty {@link LoadSettings.Builder}
   *
   * @return empty builder instance
   */
  public static LoadSettings.Builder newBuilder() {
    return newBuilder(false);
  }

  /**
   * Creates a new {@link LoadSettings.Builder}
   *
   * @param fromDefaults whether to instantiate load settings from {@link LoadSettings#getDefault()}
   * @return builder instance
   */
  public static LoadSettings.Builder newBuilder(boolean fromDefaults) {
    return new LoadSettings.Builder(fromDefaults);
  }

  /**
   * Creates new empty load settings. If used unmodified on a {@link ConfigResolver}, the config
   * resolver will fall back to the default settings (a.ka {@link LoadSettings#getDefault()}).
   *
   * @return empty load settings
   */
  public static LoadSettings empty() {
    return new LoadSettings();
  }

  private static LoadSettings def;

  /**
   * Returns the default load settings.
   *
   * @return defaults
   */
  public static LoadSettings getDefault() {
    if (def == null) {
      def = initializeDefaultSettings();
    }
    return def;
  }

  private static LoadSettings initializeDefaultSettings() {
    LoadSettings settings = new LoadSettings();
    settings.set(LoadSetting.GENERATE_NEW_OPTIONS, true);
    settings.set(LoadSetting.NULL_READ_HANDLER, NullReadHandleOption.SET_NULL);
    return settings;
  }

  private Map<String, Object> settings;

  private LoadSettings() {
    this.settings = new HashMap<>();
  }

  private LoadSettings(Map<String, Object> settings) {
    this.settings = settings;
  }

  /**
   * Returns the value held for the specified {@link LoadSetting} {@code setting}
   *
   * @param setting the load setting you want the value for
   * @param <T> value type
   * @return load setting value optional, which can be empty
   */
  public <T> Optional<T> get(LoadSetting<T> setting) {
    Object val = settings.get(setting.getKey());
    if (val == null || !setting.getType().isAssignableFrom(val.getClass())) {
      return Optional.empty();
    }
    return Optional.of(setting.getType().cast(val));
  }

  /**
   * Binds the specified {@code value} to the specified {@link LoadSetting} {@code setting}. If the
   * load setting already has a value, it gets replaced.
   *
   * @param setting the load setting you want the value to be bound to
   * @param value the value you want bound
   * @param <T> value type
   */
  public <T> void set(LoadSetting<T> setting, T value) {
    if (settings.containsKey(setting.getKey())) {
      settings.replace(setting.getKey(), value);
    } else {
      settings.put(setting.getKey(), value);
    }
  }

  /**
   * Creates a copy of the current load settings
   *
   * @return copy
   */
  public LoadSettings copy() {
    return new LoadSettings(this.settings);
  }

  /**
   * Represents a builder for {@link LoadSettings}
   *
   * @author MrIvanPlays
   * @since 2.0.0
   */
  public static final class Builder {

    private final LoadSettings loadSettings;

    public Builder(boolean fromDefaults) {
      if (fromDefaults) {
        loadSettings = LoadSettings.getDefault().copy();
      } else {
        loadSettings = LoadSettings.empty();
      }
    }

    private Builder(Builder other) {
      this.loadSettings = other.loadSettings;
    }

    /**
     * Creates a copy of the current builder.
     *
     * @return copy
     */
    public Builder copy() {
      return new Builder(this);
    }

    /**
     * Binds the specified {@code value} to the specified {@link LoadSetting} {@code setting}.
     *
     * @param setting the setting you want the value to be bound to
     * @param value the value you want bound
     * @param <T> value type
     * @return this instance for chaining
     */
    public <T> Builder withSetting(LoadSetting<T> setting, T value) {
      loadSettings.set(setting, value);
      return this;
    }

    /**
     * Builds this builder into {@link LoadSettings}
     *
     * @return load settings
     */
    public LoadSettings build() {
      return loadSettings;
    }
  }
}
