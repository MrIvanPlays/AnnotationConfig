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
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Setting;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents configuration, utilising JSON
 *
 * @since 3.0.4
 * @author Ivan Pekov
 */
@SuppressWarnings("unchecked")
@NullMarked
public final class JsonConfig {

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().setStrictness(Strictness.LENIENT).create();

  /**
   * Returns the {@link Setting} with which the gson instance is referenced in the {@link Settings}
   */
  public static final Setting<Gson> GSON_INSTANCE = Setting.of("gson", Gson.class);

  @Nullable private static ConfigResolver configResolver;

  /**
   * Returns the {@link ConfigResolver} instance of JsonConfig
   *
   * @return config resolver
   */
  public static ConfigResolver getConfigResolver() {
    if (configResolver == null) {
      generateConfigResolver();
    }
    return configResolver;
  }

  // <editor-fold desc="Internal Implementation" defaultstate="collapsed">
  private static void generateConfigResolver() {
    configResolver =
        ConfigResolver.newBuilder()
            .withValueWriter((values, fieldComments, writer, settings) ->
                writer.print(settings.get(GSON_INSTANCE).orElse(GSON).toJson(values)))
            .withSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, true)
            .withFileExtension(".json")
            .withValueReader((reader, settings) ->
                Objects.requireNonNullElse(
                    settings.get(GSON_INSTANCE).orElse(GSON).fromJson(reader, LinkedHashMap.class),
                    Collections.emptyMap()))
            .build();
  }
  // </editor-fold>
}
