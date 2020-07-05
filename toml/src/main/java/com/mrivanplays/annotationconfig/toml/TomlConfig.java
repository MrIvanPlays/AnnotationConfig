package com.mrivanplays.annotationconfig.toml;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.mrivanplays.annotationconfig.core.AnnotationType;
import com.mrivanplays.annotationconfig.core.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.internal.AnnotatedConfigResolver;
import com.mrivanplays.annotationconfig.core.internal.AnnotationHolder;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Represents configuration, utilising TOML. */
public final class TomlConfig {

  private static CustomAnnotationRegistry annotationRegistry = new CustomAnnotationRegistry();

  private static final TomlWriter DEFAULT_TOML_WRITER = new TomlWriter();

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
    load(annotatedConfig, file, DEFAULT_TOML_WRITER);
  }

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   * @param tomlWriter toml writer
   */
  public static void load(Object annotatedConfig, File file, TomlWriter tomlWriter) {
    List<Map.Entry<AnnotationHolder, List<AnnotationType>>> map =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, annotationRegistry, true);
    AnnotatedConfigResolver.ValueWriter valueWriter = new TomlValueWriter(tomlWriter);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          map,
          file,
          "# ",
          valueWriter,
          annotationRegistry,
          TomlConfig.class,
          true);
    }

    Toml toml = new Toml().read(file);
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        toml.toMap(),
        map,
        annotationRegistry,
        "# ",
        valueWriter,
        file,
        false,
        true,
        TomlConfig.class,
        false,
        null);
  }

  private static final class TomlValueWriter implements AnnotatedConfigResolver.ValueWriter {

    private TomlWriter tomlWriter;

    TomlValueWriter(TomlWriter tomlWriter) {
      this.tomlWriter = tomlWriter;
    }

    @Override
    public void write(String key, Object value, PrintWriter writer, boolean sectionExists) throws IOException {
      tomlWriter.write(Collections.singletonMap(key, value), writer);
      writer.append('\n');
    }

    @Override
    public void writeCustom(Object value, PrintWriter writer, String annoName) throws IOException {
      if (value instanceof Map) {
        tomlWriter.write(value, writer);
        writer.append('\n');
        return;
      }
      if (value instanceof String && ((String) value).indexOf('=') != -1) {
        String valueString = (String) value;
        String[] arr = valueString.split("=");
        tomlWriter.write(Collections.singletonMap(arr[0], arr[1]), writer);
        writer.append('\n');
        return;
      }
      throw new IllegalArgumentException(
          "Invalid syntax for toml custom write: annotation '"
              + annoName
              + "'; written must be either a map or string in format key=value");
    }
  }
}
