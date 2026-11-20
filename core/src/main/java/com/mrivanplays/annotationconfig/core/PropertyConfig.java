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
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.utils.ReflectionUtils;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents configuration, utilising .conf/.properties configuration type.
 *
 * @since 1.0
 * @author Ivan Pekov
 */
@NullMarked
public final class PropertyConfig {

  @Nullable private static ConfigResolver configResolver;

  /**
   * Returns the {@link ConfigResolver} instance of PropertyConfig
   *
   * @return config resolver
   */
  public static ConfigResolver getConfigResolver() {
    if (configResolver == null) {
      generateConfigResolver();
    }
    return configResolver;
  }

  // <editor-fold desc="Internal implementation" defaultstate="collapsed">
  private static final ValueWriter PROPERTIES_VALUE_WRITER = new PropertyValueWriter();

  private static void generateConfigResolver() {
    configResolver =
        ConfigResolver.newBuilder()
            .withCommentPrefix("# ")
            .withFileExtension(".properties")
            .withValueWriter(PROPERTIES_VALUE_WRITER)
            .withValueReader((reader, settings) -> {
              Properties properties = new Properties();
              properties.load(reader);
              Map<String, Object> ret = new LinkedHashMap<>();
              for (Object key : properties.keySet()) {
                Object value = properties.get(key);
                if (value instanceof String) {
                  String valueString = String.valueOf(value);
                  if (valueString.contains(",")) {
                    String[] stringArray = valueString.split(",");
                    Object[] newArray = new Object[stringArray.length];
                    for (int i = 0; i < stringArray.length; i++) {
                      newArray[i] = stringArray[i].trim();
                    }
                    value = newArray;
                  }
                }
                ret.put(String.valueOf(key), value);
              }
              return ret;
            })
            .build();
  }

  @NullMarked
  private static final class PropertyValueWriter implements ValueWriter {

    @Override
    public void write(
        Map<String, Object> values,
        Map<String, List<String>> fieldComments,
        PrintWriter writer,
        Settings settings) {
      int index = 0;
      for (Map.Entry<String, Object> entry : values.entrySet()) {
        if (entry.getValue() instanceof Map<?, ?>) {
          throw new IllegalArgumentException(".properties does not support maps");
        }
        if (entry.getValue() instanceof List<?>) {
          throw new IllegalArgumentException(".properties does not support lists");
        }
        if (fieldComments.containsKey(entry.getKey())) {
          for (String comment : fieldComments.get(entry.getKey())) {
            writer.println("# " + comment);
          }
        }
        String toWrite;
        if (entry.getValue() instanceof MultilineString) {
          toWrite = ((MultilineString) entry.getValue()).getString();
        } else if (!entry.getValue().getClass().isArray()) {
          toWrite = String.valueOf(entry.getValue());
        } else {
          toWrite =
              Arrays.deepToString(ReflectionUtils.castToArray(entry.getValue()))
                  .replace("[", "")
                  .replace("]", "");
        }
        writer.println(entry.getKey() + "=" + toWrite);
        if ((index + 1) != values.size()) {
          writer.append('\n');
        }
        index++;
      }
    }
  }
  // </editor-fold>
}
