package com.mrivanplays.annotationconfig.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation, representing a custom (non-primitive) type of the field it was annotated on. The
 * value specified should be a {@link FieldTypeResolver} class, which is resolving primitive value
 * to field value.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TypeResolver {

  /**
   * Class value of the {@link FieldTypeResolver}
   *
   * @return class value
   */
  Class<? extends FieldTypeResolver> value();
}
