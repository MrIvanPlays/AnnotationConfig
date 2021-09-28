package com.mrivanplays.annotationconfig.toml;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.mrivanplays.annotationconfig.core.ValueWriter;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.internal.AnnotatedConfigResolver;
import com.mrivanplays.annotationconfig.core.internal.AnnotationHolder;
import com.mrivanplays.annotationconfig.core.serialization.registry.SerializerRegistry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Represents configuration, utilising TOML.
 *
 * @since 1.0
 * @author MrIvanPlays
 */
public final class TomlConfig {

  private static final TomlWriter DEFAULT_TOML_WRITER = new TomlWriter();

  static {
    SerializerRegistry registry = SerializerRegistry.INSTANCE;
    if (!registry.hasSerializer(Date.class)) {
      registry.registerSerializer(Date.class, new DateResolver());
    }
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
    load(annotatedConfig, file, tomlWriter, false);
  }

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   * @param tomlWriter toml writer
   * @param generateNewOptions whether to generate new options
   */
  public static void load(
      Object annotatedConfig, File file, TomlWriter tomlWriter, boolean generateNewOptions) {
    Map<AnnotationHolder, Set<AnnotationType>> map =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, true);
    ValueWriter valueWriter = new TomlValueWriter(tomlWriter);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(annotatedConfig, map, file, "# ", valueWriter, true);
      return;
    }

    Toml toml = new Toml().read(file);
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        toml.toMap(),
        map,
        "# ",
        valueWriter,
        file,
        generateNewOptions,
        true,
        false,
        null);
  }

  /**
   * Represents the default toml value writer
   *
   * <p>since: 1.0 ; but private -> public in 2.0.0
   *
   * @author MrIvanPlays
   */
  public static final class TomlValueWriter implements ValueWriter {

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
