package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSettings;
import java.io.File;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * Represents configuration, utilising YAML
 *
 * @since 1.0
 * @author MrIvanPlays
 */
public final class YamlConfig {

  private static final Yaml YAML = new Yaml();
  private static final ValueWriter VALUE_WRITER = new YamlValueWriter();

  private static ConfigResolver configResolver;

  /**
   * Returns the {@link ConfigResolver} instance of YamlConfig
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
            .withValueWriter(VALUE_WRITER)
            .shouldReverseFields(true)
            .withCommentPrefix("# ")
            .withValueReader(
                new ValueReader() {
                  @Override
                  public Map<String, Object> read(Reader reader) {
                    Map<String, Object> values = YAML.loadAs(reader, LinkedHashMap.class);
                    if (values == null) {
                      return Collections.emptyMap();
                    }
                    return values;
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

  private static final class YamlValueWriter implements ValueWriter {

    @Override
    public void write(
        String key,
        Object value,
        PrintWriter writer,
        CustomOptions options,
        boolean sectionExists) {
      write(key, value, writer, 2, sectionExists);
    }

    private void write(
        String key, Object value, PrintWriter writer, int childIndents, boolean sectionExists) {
      StringBuilder intentPrefixBuilder = new StringBuilder();
      for (int i = 0; i < childIndents; i++) {
        intentPrefixBuilder.append(" ");
      }
      String intentPrefix = intentPrefixBuilder.toString();
      if (value instanceof Map<?, ?>) {
        if (!sectionExists) {
          writer.println(key + ":");
        }
        for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
          String mapKey = entry.getKey();
          Object v = entry.getValue();
          if (v instanceof List<?>) {
            writer.println(intentPrefix + mapKey + ":");
            for (Object b : (List<?>) v) {
              if (!(b instanceof String)) {
                writer.println(intentPrefix + "    - " + b);
              } else {
                writer.println(intentPrefix + "    - \"" + b + "\"");
              }
            }
          } else if (v instanceof Map<?, ?>) {
            write(mapKey, v, writer, childIndents + 2, sectionExists);
          } else {
            if (!(v instanceof String)) {
              writer.println(intentPrefix + mapKey + ": " + v);
            } else {
              writer.println(intentPrefix + mapKey + ": \"" + v + "\"");
            }
          }
        }
      } else if (value instanceof List<?>) {
        writer.println(key + ":");
        for (Object b : (List<?>) value) {
          if (!(b instanceof String)) {
            writer.println(intentPrefix + "- " + b);
          } else {
            writer.println(intentPrefix + "- \"" + b + "\"");
          }
        }
      } else {
        if (!(value instanceof String)) {
          writer.println(key + ": " + value);
        } else {
          writer.println(key + ": \"" + value + "\"");
        }
      }
      writer.append('\n');
    }
  }
}
