package com.mrivanplays.annotationconfig.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Min {

  int minInt() default -1;
  double minDouble() default -1;
  byte minByte() default -1;
  float minFloat() default -1;
  short minShort() default -1;
  long minLong() default -1;

}
