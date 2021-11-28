package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSettings;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
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

  private static void generateConfigResolver() {
    configResolver =
        ConfigResolver.newBuilder()
            .withCommentPrefix("# ")
            .withValueWriter(PropertyValueWriter::new)
            .withValueReader(
                new ValueReader() {
                  @Override
                  public Map<String, Object> read(Reader reader) throws IOException {
                    Properties properties = new Properties();
                    properties.load(reader);
                    Map<String, Object> ret = new LinkedHashMap<>();
                    for (Object key : properties.keySet()) {
                      ret.put(String.valueOf(key), properties.get(key));
                    }
                    return ret;
                  }
                })
            .build();
  }

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   * @deprecated use {@link #getConfigResolver()}. it has a much better description of methods. the
   *     equivalent of this method there is {@link ConfigResolver#loadOrDump(Object, File,
   *     LoadSettings)}
   */
  @Deprecated
  public static void load(Object annotatedConfig, File file) {
    getConfigResolver().loadOrDump(annotatedConfig, file);
  }

  private static final class PropertyValueWriter implements ValueWriter {

    private Map<String, Object> toWrite = new HashMap<>();
    private Map<String, List<String>> toWriteComments = new HashMap<>();

    @Override
    public void handleMapPart(
        String key,
        Map<String, Object> value,
        CustomOptions options,
        Map<String, List<String>> comments) {
      // this is .properties what do you expect
      throw new IllegalArgumentException(".properties does not support maps.");
    }

    @Override
    public void handlePart(String key, Object value, CustomOptions options, List<String> comments) {
      if (value instanceof Map<?, ?>) {
        throw new IllegalArgumentException(".properties does not support maps.");
      }
      if (value instanceof List<?>) {
        throw new IllegalArgumentException(".properties does not support lists.");
      }
      if (!toWrite.containsKey(key)) {
        toWrite.put(key, value);
      } else {
        throw new IllegalArgumentException("Duplicate key found!");
      }
      if (!comments.isEmpty()) {
        toWriteComments.put(key, comments);
      }
    }

    @Override
    public void writeAndFlush(CustomOptions options, PrintWriter writer) {
      try {
        int index = 0;
        for (Map.Entry<String, Object> entry : toWrite.entrySet()) {
          if (toWriteComments.containsKey(entry.getKey())) {
            for (String comment : toWriteComments.get(entry.getKey())) {
              writer.println("# " + comment);
            }
          }
          writer.println(entry.getKey() + "=" + entry.getValue());
          if ((index + 1) != toWrite.size()) {
            writer.append('\n');
          }
          index++;
        }
      } finally {
        toWrite.clear();
        toWriteComments.clear();
      }
    }
  }
}
