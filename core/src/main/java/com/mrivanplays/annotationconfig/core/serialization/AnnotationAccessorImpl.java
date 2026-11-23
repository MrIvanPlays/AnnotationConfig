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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @hidden
 */
@SuppressWarnings("unchecked")
@NullMarked
final class AnnotationAccessorImpl implements AnnotationAccessor {

  static final AnnotationAccessorImpl EMPTY = new AnnotationAccessorImpl(_ -> null);

  // both methods look at the declared annotations only, so they always agree
  private final Function<Class<? extends Annotation>, @Nullable Annotation> getAnnotationFunc;

  private AnnotationAccessorImpl(
      Function<Class<? extends Annotation>, @Nullable Annotation> getAnnotationFunc) {
    this.getAnnotationFunc = getAnnotationFunc;
  }

  AnnotationAccessorImpl(Field field) {
    this(Objects.requireNonNull(field, "field")::getDeclaredAnnotation);
  }

  AnnotationAccessorImpl(Class<?> clazz) {
    this(Objects.requireNonNull(clazz, "clazz")::getDeclaredAnnotation);
  }

  @Override
  public boolean hasAnnotation(Class<? extends Annotation> annotationClass) {
    Objects.requireNonNull(annotationClass, "annotationClass");
    return this.getAnnotationFunc.apply(annotationClass) != null;
  }

  @Override
  public <T extends Annotation> Optional<T> getAnnotation(Class<T> annotationClass) {
    Objects.requireNonNull(annotationClass, "annotationClass");
    return Optional.ofNullable((T) this.getAnnotationFunc.apply(annotationClass));
  }
}
