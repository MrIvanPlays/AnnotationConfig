/*
 * Copyright (c) Ivan Pekov 2026
 * Copyright (c) Contributors 2026
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.mrivanplays.annotationconfig.core.serialization;

import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Represents an annotation accessor, a controlled manner of accessing (field) annotations.
 *
 * @author Ivan Pekov
 * @since 3.0.0
 */
@NullMarked
public sealed interface AnnotationAccessor permits AnnotationAccessorImpl {

  /**
   * Returns an {@link AnnotationAccessor}, which {@link #getAnnotation(Class)} method will always
   * return {@link Optional#empty()}
   */
  AnnotationAccessor EMPTY = AnnotationAccessorImpl.EMPTY;

  /**
   * Creates a new {@link AnnotationAccessor} from the specified {@link Field} {@code field}
   *
   * @param field field to create annotation accessor from
   * @return annotation accessor
   */
  static AnnotationAccessor createFromField(Field field) {
    return new AnnotationAccessorImpl(field);
  }

  /**
   * Creates a new {@link AnnotationAccessor} from the specified {@link Class} {@code clazz}
   *
   * @param clazz class to create annotation accessor from
   * @return annotation accessor
   * @since 3.1.0
   */
  static AnnotationAccessor createFromClass(Class<?> clazz) {
    return new AnnotationAccessorImpl(clazz);
  }

  /**
   * Returns whether this annotation accessor holds the specified {@link Class} {@code
   * annotationClass}
   *
   * @param annotationClass annotation class, not null
   * @return true if holds, false otherwise
   * @since 3.1.0
   */
  boolean hasAnnotation(Class<? extends Annotation> annotationClass);

  /**
   * Returns whether this annotation accessor holds the specified {@link AnnotationType annotation
   * type}
   *
   * @param annotationType annotation type to check
   * @return true if holds, false otherwise
   * @since 3.1.0
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  default boolean hasAnnotation(AnnotationType annotationType) {
    return this.hasAnnotation(annotationType.getRawType());
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
