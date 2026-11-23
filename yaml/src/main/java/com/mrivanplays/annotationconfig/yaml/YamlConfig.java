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

package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.key.DottedResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Setting;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import org.jspecify.annotations.NullMarked;
import org.yaml.snakeyaml.Yaml;

/**
 * Represents configuration, utilising YAML
 *
 * @since 1.0
 * @author Ivan Pekov
 */
@NullMarked
public final class YamlConfig {

  /**
   * Returns the {@link Setting} with which the yaml instance is referenced in the {@link Settings}
   */
  public static final Setting<Yaml> YAML_INSTANCE = Setting.of("yaml", Yaml.class);

  /**
   * Returns the {@link ConfigResolver} instance of YamlConfig
   *
   * @return config resolver
   */
  public static ConfigResolver getConfigResolver() {
    return ResolverHolder.RESOLVER;
  }

  // <editor-fold desc="Internal Implementation" defaultstate="collapsed">
  static final YamlHandler HANDLER = new YamlHandler();

  /** Builds the resolver on first use; class initialisation makes that happen exactly once. */
  private static final class ResolverHolder {

    static final ConfigResolver RESOLVER = createConfigResolver();
  }

  private static ConfigResolver createConfigResolver() {
    return ConfigResolver.newBuilder()
        .withValueReader(HANDLER)
        .withValueWriter(HANDLER)
        .withSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, true)
        .withSetting(ACDefaultSettings.KEY_RESOLVER, DottedResolver.getInstance())
        .withCommentPrefix("# ")
        .withFileExtension(".yml")
        .withFileExtension(".yaml")
        .build();
  }
  // </editor-fold>
}
