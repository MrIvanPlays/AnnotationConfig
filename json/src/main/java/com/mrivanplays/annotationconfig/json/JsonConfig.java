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

package com.mrivanplays.annotationconfig.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.ToNumberPolicy;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.key.DottedResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Setting;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents configuration, utilising JSON.
 *
 * @since 3.1.0
 * @author Ivan Pekov
 */
@SuppressWarnings("unchecked")
@NullMarked
public final class JsonConfig {

  /**
   * The {@link Setting} with which the {@link Gson} instance is referenced in the {@link Settings}.
   *
   * <p>A configured instance is used for reading and writing. To read numbers exactly, it should be
   * created with {@code setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)}, as the default
   * instance is.
   */
  public static final Setting<Gson> GSON_INSTANCE = Setting.of("gson", Gson.class);

  private static final Gson GSON =
      new GsonBuilder()
          .setPrettyPrinting()
          .setStrictness(Strictness.LENIENT)
          // config files are read by people: write '=', '&', '<' etc. as they are
          .disableHtmlEscaping()
          // the default reads every number as a double, which loses precision past 2^53
          .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
          .create();

  private JsonConfig() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * Returns the {@link ConfigResolver} instance of plain JSON spec configuration.
   *
   * @return config resolver
   */
  public static ConfigResolver getConfigResolver() {
    return StandardHolder.RESOLVER;
  }

  /**
   * Returns the {@link ConfigResolver} instance for <a href="https://spec.json5.org">JSON5</a>
   * configuration files ({@code .json5}), a superset of JSON that supports comments.
   *
   * <p>Configs are written by {@link Json5ValueWriter}: comments as {@code //} lines above their
   * keys, identifier keys without quotes, and {@code Infinity} and {@code NaN} as JSON5 numbers.
   * They are read with Gson in lenient mode, like {@link #getConfigResolver()} reads them, so plain
   * JSON files load as well, and so does hand-written JSON5 with comments, unquoted keys and
   * single-quoted strings. Gson does not read all of JSON5: trailing commas, hexadecimal numbers,
   * numbers with a leading {@code +} or a leading or trailing decimal point, the {@code \x}, {@code
   * \v} and {@code \0} escapes and line continuations in strings are rejected or read differently.
   *
   * <p>{@link #GSON_INSTANCE} is used for reading, as with JSON; writing does not use Gson.
   *
   * @return config resolver
   * @since 3.1.0
   */
  public static ConfigResolver getJson5ConfigResolver() {
    return Json5Holder.RESOLVER;
  }

  // <editor-fold desc="Internal Implementation" defaultstate="collapsed">

  private static ConfigResolver createConfigResolver(boolean json5) {
    ConfigResolver.Builder builder =
        ConfigResolver.newBuilder()
            .withSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, true)
            .withSetting(ACDefaultSettings.KEY_RESOLVER, DottedResolver.getInstance())
            .withValueReader((reader, settings) ->
                Objects.requireNonNullElse(
                    settings.get(GSON_INSTANCE).orElse(GSON).fromJson(reader, LinkedHashMap.class),
                    Collections.emptyMap()));
    if (json5) {
      return builder
          .withValueWriter(new Json5ValueWriter())
          .withFileExtension(".json5")
          .withCommentPrefix("// ")
          .build();
    }
    return builder.withValueWriter(standardValueWriter()).withFileExtension(".json").build();
  }

  private static ValueWriter standardValueWriter() {
    return new ValueWriter() {
      @Override
      public void write(
          Map<String, Object> values,
          Map<String, List<String>> fieldComments,
          PrintWriter writer,
          Settings settings) {
        // one write, as the other writers do; a text file ends with a line break
        writer.print(
            settings.get(GSON_INSTANCE).orElse(GSON).toJson(unwrapMultiline(values)) + '\n');
      }

      // JSON has no comments, so they aren't collected
      @Override
      public boolean writesComments() {
        return false;
      }

      @Override
      public boolean writesSchemaKey() {
        return true;
      }
    };
  }

  /**
   * JSON has no multiline string syntax, so {@code @Multiline} values are written as plain strings.
   * Gson would otherwise write the {@link MultilineString} wrapper as an object.
   */
  private static @Nullable Object unwrapMultiline(@Nullable Object value) {
    if (value instanceof MultilineString multiline) {
      return multiline.getString();
    }
    // a map or list is only copied once a value in it changes, so values without @Multiline
    // strings, which is most of them, are written as they are
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> copy = null;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        Object original = entry.getValue();
        Object unwrapped = unwrapMultiline(original);
        if (unwrapped != original) {
          if (copy == null) {
            copy = new LinkedHashMap<>(map);
          }
          // replacing the value of an existing key keeps its position
          copy.put(entry.getKey(), unwrapped);
        }
      }
      return copy != null ? copy : map;
    }
    if (value instanceof List<?> list) {
      List<Object> copy = null;
      int index = 0;
      for (Object original : list) {
        Object unwrapped = unwrapMultiline(original);
        if (unwrapped != original) {
          if (copy == null) {
            copy = new ArrayList<>(list);
          }
          copy.set(index, unwrapped);
        }
        index++;
      }
      return copy != null ? copy : list;
    }
    return value;
  }

  // Each resolver is built on first use; class initialisation makes that happen exactly once.
  private static final class StandardHolder {

    static final ConfigResolver RESOLVER = createConfigResolver(false);
  }

  private static final class Json5Holder {

    static final ConfigResolver RESOLVER = createConfigResolver(true);
  }
  // </editor-fold>
}
