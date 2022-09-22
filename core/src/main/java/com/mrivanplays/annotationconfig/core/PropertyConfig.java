package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSettings;
import com.mrivanplays.annotationconfig.core.utils.ReflectionUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Represents configuration, utilising .conf/.properties configuration type.
 *
 * @since 1.0
 * @author MrIvanPlays
 */
public final class PropertyConfig {

  private static ConfigResolver configResolver;

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

  private static final ValueWriter PROPERTIES_VALUE_WRITER = new PropertyValueWriter();

  private static void generateConfigResolver() {
    configResolver =
        ConfigResolver.newBuilder()
            .withCommentPrefix("# ")
            .withFileExtension(".properties")
            .withValueWriter(PROPERTIES_VALUE_WRITER)
            .withValueReader(
                new ValueReader() {
                  @Override
                  public Map<String, Object> read(Reader reader) throws IOException {
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
                  }
                })
            .build();
  }

  private static final class PropertyValueWriter implements ValueWriter {

    @Override
    public void write(
        Map<String, Object> values,
        Map<String, List<String>> fieldComments,
        PrintWriter writer,
        CustomOptions options) {
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
}
