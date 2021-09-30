package com.mrivanplays.annotationconfig.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.dataformat.toml.TomlReadFeature;
import com.mrivanplays.annotationconfig.core.ValueWriter;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.internal.AnnotatedConfigResolver;
import com.mrivanplays.annotationconfig.core.internal.AnnotationHolder;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents configuration, utilising TOML.
 *
 * @since 1.0
 * @author MrIvanPlays
 */
public final class TomlConfig {

  private static final TomlMapper DEFAULT_TOML_MAPPER =
      TomlMapper.builder().configure(TomlReadFeature.PARSE_JAVA_TIME, true).build();

  static {
    SerializerRegistry registry = SerializerRegistry.INSTANCE;
    if (!registry.hasSerializer(Date.class)) {
      registry.registerSerializer(Date.class, new DateResolver());
    }
    if (!registry.hasSerializer(OffsetDateTime.class)) {
      registry.registerSerializer(
          OffsetDateTime.class,
          (data, field) -> OffsetDateTime.parse(data.getAsString()),
          (value, field) -> new DataObject(value.toString()));
    }
    if (!registry.hasSerializer(LocalDateTime.class)) {
      registry.registerSerializer(
          LocalDateTime.class,
          (data, field) -> LocalDateTime.parse(data.getAsString()),
          (value, field) -> new DataObject(value.toString()));
    }
    if (!registry.hasSerializer(LocalDate.class)) {
      registry.registerSerializer(
          LocalDate.class,
          (data, field) -> LocalDate.parse(data.getAsString()),
          (value, field) -> new DataObject(value.toString()));
    }
    if (!registry.hasSerializer(LocalTime.class)) {
      registry.registerSerializer(
          LocalTime.class,
          (data, field) -> LocalTime.parse(data.getAsString()),
          (value, field) -> new DataObject(value.toString()));
    }
  }

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   */
  public static void load(Object annotatedConfig, File file) {
    load(annotatedConfig, file, DEFAULT_TOML_MAPPER);
  }

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   * @param tomlMapper toml mapper
   */
  public static void load(Object annotatedConfig, File file, TomlMapper tomlMapper) {
    load(annotatedConfig, file, tomlMapper, false);
  }

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   * @param tomlMapper toml mapper
   * @param generateNewOptions whether to generate new options
   */
  public static void load(
      Object annotatedConfig, File file, TomlMapper tomlMapper, boolean generateNewOptions) {
    Map<AnnotationHolder, Set<AnnotationType>> map =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, true);
    ValueWriter valueWriter = new TomlValueWriter(tomlMapper);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(annotatedConfig, map, file, "# ", valueWriter, true);
      return;
    }

    Map<String, Object> values;
    try {
      values = tomlMapper.reader().readValue(file, LinkedHashMap.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
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

    private final TomlMapper tomlMapper;

    TomlValueWriter(TomlMapper tomlMapper) {
      this.tomlMapper = tomlMapper;
    }

    @Override
    public void write(String key, Object value, PrintWriter writer, boolean sectionExists)
        throws IOException {
      writer.println(tomlMapper.writeValueAsString(Collections.singletonMap(key, value)));
    }
  }
}
