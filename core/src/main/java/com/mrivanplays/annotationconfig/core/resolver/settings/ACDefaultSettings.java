package com.mrivanplays.annotationconfig.core.resolver.settings;

/**
 * Represents a holder of the default {@link Settings} for AnnotationConfig.
 *
 * @since 3.0.0
 * @author MrIvanPlays
 */
public final class ACDefaultSettings {

  public static final Setting<NullReadHandleOption> NULL_READ_HANDLER =
      Setting.of("null_read_handler", NullReadHandleOption.class);

  public static final Setting<Boolean> GENERATE_NEW_OPTIONS =
      Setting.of("generate_new_options", Boolean.class);

  /**
   * A setting indicating whether to find {@link java.lang.reflect.Field fields} of classes which
   * have been inherited by the given config class.
   *
   * <p>Consider this example:
   *
   * <pre>
   *   <code>
   * public class BaseConfig {
   *       int foo = 1;
   *     }
   *
   *     public class SpecificConfig extends BaseConfig {
   *       String bar = "baz";
   *     }
   *   </code>
   * </pre>
   *
   * <p>This setting indicates whether the field {@code foo} will also be an option in the dumped
   * config or not.
   */
  public static final Setting<Boolean> FIND_PARENT_FIELDS =
      Setting.of("find_parent_fields", Boolean.class);

  private static Settings defaults;

  public static Settings getDefault() {
    if (defaults != null) {
      return defaults;
    }
    return defaults =
        new Settings()
            .put(NULL_READ_HANDLER, NullReadHandleOption.SET_NULL)
            .put(GENERATE_NEW_OPTIONS, true)
            .put(FIND_PARENT_FIELDS, false)
            .copy(true);
  }
}
