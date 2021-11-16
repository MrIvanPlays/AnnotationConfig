package com.mrivanplays.annotationconfig.core.resolver.settings;

/**
 * Represents a basic load setting, which holds a {@link String} as a key identifier, and a {@link
 * Class} as type.
 *
 * @param <T> load setting type
 * @author MrIvanPlays
 * @since 2.0.0
 */
public final class LoadSetting<T> {

  /**
   * Creates a new load setting.
   *
   * @param key the key of the load setting
   * @param type the type of data this load setting handles
   * @param <T> value type
   * @return load setting instance
   */
  public static <T> LoadSetting<T> of(String key, Class<T> type) {
    return new LoadSetting<>(key, type);
  }

  /**
   * A {@link Boolean} type load setting, representing whether to generate new/non-existing options
   * in a file.
   */
  public static final LoadSetting<Boolean> GENERATE_NEW_OPTIONS =
      of("generate_new_options", Boolean.class);

  /**
   * A load setting, value of which tells AnnotationConfig how to handle null read values.
   *
   * <p>Null read values are values of {@link java.lang.reflect.Field Fields}, which upon
   * deserialization return {@code null}.
   */
  public static final LoadSetting<NullReadHandleOption> NULL_READ_HANDLER =
      of("null_read_handler", NullReadHandleOption.class);

  private final String key;
  private final Class<T> type;

  private LoadSetting(String key, Class<T> type) {
    this.key = key;
    this.type = type;
  }

  /**
   * Returns the key of this load setting.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns the class type of this load setting.
   *
   * @return the class type
   */
  public Class<T> getType() {
    return type;
  }
}
