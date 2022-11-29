package com.mrivanplays.annotationconfig.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.dataformat.toml.TomlReadFeature;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Setting;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents configuration, utilising TOML.
 *
 * @since 1.0
 * @author MrIvanPlays
 */
public final class TomlConfig {

  private static final TomlMapper DEFAULT_TOML_MAPPER =
      TomlMapper.builder().configure(TomlReadFeature.PARSE_JAVA_TIME, true).build();

  /** Returns the {@link Setting} with which the mapper is referenced. */
  public static final Setting<TomlMapper> MAPPER_KEY = Setting.of("mapper", TomlMapper.class);

  private static ConfigResolver configResolver;

  /**
   * Returns the {@link ConfigResolver} instance for toml config.
   *
   * @return config resolver
   */
  public static ConfigResolver getConfigResolver() {
    if (configResolver == null) {
      generateConfigResolver();
    }
    return configResolver;
  }

  private static final ValueWriter TOML_VALUE_WRITER = new TomlValueWriter(DEFAULT_TOML_MAPPER);

  private static void generateConfigResolver() {
    SerializerRegistry registry = SerializerRegistry.INSTANCE;
    if (!registry.hasSerializer(OffsetDateTime.class)) {
      registry.registerSimpleSerializer(
          OffsetDateTime.class,
          data -> OffsetDateTime.parse(data.getAsString()),
          value -> new DataObject(value.toString()));
    }
    if (!registry.hasSerializer(LocalDateTime.class)) {
      registry.registerSimpleSerializer(
          LocalDateTime.class,
          data -> LocalDateTime.parse(data.getAsString()),
          value -> new DataObject(value.toString()));
    }
    if (!registry.hasSerializer(LocalDate.class)) {
      registry.registerSimpleSerializer(
          LocalDate.class,
          data -> LocalDate.parse(data.getAsString()),
          value -> new DataObject(value.toString()));
    }
    if (!registry.hasSerializer(LocalTime.class)) {
      registry.registerSimpleSerializer(
          LocalTime.class,
          data -> LocalTime.parse(data.getAsString()),
          value -> new DataObject(value.toString()));
    }
    configResolver =
        ConfigResolver.newBuilder()
            .withSetting(MAPPER_KEY, DEFAULT_TOML_MAPPER)
            .withSetting(ACDefaultSettings.GENERATE_NEW_OPTIONS, false)
            .withValueWriter(TOML_VALUE_WRITER)
            .withCommentPrefix("# ")
            .withFileExtension(".toml")
            .shouldReverseFields(true)
            .withValueReader(
                (reader, settings) ->
                    (Map<String, Object>)
                        settings
                            .get(MAPPER_KEY)
                            .orElse(DEFAULT_TOML_MAPPER)
                            .reader()
                            .readValue(reader, LinkedHashMap.class))
            .build();
  }
}
