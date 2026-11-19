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

package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.ValidationResponse;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.lang.reflect.Field;

public class DummyAnnotationHandler implements AnnotationValidator<DummyAnnotation> {

  @Override
  public ValidationResponse validate(
      DummyAnnotation annotation, Object value, Settings settings, Field field) {
    if (annotation.value()) {
      if (settings.has(OptsConstant.DUMMY_DEFAULT)) {
        boolean optVal = settings.get(OptsConstant.DUMMY_DEFAULT).get();
        settings.put(OptsConstant.DUMMY_DEFAULT, !optVal);
      }
    }
    IllegalArgumentException error = new IllegalArgumentException("Dummy throwable");
    if (value instanceof String) {
      return ValidationResponse.fromBooleanValue(((String) value).contains("Hello"), error);
    }
    return ValidationResponse.fail(error);
  }
}
