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

package com.mrivanplays.annotationconfig.core.resolver.settings;

import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a holder of the default {@link Settings} for AnnotationConfig.
 *
 * @since 3.0.0
 * @author Ivan Pekov
 */
@NullMarked
public final class ACDefaultSettings {

  /**
   * Whether the fields of a config class are written in declaration order. Without it they are
   * written in reverse declaration order, as {@code .properties} does; YAML, JSON and TOML set it.
   * Defaults to {@code false}.
   */
  public static final Setting<Boolean> SHOULD_REVERSE_FIELDS =
      Setting.of("should_reverse_fields", Boolean.class);

  /**
   * How a value that is read as {@code null} is handled: whether the field is set to {@code null}
   * or keeps its default value. Defaults to {@link NullReadHandleOption#SET_NULL}.
   */
  public static final Setting<NullReadHandleOption> NULL_READ_HANDLER =
      Setting.of("null_read_handler", NullReadHandleOption.class);

  /**
   * Whether loading a file that misses options of the config writes the file again with them.
   * Defaults to {@code true}.
   */
  public static final Setting<Boolean> GENERATE_NEW_OPTIONS =
      Setting.of("generate_new_options", Boolean.class);

  /**
   * The {@link KeyResolver} that puts the values of the options into the written map and gets them
   * out of the read one. Defaults to {@link KeyResolver#DEFAULT}.
   *
   * @since 3.1.0
   */
  public static final Setting<KeyResolver> KEY_RESOLVER =
      Setting.of("key_resolver", KeyResolver.class);

  /**
   * A setting indicating whether to find {@link java.lang.reflect.Field fields} of classes which
   * have been inherited by the given config class. Defaults to {@code false}.
   *
   * <p>Consider this example:
   *
   * <pre>{@code
   * public class BaseConfig {
   *   int foo = 1;
   * }
   *
   * public class SpecificConfig extends BaseConfig {
   *   String bar = "baz";
   * }
   * }</pre>
   *
   * <p>This setting indicates whether the field {@code foo} will also be an option in the dumped
   * config or not.
   */
  public static final Setting<Boolean> FIND_PARENT_FIELDS =
      Setting.of("find_parent_fields", Boolean.class);

  /**
   * A setting indicating whether {@code static} {@link java.lang.reflect.Field fields} of the
   * config class (and, with {@link #FIND_PARENT_FIELDS}, of its parents) are options of the config.
   * Defaults to {@code false}.
   *
   * <p>Consider this example:
   *
   * <pre>{@code
   * public class Config {
   *   static int counter = 0;
   *   String foo = "bar";
   * }
   * }</pre>
   *
   * <p>With this setting off only {@code foo} is dumped and loaded. With it on, {@code counter} is
   * dumped too, and loading sets it, which changes it for every instance of the class.
   *
   * @since 3.1.0
   */
  public static final Setting<Boolean> FIND_STATIC_FIELDS =
      Setting.of("find_static_fields", Boolean.class);

  /**
   * Whether loading reads every option before it reports the options that could not be read. With
   * it on, a load that finds a value it can't read (out of the bounds of {@code @Min} or {@code
   * Max}, not one of {@code @OneOf}, an unknown enum constant, a number that isn't one, ...) reads
   * the other options and then throws one {@link
   * com.mrivanplays.annotationconfig.core.resolver.ConfigLoadException} that lists them all. With
   * it off, loading stops at the first one. Defaults to {@code false}.
   *
   * <p>With it on, every option that could be read gets its value and the ones that failed keep
   * theirs; with it off, so do the options read before the failure, and the ones after it are not
   * read. Either way a file that misses options is not written again.
   *
   * @since 3.1.1
   */
  public static final Setting<Boolean> REPORT_ALL_ERRORS =
      Setting.of("report_all_errors", Boolean.class);

  /**
   * The JSON Schema a dumped file references, so that an editor completes and checks the file
   * with it: a path, relative to the file, or a URL, such as {@code config.schema.json}. The
   * schema itself comes from {@link
   * com.mrivanplays.annotationconfig.core.resolver.ConfigResolver#schema(Object)}.
   *
   * <p>Not set by default: a dump references no schema. When it is set, each format references it
   * the way its editors read it (see {@link
   * com.mrivanplays.annotationconfig.core.resolver.ValueWriter#schemaComment(String)}):
   *
   * <ul>
   *   <li>JSON and JSON5: a {@code "$schema"} key, the first key of the file
   *   <li>YAML: a {@code # yaml-language-server: $schema=...} comment, the first line
   *   <li>TOML: a {@code #:schema ...} comment, the first line
   *   <li>{@code .properties}: nothing, as no editor checks it against a schema
   * </ul>
   *
   * <p>Loading ignores the reference. It must not hold a line break.
   *
   * @since 3.1.1
   */
  public static final Setting<String> SCHEMA_REFERENCE =
      Setting.of("schema_reference", String.class);

  private ACDefaultSettings() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * Get the default {@link Settings settings}.
   *
   * @return defaults
   */
  public static Settings getDefault() {
    return Holder.SETTINGS;
  }

  // <editor-fold desc="Holder impl" defaultstate="collapsed">
  private static Settings generateSettings() {
    return new Settings()
        .put(SHOULD_REVERSE_FIELDS, false)
        .put(NULL_READ_HANDLER, NullReadHandleOption.SET_NULL)
        .put(GENERATE_NEW_OPTIONS, true)
        .put(FIND_PARENT_FIELDS, false)
        .put(FIND_STATIC_FIELDS, false)
        .put(REPORT_ALL_ERRORS, false)
        .put(KEY_RESOLVER, KeyResolver.DEFAULT)
        .copy(true);
  }

  private static final class Holder {
    static final Settings SETTINGS = generateSettings();
  }
  // </editor-fold>
}
