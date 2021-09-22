package com.mrivanplays.annotationconfig.core.annotations.comment;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation, telling the config writer to write a comment with the specified value. */
@Documented
@Repeatable(Comments.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Comment {

  /**
   * The comment you want to specify.
   *
   * @return comment
   */
  String value();
}
