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

package com.mrivanplays.annotationconfig.scenarios.validation;

import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.ValidationResponse;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DummyAnnotation2Handler implements AnnotationValidator<DummyAnnotation2> {

  /** Counts how often the success callback ran, so tests can check that it does. */
  static final AtomicInteger SUCCESS_CALLBACKS = new AtomicInteger();

  @Override
  public ValidationResponse validate(
      DummyAnnotation2 annotation, Object value, Settings settings, Field field) {
    return ValidationResponse.success(SUCCESS_CALLBACKS::incrementAndGet);
  }

  @Override
  public Object writeValue(Object value) {
    return "Lorem ipsum dolor sit amet.";
  }
}
