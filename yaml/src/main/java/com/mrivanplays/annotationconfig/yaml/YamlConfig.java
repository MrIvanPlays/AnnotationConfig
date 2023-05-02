package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.key.DottedResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Setting;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.util.Collections;
import java.util.LinkedHashMap;
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

  /**
   * Returns the {@link Setting} with which the yaml instance is referenced in the {@link Settings}
   */
  public static final Setting<Yaml> YAML_INSTANCE = Setting.of("yaml", Yaml.class);

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

  private static final ValueWriter YAML_VALUE_WRITER = new YamlValueWriter();

  private static void generateConfigResolver() {
    configResolver =
        ConfigResolver.newBuilder()
            .withKeyResolver(DottedResolver.getInstance())
            .withValueWriter(YAML_VALUE_WRITER)
            .withSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, true)
            .withCommentPrefix("# ")
            .withFileExtension(".yml")
            .withFileExtension(".yaml")
            .withValueReader(
                (reader, settings) -> {
                  Map<String, Object> values =
                      settings.get(YAML_INSTANCE).orElse(YAML).loadAs(reader, LinkedHashMap.class);
                  if (values == null) {
                    return Collections.emptyMap();
                  }
                  return values;
                })
            .build();
  }
}
