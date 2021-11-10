package com.mrivanplays.annotationconfig.core.annotations.custom;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Represents a validator of a custom annotation.
 *
 * @param <T> annotation type
 * @author MrIvanPlays
 * @since 2.0.0
 */
public interface AnnotationValidator<T extends Annotation> {

  /**
   * AnnotationConfig calls this method whenever it finds the custom annotation this validator is
   * bound to in order to determine whether the inputted {@code value} is valid for the custom
   * annotation with which the inputted {@link Field} {@code field} was annotated.
   *
   * <p>If validation has not passed (this returns {@code false}) and {@link #error()} is not null,
   * the return value of {@link #error()} will be thrown via a {@link RuntimeException}. If {@link
   * #error()} is null, the field set will be silently skipped.
   *
   * @param annotation the annotation this validator validates
   * @param value the value AnnotationConfig received
   * @param field the annotated field
   * @return true if value is valid, false otherwise
   */
  boolean validate(T annotation, Object value, Field field);

  /**
   * AnnotationConfig throws the returned {@link Throwable} of this method, if not null, whenever
   * the result of {@link #validate(Annotation, Object, Field)} is {@code false}.
   *
   * @return an error to throw if validate result is false
   */
  default Throwable error() {
    return null;
  }
}
