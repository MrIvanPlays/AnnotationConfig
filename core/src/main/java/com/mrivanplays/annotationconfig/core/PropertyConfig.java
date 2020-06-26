package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.internal.AnnotatedConfigResolver;
import com.mrivanplays.annotationconfig.core.internal.AnnotationHolder;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Represents configuration, utilising .conf/.properties configuration type. */
public final class PropertyConfig {

  private static CustomAnnotationRegistry annotationRegistry = new CustomAnnotationRegistry();

  private static final AnnotatedConfigResolver.ValueWriter VALUE_WRITER = new PropertyValueWriter();

  /**
   * Returns the {@link CustomAnnotationRegistry} for this config.
   *
   * @return custom annotation registry
   */
  public static CustomAnnotationRegistry getAnnotationRegistry() {
    return annotationRegistry;
  }

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   */
  public static void load(Object annotatedConfig, File file) {
    List<Map.Entry<AnnotationHolder, List<AnnotationType>>> map =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, annotationRegistry, false);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          map,
          file,
          "# ",
          VALUE_WRITER,
          annotationRegistry,
          PropertyConfig.class,
          false);
    }
    Properties properties = new Properties();
    try (Reader reader = new FileReader(file)) {
      properties.load(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Map<String, Object> toMap = new HashMap<>();
    for (Object key : properties.keySet()) {
      toMap.put(String.valueOf(key), properties.get(key));
    }
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        toMap,
        map,
        annotationRegistry,
        "# ",
        VALUE_WRITER,
        file,
        true,
        false,
        PropertyConfig.class);
  }

  private static final class PropertyValueWriter implements AnnotatedConfigResolver.ValueWriter {

    @Override
    public void write(String key, Object value, PrintWriter writer) {
      if (value instanceof Map<?, ?>) {
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
          if (entry.getKey() instanceof String) {
            write(key, entry.getValue(), writer);
          }
        }
      }
      writer.println(key + "=" + value.toString());
      writer.append('\n');
    }

    @Override
    public void writeCustom(Object value, PrintWriter writer, String annoName) {
      if (!(value instanceof String)
          && !(value instanceof Character)
          && !(value instanceof char[])) {
        throw new IllegalArgumentException(
            "Cannot write other than String, char and char[] for .properties/.conf config: annotation '"
                + annoName
                + "'");
      }
      writer.write(String.valueOf(value));
    }
  }
}
