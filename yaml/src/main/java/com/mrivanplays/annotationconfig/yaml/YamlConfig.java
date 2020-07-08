package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.AnnotationType;
import com.mrivanplays.annotationconfig.core.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.internal.AnnotatedConfigResolver;
import com.mrivanplays.annotationconfig.core.internal.AnnotationHolder;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/** Represents configuration, utilising YAML */
public final class YamlConfig {

  private static CustomAnnotationRegistry annotationRegistry = new CustomAnnotationRegistry();

  /**
   * Returns the {@link CustomAnnotationRegistry} for this config.
   *
   * @return custom annotation registry
   */
  public static CustomAnnotationRegistry getAnnotationRegistry() {
    return annotationRegistry;
  }

  private static final Yaml YAML = new Yaml();
  private static final AnnotatedConfigResolver.ValueWriter VALUE_WRITER = new YamlValueWriter();

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   */
  public static void load(Object annotatedConfig, File file) {
    Map<AnnotationHolder, List<AnnotationType>> map =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, annotationRegistry, true);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          map,
          file,
          "# ",
          VALUE_WRITER,
          annotationRegistry,
          YamlConfig.class,
          true);
    }

    try (Reader reader = new FileReader(file)) {
      Map<String, Object> values = YAML.loadAs(reader, LinkedHashMap.class);
      if (values == null) {
        return;
      }
      AnnotatedConfigResolver.setFields(
          annotatedConfig,
          values,
          map,
          annotationRegistry,
          "# ",
          VALUE_WRITER,
          file,
          true,
          true,
          YamlConfig.class,
          false,
          null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static final class YamlValueWriter implements AnnotatedConfigResolver.ValueWriter {

    @Override
    public void write(String key, Object value, PrintWriter writer, boolean sectionExists) {
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

    @Override
    public void writeCustom(Object value, PrintWriter writer, String annoName) {
      if (!(value instanceof String)
          && !(value instanceof Character)
          && !(value instanceof char[])) {
        throw new IllegalArgumentException(
            "Cannot write other than String, char and char[] for yaml config: annotation '"
                + annoName
                + "'");
      }
      writer.write(String.valueOf(value));
    }
  }
}
