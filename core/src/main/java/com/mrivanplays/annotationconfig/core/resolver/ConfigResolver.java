package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.internal.ConfigResolverImpl;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.options.Option;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSetting;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSettings;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

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
   * Dumps the specified {@code annotatedConfig} to the specified {@link File} {@code file}. If, at
   * the time of calling this method, the file exists, it will get deleted ( default implementation
   * ).
   *
   * <p>If you are going to call {@link #load(Object, File, LoadSettings)} after calling this
   * method, consider using {@link #loadOrDump(Object, File, LoadSettings)} rather than calling dump
   * and load one after each other. This way you save CPU time by not making the library find
   * annotations twice.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param file the file you want to dump the annotated config to
   */
  void dump(Object annotatedConfig, File file);

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link OutputStream} {@code os}.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param os the output stream to dump the annotated config to
   */
  default void dump(Object annotatedConfig, OutputStream os) {
    dump(annotatedConfig, new OutputStreamWriter(os));
  }

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link Writer} {@code writer}.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param writer the writer to dump the annotated config to
   */
  void dump(Object annotatedConfig, Writer writer);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link File} {@code file} using
   * the default {@link LoadSettings} from the builder of this config resolver, or {@link
   * LoadSettings#getDefault()}.
   *
   * <p>If you have called {@link #dump(Object, File)} before calling this method, consider using
   * {@link #loadOrDump(Object, File)} rather than calling dump and load one after each other. This
   * way you save CPU time by not making the library find annotations twice.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param file the file you want to load
   */
  void load(Object annotatedConfig, File file);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link File} {@code file} using
   * the {@link LoadSettings} {@code loadSettings} specified.
   *
   * <p>If you have called {@link #dump(Object, File)} before calling this method, consider using
   * {@link #loadOrDump(Object, File, LoadSettings)} rather than calling dump and load one after
   * each other. This way you save CPU time by not making the library find annotations twice.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param file the file you want to load
   * @param loadSettings the load settings
   */
  void load(Object annotatedConfig, File file, LoadSettings loadSettings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Map} {@code values} using
   * the default {@link LoadSettings} from the builder of this config resolver, or {@link
   * LoadSettings#getDefault()}
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param values the values you want to load
   */
  void load(Object annotatedConfig, Map<String, Object> values);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Map} {@code values} using
   * the {@link LoadSettings} {@code loadSettings} specified.
   *
   * @param annotatedConfig the annotated you config you want to load to
   * @param values the values you want to load
   * @param loadSettings the load settings
   */
  void load(Object annotatedConfig, Map<String, Object> values, LoadSettings loadSettings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link InputStream} {@code in}
   * using the default {@link LoadSettings} from the builder of this config resolver, or {@link
   * LoadSettings#getDefault()}.
   *
   * <p>If you have a {@link File} instance before calling that, consider using {@link #load(Object,
   * File)}. This way you allow AnnotatedConfig to generate missing options if the load settings
   * allow it.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param in the input stream you want to load
   */
  default void load(Object annotatedConfig, InputStream in) {
    load(annotatedConfig, new InputStreamReader(in));
  }

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link InputStream} {@code in}
   * using the {@link LoadSettings} {@code loadSettings} specified.
   *
   * <p>If you have a {@link File} instance before calling that, consider using {@link #load(Object,
   * File, LoadSettings)}. This way you allow AnnotatedConfig to generate missing options if the
   * load settings allow it.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param in the input stream you want to load
   * @param loadSettings the load settings
   */
  default void load(Object annotatedConfig, InputStream in, LoadSettings loadSettings) {
    load(annotatedConfig, new InputStreamReader(in), loadSettings);
  }

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Reader} {@code reader}
   * using the default {@link LoadSettings} from the builder of this config resolver, or {@link
   * LoadSettings#getDefault()}.
   *
   * <p>If you have a {@link File} instance before calling that, consider using {@link #load(Object,
   * File)}. This way you allow AnnotatedConfig to generate missing options if the load settings
   * allow it.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param reader the reader you want to load
   */
  void load(Object annotatedConfig, Reader reader);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Reader} {@code reader}
   * using the {@link LoadSettings} {@code loadSettings} specified.
   *
   * <p>If you have a {@link File} instance before calling that, consider using {@link #load(Object,
   * File, LoadSettings)}. This way you allow AnnotatedConfig to generate missing options if the
   * load settings allow it.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param reader the reader you want to load
   * @param loadSettings the load settings
   */
  void load(Object annotatedConfig, Reader reader, LoadSettings loadSettings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link File} {@code file}, if it
   * exists, if not, dumps the specified {@code annotatedConfig} to the specified file, using the
   * default {@link LoadSettings} from the builder of this config resolver, or {@link
   * LoadSettings#getDefault()}.
   *
   * @param annotatedConfig the annotated config you want to load/dump
   * @param file the file you want to load/dump to
   */
  void loadOrDump(Object annotatedConfig, File file);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link File} {@code file}, if it
   * exists, if not, dumps the specified {@code annotatedConfig} to the specified file, using the
   * {@link LoadSettings} {@code loadSettings} specified.
   *
   * @param annotatedConfig the annotated config you want to load/dump
   * @param file the file you want to load/dump to
   * @param loadSettings the load settings
   */
  void loadOrDump(Object annotatedConfig, File file, LoadSettings loadSettings);

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
    private LoadSettings defaultLoadSettings;
    private KeyResolver keyResolver;
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
      this.defaultLoadSettings = copy.defaultLoadSettings;
      this.keyResolver = copy.keyResolver;
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
     * Sets a {@link ValueWriter} for the config type you want to generate configs. This cannot be
     * null.
     *
     * @param val the value writer supplier you want to set
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
     * @param <T> value type
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
     * Binds the specified {@code value} to the specified {@link LoadSetting} {@code setting} in the
     * default {@link LoadSettings}
     *
     * @param setting the setting you want this value to be bound to
     * @param value the value bound
     * @param <T> value type
     * @return this instance for chaining
     */
    public <T> Builder withLoadSetting(LoadSetting<T> setting, T value) {
      if (defaultLoadSettings == null) {
        defaultLoadSettings = LoadSettings.getDefault().copy();
      }
      defaultLoadSettings.set(setting, value);
      return this;
    }

    /**
     * Sets the {@link KeyResolver} for the config type you want to generate configs. If null, it
     * will use the default {@link KeyResolver#DEFAULT}.
     *
     * @param resolver the resolver you want to set
     * @return this instance for chaining
     */
    public Builder withKeyResolver(KeyResolver resolver) {
      this.keyResolver = resolver;
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
          commentPrefix,
          valueWriter,
          valueReader,
          options,
          defaultLoadSettings,
          keyResolver,
          reverseFields);
    }
  }
}
