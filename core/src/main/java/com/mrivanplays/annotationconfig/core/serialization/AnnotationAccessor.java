package com.mrivanplays.annotationconfig.core.serialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents an annotation accessor, a controlled manner of accessing (field) annotations.
 *
 * @author MrIvanPlays
 * @since 3.0.0
 */
public interface AnnotationAccessor {

  /**
   * Returns an {@link AnnotationAccessor}, which {@link #getAnnotation(Class)} method will always
   * return {@link Optional#empty()}
   */
  AnnotationAccessor EMPTY =
      new AnnotationAccessor() {
        @Override
        public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
          return Optional.empty();
        }
      };

  /**
   * Creates a new {@link AnnotationAccessor} from the specified {@link Field} {@code field}
   *
   * @param field field to create annotation accessor from
   * @return annotation accessor
   */
  static AnnotationAccessor createFromField(Field field) {
    Objects.requireNonNull(field, "field");
    return new AnnotationAccessor() {
      @Override
      public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass, "annotationClass");
        return Optional.ofNullable(field.getDeclaredAnnotation(annotationClass));
      }
    };
  }

  /**
   * Returns an {@link Optional}, which may be fulfilled with the needed {@link Annotation}, nailed
   * to the specified {@link Class} {@code annotationClass}.
   *
   * @param annotationClass annotation class
   * @param <T> annotation type
   * @return annotation or empty optional
   */
  <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass);
}
