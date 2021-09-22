package com.mrivanplays.annotationconfig.toml;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.mrivanplays.annotationconfig.core.AnnotationType;
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

  private static final TomlWriter DEFAULT_TOML_WRITER = new TomlWriter();

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
    Map<AnnotationHolder, List<AnnotationType>> map =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, true);
    AnnotatedConfigResolver.ValueWriter valueWriter = new TomlValueWriter(tomlWriter);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(annotatedConfig, map, file, "# ", valueWriter, true);
      return;
    }

    Toml toml = new Toml().read(file);
    AnnotatedConfigResolver.setFields(
        annotatedConfig, toml.toMap(), map, "# ", valueWriter, file, false, true, false, null);
  }

  private static final class TomlValueWriter implements AnnotatedConfigResolver.ValueWriter {

    private final TomlWriter tomlWriter;

    TomlValueWriter(TomlWriter tomlWriter) {
      this.tomlWriter = tomlWriter;
    }

    @Override
    public void write(String key, Object value, PrintWriter writer, boolean sectionExists)
        throws IOException {
      tomlWriter.write(Collections.singletonMap(key, value), writer);
      writer.append('\n');
    }
  }
}
