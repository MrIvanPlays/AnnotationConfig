package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
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

  private static final ValueWriter VALUE_WRITER = new PropertyValueWriter();

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   */
  public static void load(Object annotatedConfig, File file) {
    Map<AnnotationHolder, List<AnnotationType>> map =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, false);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(annotatedConfig, map, file, "# ", VALUE_WRITER, false);
      return;
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
        annotatedConfig, toMap, map, "# ", VALUE_WRITER, file, true, false, false, null);
  }

  private static final class PropertyValueWriter implements ValueWriter {

    @Override
    public void write(String key, Object value, PrintWriter writer, boolean sectionExists) {
      if (value instanceof Map<?, ?>) {
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
          if (entry.getKey() instanceof String && !(entry.getValue() instanceof Map)) {
            writer.println(key + "=" + value.toString());
          } else if (entry.getValue() instanceof Map) {
            write(key, entry.getValue(), writer, sectionExists);
          }
        }
        return;
      }
      writer.println(key + "=" + value.toString());
      writer.append('\n');
    }
  }
}
