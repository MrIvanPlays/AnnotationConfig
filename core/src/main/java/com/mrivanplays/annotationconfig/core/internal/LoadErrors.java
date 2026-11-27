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

import com.mrivanplays.annotationconfig.core.resolver.ConfigLoadException;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The options one load could not read, collected with {@code REPORT_ALL_ERRORS} instead of stopping
 * at the first.
 *
 * @hidden
 */
@NullMarked
final class LoadErrors {

  private final List<ConfigLoadException.Problem> problems = new ArrayList<>();

  private LoadErrors() {}

  /** A collector if {@code settings} turn {@code REPORT_ALL_ERRORS} on, {@code null} otherwise. */
  static @Nullable LoadErrors of(Settings settings) {
    return settings.get(ACDefaultSettings.REPORT_ALL_ERRORS).orElse(false)
        ? new LoadErrors()
        : null;
  }

  /**
   * Collects {@code error}, which reading the option at {@code key} failed with, or throws it when
   * {@code errors} is {@code null}.
   */
  static void report(@Nullable LoadErrors errors, String key, RuntimeException error) {
    if (errors == null) {
      throw error;
    }
    errors.problems.add(new ConfigLoadException.Problem(key, error));
  }

  /**
   * Throws a {@link ConfigLoadException} listing the collected problems, if there are any.
   *
   * @param source what was loaded, for the message
   */
  static void throwIfAny(@Nullable LoadErrors errors, String source) {
    if (errors != null && !errors.problems.isEmpty()) {
      throw new ConfigLoadException(source, errors.problems);
    }
  }
}
