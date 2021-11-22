package com.mrivanplays.annotationconfig.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.dataformat.toml.TomlReadFeature;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.options.Option;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSetting;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
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

  /** Returns the key on which the mapper is stored. */
  public static final String MAPPER_KEY = "mapper";

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

  private static void generateConfigResolver() {
    configResolver =
        ConfigResolver.newBuilder()
            .withOption(MAPPER_KEY, Option.of(DEFAULT_TOML_MAPPER).markReplaceable())
            .withLoadSetting(LoadSetting.GENERATE_NEW_OPTIONS, false)
            .withValueWriter(() -> new TomlValueWriter(DEFAULT_TOML_MAPPER))
            .withCommentPrefix("# ")
            .shouldReverseFields(true)
            .withValueReader(
                new ValueReader() {
                  @Override
                  public Map<String, Object> read(Reader reader, CustomOptions options)
                      throws IOException {
                    return (Map<String, Object>)
                        options
                            .getAsOr(MAPPER_KEY, TomlMapper.class, DEFAULT_TOML_MAPPER)
                            .reader()
                            .readValue(reader, LinkedHashMap.class);
                  }
                })
            .build();
  }

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
   * @deprecated see {@link #load(Object, File, TomlMapper)}
   */
  @Deprecated
  public static void load(Object annotatedConfig, File file) {
    load(annotatedConfig, file, DEFAULT_TOML_MAPPER);
  }

  /**
   * Loads the config object from the file. If the file does not exist, it creates one.
   *
   * @param annotatedConfig annotated config
   * @param file file
   * @param tomlMapper toml mapper
   * @deprecated use {@link #getConfigResolver()}
   */
  @Deprecated
  public static void load(Object annotatedConfig, File file, TomlMapper tomlMapper) {
    ConfigResolver resolver = getConfigResolver();
    if (!resolver.options().has(MAPPER_KEY)) {
      resolver.options().put(MAPPER_KEY, Option.of(tomlMapper).markReplaceable());
    } else {
      if (resolver.options().isReplaceable(MAPPER_KEY).orElse(false)) {
        resolver.options().put(MAPPER_KEY, Option.of(tomlMapper).markReplaceable());
      }
    }
    resolver.loadOrDump(annotatedConfig, file);
  }
}
