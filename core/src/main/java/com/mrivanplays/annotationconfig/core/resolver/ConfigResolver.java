package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.internal.ConfigResolverImpl;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.options.Option;
import java.io.File;

/**
 * Represents a resolver of configurations.
 *
 * <p>Should be used for implementing new configuration types
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
public interface ConfigResolver {

  /**
   * Creates a new {@link ConfigResolver.Builder}
   *
   * @return new builder
   */
  static ConfigResolver.Builder newBuilder() {
    return new ConfigResolver.Builder();
  }

  /**
   * Returns the {@link CustomOptions} instance.
   *
   * @return options
   * @see CustomOptions
   */
  CustomOptions options();

  /**
   * Dumps the specified annotated config to the specified {@link File}. If the specified {@link
   * File} exists, it will get deleted ( default implementation ).
   *
   * <p>If you are going to call {@link #load(Object, File, boolean)} after calling this method,
   * consider using {@link #loadOrDump(Object, File, boolean)} rather than calling dump and load one
   * after each other. This way you save CPU time by not making the library find annotations twice.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param file the file you want to dump the config to
   */
  void dump(Object annotatedConfig, File file);

  /**
   * Loads the specified {@link File} to the specified annotated config.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param file the file you want to load
   * @param generateNewOptions whether to generate options which don't persist in the file
   * @see #load(Object, File, boolean, NullReadHandleOption)
   */
  default void load(Object annotatedConfig, File file, boolean generateNewOptions) {
    load(annotatedConfig, file, generateNewOptions, NullReadHandleOption.SET_NULL);
  }

  /**
   * Loads the specified {@link File} to the specified annotated config. If the {@link File} is
   * empty or no values have been read, it won't modify anything on the specified annotated config (
   * default implementation ).
   *
   * <p>If you have called {@link #dump(Object, File)} before calling this method, consider using
   * {@link #loadOrDump(Object, File, boolean)} rather than calling dump and load one after each
   * other. This way you save CPU time by not making the library find annotations twice.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param file the file you want to load
   * @param generateNewOptions whether to generate options which don't persist in the file
   * @param nullReadHandler how to handle null options
   */
  void load(Object annotatedConfig, File file, boolean generateNewOptions, NullReadHandleOption nullReadHandler);

  /**
   * Loads the specific {@link File} to the specified annotated config.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param file the file you want to load
   * @see #load(Object, File, boolean)
   */
  default void load(Object annotatedConfig, File file) {
    load(annotatedConfig, file, true);
  }

  /**
   * Dumps the specified annotated config if the specified {@link File} doesn't exist or loads the
   * specified {@link File} to the specified annotated config if the specified {@link File} exists.
   *
   * @param annotatedConfig the annotated config you want to dump/load
   * @param file the file you want to dump to or load from
   * @param generateNewOptions whether to generate options which don't persist in the file
   * @see #loadOrDump(Object, File, boolean, NullReadHandleOption)
   */
  default void loadOrDump(Object annotatedConfig, File file, boolean generateNewOptions) {
    loadOrDump(annotatedConfig, file, generateNewOptions, NullReadHandleOption.SET_NULL);
  }

  /**
   * Dumps the specified annotated config if the specified {@link File} doesn't exist or loads the
   * specified {@link File} to the specified annotated config if the specified {@link File} exists.
   * If the {@link File} is empty or no values have been read, it won't modify anything on the
   * specified annotated config ( default implementation )
   *
   * @param annotatedConfig the annotated config you want to dump/load
   * @param file the file you want to dump to or load from
   * @param generateNewOptions whether to generate options which don't persist in the file
   * @param nullReadHandler how to handle null options
   */
  void loadOrDump(Object annotatedConfig, File file, boolean generateNewOptions, NullReadHandleOption nullReadHandler);

  /**
   * Dumps the specified annotated config if the specified {@link File} doesn't exist or loads the
   * specified {@link File} to the specified annotated config if the specified {@link File} exists.
   *
   * @param annotatedConfig the annotated config you want to dump/load
   * @param file the file you want to dump to or load from
   * @see #loadOrDump(Object, File, boolean)
   */
  default void loadOrDump(Object annotatedConfig, File file) {
    loadOrDump(annotatedConfig, file, true);
  }

  /**
   * Represents a builder of a {@link ConfigResolver}
   *
   * @since 2.0.0
   * @author MrIvanPlays
   */
  class Builder {

    private String commentPrefix;
    private ValueWriter valueWriter;
    private ValueReader valueReader;
    private CustomOptions options;
    private boolean reverseFields = false;

    public Builder() {}

    /**
     * Creates a copy of the specified builder
     *
     * @param copy the builder you want to copy
     */
    public Builder(ConfigResolver.Builder copy) {
      this.commentPrefix = copy.commentPrefix;
      this.valueWriter = copy.valueWriter;
      this.valueReader = copy.valueReader;
      this.options = copy.options;
      this.reverseFields = copy.reverseFields;
    }

    /**
     * Creates a copy of this builder
     *
     * @return a new builder instance, copy of this builder instance
     */
    public Builder copy() {
      return new Builder(this);
    }

    /**
     * Sets the comment prefix for the config type you want to generate configs. This cannot be
     * null.
     *
     * @param val the config prefix you want to set
     * @return this instance for chaining
     */
    public Builder withCommentPrefix(String val) {
      commentPrefix = val;
      return this;
    }

    /**
     * Sets the {@link ValueWriter} for the config type you want to generate configs. This cannot be
     * null.
     *
     * @param val the value writer you want to set
     * @return this instance for chaining
     */
    public Builder withValueWriter(ValueWriter val) {
      valueWriter = val;
      return this;
    }

    /**
     * Sets the value reader for the config type you want to generate configs. This cannot be null.
     *
     * @param val the value reader you want to set
     * @return this instance for chaining
     */
    public Builder withValueReader(ValueReader val) {
      valueReader = val;
      return this;
    }

    /**
     * Binds the specified {@link Option} value to the specifed key.
     *
     * @param key the key you want this option to be bound to
     * @param value the value bound
     * @return this instance for chaining
     */
    public <T> Builder withOption(String key, Option<T> value) {
      if (options == null) {
        options = CustomOptions.empty();
      }
      options.put(key, value);
      return this;
    }

    /**
     * Sets whether to reverse fields when generating options. This is needed for some config types
     * so the generated options are chronological with the annotated config's fields. The default
     * value here is {@code false}.
     *
     * @param val whether to reverse fields
     * @return this instance for chaining
     */
    public Builder shouldReverseFields(boolean val) {
      reverseFields = val;
      return this;
    }

    /**
     * Builds a new {@link ConfigResolver} ready for use.
     *
     * @return new config resolver instance
     */
    public ConfigResolver build() {
      return new ConfigResolverImpl(
          commentPrefix, valueWriter, valueReader, options, reverseFields);
    }
  }
}
