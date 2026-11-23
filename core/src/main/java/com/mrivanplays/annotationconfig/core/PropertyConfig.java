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

package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;

/**
 * Represents configuration, utilising .conf/.properties configuration type.
 *
 * @since 1.0
 * @author Ivan Pekov
 */
@NullMarked
public final class PropertyConfig {

  /**
   * Returns the {@link ConfigResolver} instance of PropertyConfig
   *
   * @return config resolver
   */
  public static ConfigResolver getConfigResolver() {
    return ResolverHolder.RESOLVER;
  }

  // <editor-fold desc="Internal implementation" defaultstate="collapsed">

  /** Builds the resolver on first use; class initialization makes that happen exactly once. */
  private static final class ResolverHolder {

    static final ConfigResolver RESOLVER = createConfigResolver();
  }

  private static ConfigResolver createConfigResolver() {
    return ConfigResolver.newBuilder()
        .withCommentPrefix("# ")
        .withFileExtension(".properties")
        .withValueReader((reader, _) -> {
          Properties properties = new Properties();
          properties.load(reader);
          Map<String, Object> ret = new HashMap<>();
          for (String key : properties.stringPropertyNames()) {
            ret.put(key, parseValue(properties.getProperty(key)));
          }
          return ret;
        })
        .withValueWriter((values, fieldComments, writer, _) ->
            PropertiesDumper.dump(writer, values, fieldComments, true))
        .build();
  }

  private static Object parseValue(String s) {
    if (s.indexOf(',') < 0) {
      return s;
    }
    String[] parts = s.split(",", -1); // -1 keeps trailing empty elements
    Object[] result = new Object[parts.length];
    for (int i = 0; i < parts.length; i++) {
      result[i] = parts[i].trim();
    }
    return result;
  }
  // </editor-fold>
}
