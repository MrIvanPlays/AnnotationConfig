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

package com.mrivanplays.annotationconfig.core.annotations.custom;

import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a validator of a custom annotation.
 *
 * @param <T> annotation type
 * @author Ivan Pekov
 * @since 2.0.0
 */
@NullMarked
public interface AnnotationValidator<T extends Annotation> {

  /**
   * AnnotationConfig calls this method whenever it finds the custom annotation this validator is
   * bound to in order to determine whether the inputted {@code value} is valid for the custom
   * annotation with which the inputted {@link Field} {@code field} was annotated.
   *
   * @param annotation the annotation this validator validates
   * @param value the value AnnotationConfig received; {@code null} when it is read as null and the
   *     null read handler is {@code SET_NULL}
   * @param settings the settings for the config resolver
   * @param field the annotated field
   * @return validation response
   * @see ValidationResponse
   */
  ValidationResponse validate(T annotation, @Nullable Object value, Settings settings, Field field);

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
  @Nullable
  default Object writeValue(Object value) {
    return null;
  }
}
