package com.mrivanplays.annotationconfig.core.resolver.settings;

import com.mrivanplays.annotationconfig.core.resolver.NullReadHandleOption;

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

  private static Settings defaults;

  public static Settings getDefault() {
    if (defaults != null) {
      return defaults;
    }
    return defaults =
        new Settings()
            .put(NULL_READ_HANDLER, NullReadHandleOption.SET_NULL)
            .put(GENERATE_NEW_OPTIONS, true)
            .copy(true);
  }
}
