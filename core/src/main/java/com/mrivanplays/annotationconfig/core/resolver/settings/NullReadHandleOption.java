package com.mrivanplays.annotationconfig.core.resolver.settings;

/**
 * Represents a simple 2 option enum, which is telling the resolver how to handle null values, which
 * have been deserialized, but the deserialized value returned null.
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
public enum NullReadHandleOption {
  /**
   * If this constant is set, it tells the resolver that if we get a null value it will skip binding
   * it to the {@link java.lang.reflect.Field}, which is representing that value, which in tern
   * means that it will return the default value.
   */
  USE_DEFAULT_VALUE,

  /**
   * If this constant is set, it tells the resolver that if we get a null value it will bind it to
   * the {@link java.lang.reflect.Field}, which is representing that value.
   */
  SET_NULL
}
