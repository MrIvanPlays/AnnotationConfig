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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a resolver of configurations.
 *
 * <p>Should be used for implementing new configuration types
 *
 * @since 2.0.0
 * @author Ivan Pekov
 */
@NullMarked
public interface ConfigResolver {

  /**
   * Creates a new {@link ConfigResolver.Builder}.
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
   * Dumps the specified {@code annotatedConfig} to the specified {@link File} {@code file}. An
   * existing file is replaced.
   *
   * <p>To write the defaults only when the file doesn't exist yet and load it otherwise, use {@link
   * #loadOrDump(Object, File)}.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param file the file you want to dump the annotated config to
   */
  void dump(Object annotatedConfig, File file);

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link File file}, using the
   * specified {@link Settings settings}. An existing file is replaced.
   *
   * <p>To write the defaults only when the file doesn't exist yet and load it otherwise, use {@link
   * #loadOrDump(Object, File, Settings)}.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config to dump
   * @param file the file to dump to
   * @param settings the settings to use
   * @throws IllegalArgumentException if the provided settings are immutable
   * @since 3.1.0
   */
  void dump(Object annotatedConfig, File file, Settings settings);

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link Path} {@code path}. An
   * existing file is replaced.
   *
   * <p>To write the defaults only when the file doesn't exist yet and load it otherwise, use {@link
   * #loadOrDump(Object, Path)}.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param path the file path you want to dump the annotation config to
   */
  void dump(Object annotatedConfig, Path path);

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link Path path}, using the
   * specified {@link Settings settings}. An existing file is replaced.
   *
   * <p>To write the defaults only when the file doesn't exist yet and load it otherwise, use {@link
   * #loadOrDump(Object, Path, Settings)}.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config to dump
   * @param path the file path to dump to
   * @param settings the settings to use
   * @throws IllegalArgumentException if the provided settings are immutable
   * @since 3.1.0
   */
  void dump(Object annotatedConfig, Path path, Settings settings);

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link OutputStream} {@code os},
   * written as UTF-8.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param os the output stream to dump the annotated config to
   */
  default void dump(Object annotatedConfig, OutputStream os) {
    dump(annotatedConfig, new OutputStreamWriter(os, StandardCharsets.UTF_8));
  }

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link OutputStream output
   * stream}, written as UTF-8, using the specified {@link Settings settings}.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config to dump
   * @param os the output stream to dump to
   * @param settings the settings to use
   * @throws IllegalArgumentException if the provided settings are immutable
   * @since 3.1.0
   */
  default void dump(Object annotatedConfig, OutputStream os, Settings settings) {
    this.dump(annotatedConfig, new OutputStreamWriter(os, StandardCharsets.UTF_8), settings);
  }

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link WritableObject writable
   * object}.
   *
   * @param annotatedConfig the annotated config to dump
   * @param writableObject the writable object to dump to
   * @since 3.1.0
   */
  default void dump(Object annotatedConfig, WritableObject writableObject) {
    this.dump(annotatedConfig, writableObject.writer());
  }

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link WritableObject writable
   * object} using the specified {@link Settings settings}.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config to dump
   * @param writableObject the writable object to dump to
   * @param settings the settings to use
   * @throws IllegalArgumentException if the provided settings are immutable
   * @since 3.1.0
   */
  default void dump(Object annotatedConfig, WritableObject writableObject, Settings settings) {
    this.dump(annotatedConfig, writableObject.writer(), settings);
  }

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link Writer} {@code writer}.
   *
   * @param annotatedConfig the annotated config you want to dump
   * @param writer the writer to dump the annotated config to
   */
  void dump(Object annotatedConfig, Writer writer);

  /**
   * Dumps the specified {@code annotatedConfig} to the specified {@link Writer writer}, using the
   * provided {@link Settings settings}.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the config to dump
   * @param writer the writer to dump to
   * @param settings the settings to use
   * @throws IllegalArgumentException if the provided settings are immutable
   * @since 3.1.0
   */
  void dump(Object annotatedConfig, Writer writer, Settings settings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link File} {@code file} using
   * the default {@link Settings} from the builder of this config resolver, or {@link
   * ACDefaultSettings#getDefault()}.
   *
   * <p>To write the defaults when the file doesn't exist yet, use {@link #loadOrDump(Object,
   * File)}.
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
   * <p>To write the defaults when the file doesn't exist yet, use {@link #loadOrDump(Object,
   * Path)}.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param path the file path you want to load
   */
  void load(Object annotatedConfig, Path path);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link File} {@code file} using
   * the {@link Settings} {@code settings} specified.
   *
   * <p>To write the defaults when the file doesn't exist yet, use {@link #loadOrDump(Object, File,
   * Settings)}.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param file the file you want to load
   * @param settings the load settings
   * @throws IllegalArgumentException if the provided settings are immutable
   */
  void load(Object annotatedConfig, File file, Settings settings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Path} {@code path} using
   * the {@link Settings} {@code settings} specified.
   *
   * <p>To write the defaults when the file doesn't exist yet, use {@link #loadOrDump(Object, Path,
   * Settings)}.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param path the file path you want to load
   * @param settings the load settings
   * @throws IllegalArgumentException if the provided settings are immutable
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
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param values the values you want to load
   * @param settings the load settings
   * @throws IllegalArgumentException if the provided settings are immutable
   */
  void load(Object annotatedConfig, Map<String, Object> values, Settings settings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link InputStream} {@code in},
   * read as UTF-8, using the default {@link Settings} from the builder of this config resolver, or
   * {@link ACDefaultSettings#getDefault()}.
   *
   * <p>If you have a {@link File} or {@link Path}, consider using {@link #load(Object, File)} or
   * {@link #load(Object, Path)}. This way AnnotationConfig can add missing options to the file if
   * the load settings allow it.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param in the input stream you want to load
   */
  default void load(Object annotatedConfig, InputStream in) {
    load(annotatedConfig, new InputStreamReader(in, StandardCharsets.UTF_8));
  }

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link InputStream} {@code in}
   * using the {@link Settings} {@code settings} specified.
   *
   * <p>The stream is read as UTF-8. If you have a {@link File} or {@link Path}, consider using
   * {@link #load(Object, File, Settings)} or {@link #load(Object, Path, Settings)}. This way
   * AnnotationConfig can add missing options to the file if the load settings allow it.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param in the input stream you want to load
   * @param settings the load settings
   * @throws IllegalArgumentException if the provided settings are immutable
   */
  default void load(Object annotatedConfig, InputStream in, Settings settings) {
    load(annotatedConfig, new InputStreamReader(in, StandardCharsets.UTF_8), settings);
  }

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Reader} {@code reader}
   * using the default {@link Settings} from the builder of this config resolver, or {@link
   * ACDefaultSettings#getDefault()}.
   *
   * <p>If you have a {@link File} or {@link Path}, consider using {@link #load(Object, File)} or
   * {@link #load(Object, Path)}. This way AnnotationConfig can add missing options to the file if
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
   * <p>If you have a {@link File} or {@link Path}, consider using {@link #load(Object, File,
   * Settings)} or {@link #load(Object, Path, Settings)}. This way AnnotationConfig can add missing
   * options to the file if the load settings allow it.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config you want to load to
   * @param reader the reader you want to load
   * @param settings the load settings
   * @throws IllegalArgumentException if the provided settings are immutable
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
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config you want to load/dump
   * @param file the file you want to load/dump to
   * @param settings the load settings
   * @throws IllegalArgumentException if the provided settings are immutable
   */
  void loadOrDump(Object annotatedConfig, File file, Settings settings);

  /**
   * Loads the specified {@code annotatedConfig} from the specified {@link Path} {@code path}, if it
   * exists, if not, dumps the specified {@code annotatedConfig} to the specified file, using the
   * {@link Settings} {@code settings} specified.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param annotatedConfig the annotated config you want to load/dump
   * @param path the file path you want to load/dump to
   * @param settings the load settings
   * @throws IllegalArgumentException if the provided settings are immutable
   */
  void loadOrDump(Object annotatedConfig, Path path, Settings settings);

  /**
   * Loads the configurations in the {@link File} {@code dir} specified. If it holds none, the
   * defaults are dumped to the specified {@link WritableObject} {@code dumpFile}. <br>
   * <b>The specified {@link File} {@code dir} has to be a directory.</b> It is created if it does
   * not exist.
   *
   * @param dir the directory to load configurations from
   * @param configToResolveTo a {@link Supplier} of the needed configuration objects
   * @param dumpFile the default dump file
   * @param <T> configuration type needed
   * @return a map of the loaded configurations, key being the file name. If the map is empty then
   *     AnnotationConfig couldn't find any configurations in the folder, and you should fall back
   *     to the defaults.
   * @throws IllegalArgumentException if the specified {@code dir} exists and is not a directory
   */
  <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile);

  /**
   * Loads the configurations in the {@link File} {@code dir} specified. If it holds none, the
   * defaults are dumped to the specified {@link WritableObject} {@code dumpFile}. <br>
   * <b>The specified {@link File} {@code dir} has to be a directory.</b> It is created if it does
   * not exist.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param dir the directory to load configurations from
   * @param configToResolveTo a {@link Supplier} of the needed configuration objects
   * @param dumpFile the default dump file
   * @param settings the load settings to use when loading the configurations
   * @return a map of the loaded configurations, key being the file name. If the map is empty then
   *     AnnotationConfig couldn't find any configurations in the folder, and you should fall back
   *     to the defaults.
   * @throws IllegalArgumentException if the specified {@code dir} exists and is not a directory
   * @throws IllegalArgumentException if the provided settings are immutable
   * @param <T> configuration type needed
   */
  <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile, Settings settings);

  /**
   * Loads the configurations in the {@link Path} {@code dir} specified. If it holds none, the
   * defaults are dumped to the specified {@link WritableObject} {@code dumpFile}. <br>
   * <b>The specified {@link Path} {@code dir} has to be a directory.</b> It is created if it does
   * not exist.
   *
   * @param dir the directory to load configurations from
   * @param configToResolveTo a {@link Supplier} of the needed configuration objects
   * @param dumpFile the default dump file
   * @param <T> configuration type needed
   * @return a map of the loaded configurations, key being the file name. If the map is empty then
   *     AnnotationConfig couldn't find any configurations in the folder, and you should fall back
   *     to the defaults.
   * @throws IllegalArgumentException if the specified {@code dir} exists and is not a directory
   */
  <T> Map<String, T> resolveMultiple(
      Path dir, Supplier<T> configToResolveTo, WritableObject dumpFile);

  /**
   * Loads the configurations in the {@link Path} {@code dir} specified. If it holds none, the
   * defaults are dumped to the specified {@link WritableObject} {@code dumpFile}. <br>
   * <b>The specified {@link Path} {@code dir} has to be a directory.</b> It is created if it does
   * not exist.
   *
   * <p>The provided settings must be <i>mutable</i> as AnnotationConfig will fill them with
   * defaults if they are missing.
   *
   * @param dir the directory to load configurations from
   * @param configToResolveTo a {@link Supplier} of the needed configuration objects
   * @param dumpFile the default dump file
   * @param settings the load settings to use when loading the configurations
   * @return a map of the loaded configurations, key being the file name. If the map is empty then
   *     AnnotationConfig couldn't find any configurations in the folder, and you should fall back
   *     to the defaults.
   * @throws IllegalArgumentException if the specified {@code dir} exists and is not a directory
   * @throws IllegalArgumentException if the provided settings are immutable
   * @param <T> configuration type needed
   */
  <T> Map<String, T> resolveMultiple(
      Path dir, Supplier<T> configToResolveTo, WritableObject dumpFile, Settings settings);

  /**
   * Represents a builder of a {@link ConfigResolver}.
   *
   * @since 2.0.0
   * @author Ivan Pekov
   */
  class Builder {

    @Nullable private String commentPrefix;
    @Nullable private ValueWriter valueWriter;
    @Nullable private ValueReader valueReader;
    @Nullable private Settings settings;
    @Nullable private List<String> fileExtensions;

    public Builder() {}

    /**
     * Creates a copy of the specified builder.
     *
     * @param copy the builder you want to copy
     */
    public Builder(ConfigResolver.Builder copy) {
      this.commentPrefix = copy.commentPrefix;
      this.valueWriter = copy.valueWriter;
      this.valueReader = copy.valueReader;
      this.settings = copy.settings == null ? null : copy.settings.copy(false);
      this.fileExtensions =
          copy.fileExtensions == null ? new ArrayList<>() : new ArrayList<>(copy.fileExtensions);
    }

    /**
     * Creates a copy of this builder.
     *
     * @return a new builder instance, copy of this builder instance
     */
    public Builder copy() {
      return new Builder(this);
    }

    /**
     * Sets the comment prefix for the config type you want to generate configs. Formats without
     * comments, such as JSON, set none.
     *
     * @param val the comment prefix you want to set
     * @return this instance for chaining
     */
    public Builder withCommentPrefix(@Nullable String val) {
      commentPrefix = val;
      return this;
    }

    /**
     * Sets a {@link ValueWriter} for the config type you want to generate configs. This cannot be
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
     * Binds the specified {@code value} to the specified {@link Setting} {@code setting} in the
     * default {@link Settings}.
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
     * Sets the {@link KeyResolver} for the config type you want to generate configs. Without one,
     * {@link KeyResolver#DEFAULT} is used.
     *
     * @param resolver the resolver you want to set
     * @return this instance for chaining
     * @deprecated use {@link #withSetting(Setting, Object)} with the {@link
     *     ACDefaultSettings#KEY_RESOLVER} setting.
     */
    @Deprecated(since = "3.1.0", forRemoval = true)
    public Builder withKeyResolver(KeyResolver resolver) {
      return withSetting(ACDefaultSettings.KEY_RESOLVER, resolver);
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
     * @deprecated use {@link #withSetting(Setting, Object)} with the {@link
     *     ACDefaultSettings#SHOULD_REVERSE_FIELDS} setting.
     */
    @Deprecated(since = "3.0.2", forRemoval = true)
    public Builder shouldReverseFields(boolean val) {
      return withSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, val);
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
      //noinspection DataFlowIssue
      return new ConfigResolverImpl(
          commentPrefix, valueWriter, valueReader, settings, fileExtensions);
    }
  }
}
