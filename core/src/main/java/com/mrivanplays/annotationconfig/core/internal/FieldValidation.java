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

package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.annotations.custom.ValidationResponse;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.lang.annotation.Annotation;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Checks a read value against the {@code @Min}, {@code @Max} and {@code @OneOf} annotations and the
 * validators of the custom annotations of its field. Used for the fields of a config and its config
 * objects, the fields of custom data types and the components of records alike.
 *
 * @hidden
 */
@NullMarked
public final class FieldValidation {

  private FieldValidation() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * Validates {@code value}, read for {@code field} from {@code key}.
   *
   * @return {@code false} if a custom validator failed silently: the value is not set, and the
   *     field keeps its value
   * @throws IllegalArgumentException if the value is out of the bounds of {@code @Min} or {@code
   *     Max}, or not one of the values of {@code @OneOf}
   * @throws RuntimeException wrapping the error a custom validator reported
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static boolean validate(
      FieldInfo field, String key, @Nullable Object value, Settings settings) {
    if (field.minOrMax()) {
      MinMaxValidator.validate(field, key, value);
    }
    if (field.hasOneOf()) {
      OneOfValidator.validate(field, key, value);
    }
    CustomAnnotationRegistry registry = CustomAnnotationRegistry.INSTANCE;
    if (registry.isEmpty()) {
      return true;
    }
    for (Annotation annotation : field.annotations()) {
      Class<? extends Annotation> type = annotation.annotationType();
      if (AnnotationType.match(type).isPresent()) {
        // do not handle any validation of our own annotations even if someone registered a
        // validator for them.
        continue;
      }
      Optional<AnnotationValidator<? extends Annotation>> validator = registry.getValidator(type);
      if (validator.isEmpty()) {
        continue;
      }
      ValidationResponse response =
          ((AnnotationValidator) validator.get())
              .validate(annotation, value, settings, field.field());
      if (response.throwError() != null) {
        throw new RuntimeException(response.throwError());
      }
      if (response.shouldFailSilently()) {
        return false;
      }
      Runnable onSuccess = response.onSuccess();
      if (onSuccess != null) {
        onSuccess.run();
      }
    }
    return true;
  }
}
