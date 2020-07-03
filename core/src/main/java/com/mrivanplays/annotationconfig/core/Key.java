package com.mrivanplays.annotationconfig.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation, representing the config key for the field the annotation is called on. If the
 * annotation is not present, the field name is used as a key.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Key {

  /**
   * Key value
   *
   * @return key
   */
  String value();
}
