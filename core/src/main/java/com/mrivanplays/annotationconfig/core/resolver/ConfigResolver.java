package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.internal.ConfigResolverImpl;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Setting;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
   * Returns the {@link Settings} instance held by this config resolver.
   *
   * @return settings
   * @see Settings
   */
  Settings settings();

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link File} {@code file}. If, at
   * the time of calling this method, the file exists, it will get deleted ( default implementation
   * ).
   *
   * <p>If you are going to call {@link #load(Object, File, Settings)} after calling this method,
   * consider using {@link #loadOrDump(Object, File, Settings)} rather than calling dump and load
   * one after each other. This way you save CPU time by not making the library find annotations
   * twice.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param file the file you want to dump the annotated config to
   */
  void dump(Object annotatedConfig, File file);

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link Path} {@code path}. If, at
   * the time of calling this method, the file exists, it will get deleted ( default implementation
   * ).
   *
   * <p>If you are going to call {@link #load(Object, Path, Settings)} after calling this method,
   * consider using {@link #loadOrDump(Object, Path, Settings)} rather than calling dump and load
   * one after each other. This way you save CPU time by not making the library find annotations
   * twice.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param path the file path you want to dump the annotation config to
   */
  void dump(Object annotatedConfig, Path path);

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
   * the default {@link Settings} from the builder of this config resolver, or {@link
   * ACDefaultSettings#getDefault()}.
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
   * Loads the specified {@code annotatedConfig} from the specified {@link Path} {@code path} using
   * the default {@link Settings} from the builder of this config resolver, or {@link
   * ACDefaultSettings#getDefault()}.
   *
   * <p>If you have called {@link #dump(Object, Path)} before calling this method, consider using
   * {@link #loadOrDump(Object, Path)} rather than calling dump and load one after each other. This
   * way you save CPU time by not making the library find annotations twice.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param path the file path you want to load
   */
  void load(Object annotatedConfig, Path path);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link File} {@code file} using
   * the {@link Settings} {@code settings} specified.
   *
   * <p>If you have called {@link #dump(Object, File)} before calling this method, consider using
   * {@link #loadOrDump(Object, File, Settings)} rather than calling dump and load one after each
   * other. This way you save CPU time by not making the library find annotations twice.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param file the file you want to load
   * @param settings the load settings
   */
  void load(Object annotatedConfig, File file, Settings settings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Path} {@code path} using
   * the {@link Settings} {@code settings} specified.
   *
   * <p>If you have called {@link #dump(Object, Path)} before calling this method, consider using
   * {@link #loadOrDump(Object, Path, Settings)} rather than calling dump and load one after each
   * other. This way you save CPU time by not making the library find annotations twice.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param path the file path you want to load
   * @param settings the load settings
   */
  void load(Object annotatedConfig, Path path, Settings settings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Map} {@code values} using
   * the default {@link Settings} from the builder of this config resolver, or {@link
   * ACDefaultSettings#getDefault()}.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param values the values you want to load
   */
  void load(Object annotatedConfig, Map<String, Object> values);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Map} {@code values} using
   * the {@link Settings} {@code settings} specified.
   *
   * @param annotatedConfig the annotated you config you want to load to
   * @param values the values you want to load
   * @param settings the load settings
   */
  void load(Object annotatedConfig, Map<String, Object> values, Settings settings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link InputStream} {@code in}
   * the default {@link Settings} from the builder of this config resolver, or {@link
   * ACDefaultSettings#getDefault()}.
   *
   * <p>If you have a {@link File} or {@link Path} instance before calling that, consider using
   * {@link #load(Object, File)}. This way you allow AnnotatedConfig to generate missing options if
   * the load settings allow it.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param in the input stream you want to load
   */
  default void load(Object annotatedConfig, InputStream in) {
    load(annotatedConfig, new InputStreamReader(in));
  }

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link InputStream} {@code in}
   * using the {@link Settings} {@code settings} specified.
   *
   * <p>If you have a {@link File} or {@link Path} instance before calling that, consider using
   * {@link #load(Object, File, Settings)}. This way you allow AnnotatedConfig to generate missing
   * options if the load settings allow it.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param in the input stream you want to load
   * @param settings the load settings
   */
  default void load(Object annotatedConfig, InputStream in, Settings settings) {
    load(annotatedConfig, new InputStreamReader(in), settings);
  }

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Reader} {@code reader}
   * the default {@link Settings} from the builder of this config resolver, or {@link
   * ACDefaultSettings#getDefault()}.
   *
   * <p>If you have a {@link File} or {@link Path} instance before calling that, consider using
   * {@link #load(Object, File)}. This way you allow AnnotatedConfig to generate missing options if
   * the load settings allow it.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param reader the reader you want to load
   */
  void load(Object annotatedConfig, Reader reader);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Reader} {@code reader}
   * using the {@link Settings} {@code settings} specified.
   *
   * <p>If you have a {@link File} or {@link Path} instance before calling that, consider using
   * {@link #load(Object, File, Settings)}. This way you allow AnnotatedConfig to generate missing
   * options if the load settings allow it.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param reader the reader you want to load
   * @param settings the load settings
   */
  void load(Object annotatedConfig, Reader reader, Settings settings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link File} {@code file}, if it
   * exists, if not, dumps the specified {@code annotatedConfig} to the specified file, using the
   * default {@link Settings} from the builder of this config resolver, or {@link
   * ACDefaultSettings#getDefault()}.
   *
   * @param annotatedConfig the annotated config you want to load/dump
   * @param file the file you want to load/dump to
   */
  void loadOrDump(Object annotatedConfig, File file);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Path} {@code path}, if it
   * exists, if not, dumps the specified {@code annotatedConfig} to the specified file, using the
   * default {@link Settings} from the builder of this config resolver, or {@link
   * ACDefaultSettings#getDefault()}.
   *
   * @param annotatedConfig the annotated config you want to load/dump
   * @param path the file path you want to load/dump to
   */
  void loadOrDump(Object annotatedConfig, Path path);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link File} {@code file}, if it
   * exists, if not, dumps the specified {@code annotatedConfig} to the specified file, using the
   * {@link Settings} {@code settings} specified.
   *
   * @param annotatedConfig the annotated config you want to load/dump
   * @param file the file you want to load/dump to
   * @param settings the load settings
   */
  void loadOrDump(Object annotatedConfig, File file, Settings settings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Path} {@code path}, if it
   * exists, if not, dumps the specified {@code annotatedConfig} to the specified file, using the
   * {@link Settings} {@code settings} specified.
   *
   * @param annotatedConfig the annotated config you want to load/dump
   * @param path the file path you want to load/dump to
   * @param settings the load settings
   */
  void loadOrDump(Object annotatedConfig, Path path, Settings settings);

  /**
   * Loads the configurations in the {@link File} {@code dir} specified, and if any do not exist,
   * the defaults (if specified) are dumped to the specified {@link WritableObject} {@code
   * dumpFile}. <br>
   * <b>WARNING: The specified {@link File} {@code dir} has to be a directory!!!!!</b>
   *
   * @param dir the directory to load configurations from
   * @param configToResolveTo a {@link Supplier} of the needed configuration objects
   * @param dumpFile the default dump file
   * @return a map of the loaded configurations, key being the file name. If the map is empty then
   *     AnnotationConfig couldn't find any configurations in the folder, and you should fall back
   *     to the defaults.
   * @throws IllegalArgumentException if the specified {@code dir} is not a directory!
   * @param <T> configuration type needed
   */
  <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile);

  /**
   * Loads the configurations in the {@link File} {@code dir} specified, and if any do not exist,
   * the defaults (if specified) are dumped to the specified {@link WritableObject} {@code
   * dumpFile}. <br>
   * <b>WARNING: The specified {@link File} {@code dir} has to be a directory!!!!!</b>
   *
   * @param dir the directory to load configurations from
   * @param configToResolveTo a {@link Supplier} of the needed configuration objects
   * @param dumpFile the default dump file
   * @param settings the load settings to use when loading the configurations
   * @return a map of the loaded configurations, key being the file name. If the map is empty then
   *     AnnotationConfig couldn't find any configurations in the folder, and you should fall back
   *     to the defaults.
   * @throws IllegalArgumentException if the specified {@code dir} is not a directory!
   * @param <T> configuration type needed
   */
  <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile, Settings settings);

  /**
   * Loads the configurations in the {@link Path} {@code dir} specified, and if any do not exist,
   * the defaults (if specified) are dumped to the specified {@link WritableObject} {@code
   * dumpFile}. <br>
   * <b>WARNING: The specified {@link Path} {@code dir} has to be a directory!!!!!</b>
   *
   * @param dir the directory to load configurations from
   * @param configToResolveTo a {@link Supplier} of the needed configuration objects
   * @param dumpFile the default dump file
   * @return a map of the loaded configurations, key being the file name. If the map is empty then
   *     AnnotationConfig couldn't find any configurations in the folder, and you should fall back
   *     to the defaults.
   * @throws IllegalArgumentException if the specified {@code dir} is not a directory!
   * @param <T> configuration type needed
   */
  <T> Map<String, T> resolveMultiple(
      Path dir, Supplier<T> configToResolveTo, WritableObject dumpFile);
  /**
   * Loads the configurations in the {@link Path} {@code dir} specified, and if any do not exist,
   * the defaults (if specified) are dumped to the specified {@link WritableObject} {@code
   * dumpFile}. <br>
   * <b>WARNING: The specified {@link Path} {@code dir} has to be a directory!!!!!</b>
   *
   * @param dir the directory to load configurations from
   * @param configToResolveTo a {@link Supplier} of the needed configuration objects
   * @param dumpFile the default dump file
   * @param settings the load settings to use when loading the configurations
   * @return a map of the loaded configurations, key being the file name. If the map is empty then
   *     AnnotationConfig couldn't find any configurations in the folder, and you should fall back
   *     to the defaults.
   * @throws IllegalArgumentException if the specified {@code dir} is not a directory!
   * @param <T> configuration type needed
   */
  <T> Map<String, T> resolveMultiple(
      Path dir, Supplier<T> configToResolveTo, WritableObject dumpFile, Settings settings);

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
    private Settings settings;
    private KeyResolver keyResolver;
    private List<String> fileExtensions;
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
      this.settings = copy.settings;
      this.keyResolver = copy.keyResolver;
      this.fileExtensions = copy.fileExtensions;
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
     * Binds the specified {@code value} to the specified {@link Setting} {@code setting} in the
     * default {@link Settings}
     *
     * @param setting the setting you want this value to be bound to
     * @param value the value bound
     * @param <T> value type
     * @return this instance for chaining
     */
    public <T> Builder withSetting(Setting<T> setting, T value) {
      if (settings == null) {
        settings = ACDefaultSettings.getDefault().copy();
      }
      settings.put(setting, value);
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
     * Sets a file extension for the config type you want to generate configs for.
     *
     * <p>A config file can have multiple extensions, so calling this method multiple times with
     * different values is allowed.
     *
     * <p>As an example, the file extension for YAML is ".yml".
     *
     * @param fileExtension file extension
     * @return this instance for chaining
     */
    public Builder withFileExtension(String fileExtension) {
      if (this.fileExtensions == null) {
        this.fileExtensions = new ArrayList<>();
      }
      if (!this.fileExtensions.contains(fileExtension)) {
        this.fileExtensions.add(fileExtension);
      }
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
      if (fileExtensions == null || fileExtensions.isEmpty()) {
        throw new IllegalArgumentException("No file extensions specified");
      }
      return new ConfigResolverImpl(
          commentPrefix,
          valueWriter,
          valueReader,
          settings,
          keyResolver,
          fileExtensions.toArray(new String[0]),
          reverseFields);
    }
  }
}
