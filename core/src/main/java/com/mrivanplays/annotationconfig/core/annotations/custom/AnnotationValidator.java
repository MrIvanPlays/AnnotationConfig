package com.mrivanplays.annotationconfig.core.annotations.custom;

import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * Represents a validator of a custom annotation.
 *
 * @param <T> annotation type
 * @author MrIvanPlays
 * @since 2.0.0
 */
@FunctionalInterface
public interface AnnotationValidator<T extends Annotation> {

  /**
   * AnnotationConfig calls this method whenever it finds the custom annotation this validator is
   * bound to in order to determine whether the inputted {@code value} is valid for the custom
   * annotation with which the inputted {@link Field} {@code field} was annotated.
   *
   * @param annotation the annotation this validator validates
   * @param value the value AnnotationConfig received
   * @param options the custom options for the config resolver
   * @param field the annotated field
   * @return validation response
   * @see ValidationResponse
   */
  ValidationResponse validate(T annotation, Object value, CustomOptions options, Field field);
}
