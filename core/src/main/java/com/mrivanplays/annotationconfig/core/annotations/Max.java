package com.mrivanplays.annotationconfig.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation, representing the maximal string length or the maximal
 * int/double/byte/float/short/long value.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Max {

  int maxInt() default -1;

  double maxDouble() default -1;

  byte maxByte() default -1;

  float maxFloat() default -1;

  short maxShort() default -1;

  long maxLong() default -1;
}
