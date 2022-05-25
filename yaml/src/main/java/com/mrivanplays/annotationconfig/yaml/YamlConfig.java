package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.key.DottedResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSettings;
import java.io.File;
import java.io.Reader;
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
            .shouldReverseFields(true)
            .withCommentPrefix("# ")
            .withFileExtension(".yml")
            .withFileExtension(".yaml")
            .withValueReader(
                new ValueReader() {
                  @Override
                  public Map<String, Object> read(Reader reader) {
                    Map<String, Object> values = YAML.loadAs(reader, LinkedHashMap.class);
                    if (values == null) {
                      return Collections.emptyMap();
                    }
                    return values;
                  }
                })
            .build();
  }
}
