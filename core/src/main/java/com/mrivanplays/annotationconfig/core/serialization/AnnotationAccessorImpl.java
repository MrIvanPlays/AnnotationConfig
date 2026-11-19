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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @hidden
 */
@SuppressWarnings("unchecked")
final class AnnotationAccessorImpl implements AnnotationAccessor {

  static AnnotationAccessorImpl EMPTY = new AnnotationAccessorImpl();

  private final Predicate<Class<? extends Annotation>> hasAnnotationPredicate;
  private Function<Class<? extends Annotation>, ? extends Annotation> getAnnotationFunc;

  private AnnotationAccessorImpl() {
    this.hasAnnotationPredicate = _ -> false;
  }

  AnnotationAccessorImpl(Field field) {
    Objects.requireNonNull(field, "field");
    this.hasAnnotationPredicate = field::isAnnotationPresent;
    this.getAnnotationFunc = field::getDeclaredAnnotation;
  }

  AnnotationAccessorImpl(Class<?> clazz) {
    Objects.requireNonNull(clazz, "clazz");
    this.hasAnnotationPredicate = clazz::isAnnotationPresent;
    this.getAnnotationFunc = clazz::getDeclaredAnnotation;
  }

  @Override
  public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
    return hasAnnotationPredicate.test(annotationClass);
  }

  @Override
  public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
    Objects.requireNonNull(annotationClass, "annotationClass");
    if (this.hasAnnotation(annotationClass)) {
      return Optional.ofNullable((T) getAnnotationFunc.apply(annotationClass));
    }
    return Optional.empty();
  }
}
