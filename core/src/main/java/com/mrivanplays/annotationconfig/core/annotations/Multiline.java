package com.mrivanplays.annotationconfig.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which tells AnnotationConfig that the field it is annotated on should be written as a
 * multiline {@link String}. This annotation can only be applied on {@link java.lang.reflect.Field
 * Fields} with type {@link String}.
 *
 * <p>If the {@code String} does not contain a '\n' character, it will be dumped as a regular {@code
 * String}.
 *
 * <p>If the configuration type AnnotationConfig is dumping to, the {@code String} will be dumped
 * just as a regular {@code String}.
 *
 * @since 2.1.0
 * @author MrIvanPlays
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Multiline {

  /**
   * The character to dump which indicates a multiline {@code String}. AnnotationConfig supports
   * double quotes, '>' and '|', but if a custom configuration type is used, it may support other
   * characters.
   *
   * @return character indicating multiline string
   */
  char value() default '"';
}
