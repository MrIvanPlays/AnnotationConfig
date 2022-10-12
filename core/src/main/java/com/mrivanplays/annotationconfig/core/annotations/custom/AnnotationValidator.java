package com.mrivanplays.annotationconfig.core.annotations.custom;

import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
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
   * @param annotation the annotation this validator validates
   * @param value the value AnnotationConfig received
   * @param settings the settings for the config resolver
   * @param field the annotated field
   * @return validation response
   * @see ValidationResponse
   */
  ValidationResponse validate(T annotation, Object value, Settings settings, Field field);

  /**
   * AnnotationConfig calls this method whenever it finds the custom annotation this validator is
   * bound to in order to write the {@code value} to a config. Method is optional to implement, if
   * it is not implemented, or returns {@code null}, AnnotationConfig will write the {@code value}
   * in the default way it can.
   *
   * <p>If the returned value is an object, for which there is a registered {@link
   * com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer}, it will serialize it
   * accordingly.
   *
   * @param value the value to write
   * @return an easier serializable value
   */
  default Object writeValue(Object value) {
    return null;
  }
}
