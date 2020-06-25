package com.mrivanplays.annotationconfig.core;

import java.lang.reflect.Field;

/**
 * Represents a field type resolver.
 *
 * <p>A field type resolver is an raw argument resolver, read from the config, and then parsed to a
 * field type.
 */
public interface FieldTypeResolver {

  /**
   * AnnotationConfig will call this method whenever it is setting a config object's field, if a
   * field type resolver was specified on the field.
   *
   * @param value config value
   * @param field field we're resolving
   * @return value with type of the field this resolver was specified on.
   * @throws Exception something wrong occurred
   */
  Object toType(Object value, Field field) throws Exception;

  /**
   * AnnotationConfig will call this method before calling {@link #toType(Object, Field)} in order
   * to see if it should proceed processing the value convertion.
   *
   * @param fieldType field type
   * @return should resolve or not
   */
  boolean shouldResolve(Class<?> fieldType);
}
