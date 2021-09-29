package com.mrivanplays.annotationconfig.core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
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

  private static final ValueWriter VALUE_WRITER = new PropertyValueWriter();

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

  private static void generateConfigResolver() {
    configResolver =
        ConfigResolver.newBuilder()
            .withCommentPrefix("# ")
            .withValueWriter(VALUE_WRITER)
            .withValueReader(
                file -> {
                  Properties properties = new Properties();
                  try (Reader reader = new FileReader(file)) {
                    properties.load(reader);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                  Map<String, Object> ret = new LinkedHashMap<>();
                  for (Object key : properties.keySet()) {
                    ret.put(String.valueOf(key), properties.get(key));
                  }
                  return ret;
                })
            .build();
  }

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   * @deprecated use {@link #getConfigResolver()}. it has a much better description of methods. the
   *     equivalent of this method there is {@link ConfigResolver#loadOrDump(Object, File, boolean)}
   */
  @Deprecated
  public static void load(Object annotatedConfig, File file) {
    getConfigResolver().loadOrDump(annotatedConfig, file, true);
  }

  private static final class PropertyValueWriter implements ValueWriter {

    @Override
    public void write(String key, Object value, PrintWriter writer, boolean sectionExists) {
      if (value instanceof Map<?, ?>) {
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
          if (entry.getKey() instanceof String && !(entry.getValue() instanceof Map)) {
            writer.println(key + "=" + value);
          } else if (entry.getValue() instanceof Map) {
            write(key, entry.getValue(), writer, sectionExists);
          }
        }
        return;
      }
      if (value instanceof List<?>) {
        throw new IllegalArgumentException(".properties does not support lists.");
      }
      writer.println(key + "=" + value);
      writer.append('\n');
    }
  }
}
