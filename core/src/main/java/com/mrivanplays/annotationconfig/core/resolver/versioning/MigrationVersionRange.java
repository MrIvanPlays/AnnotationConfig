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

package com.mrivanplays.annotationconfig.core.resolver.versioning;

import org.jspecify.annotations.NullMarked;

/**
 * The versions a {@link MigrationStrategy} migrates between: it reads values of version {@link
 * #from()} and returns values of version {@link #to()}. A step may skip versions ({@code 1 -> 3}).
 *
 * @author Ivan Pekov
 * @since 3.1.0
 */
@NullMarked
public sealed interface MigrationVersionRange permits MigrationVersionRangeImpl {

  /**
   * Creates a range from version {@code from} to version {@code to}.
   *
   * @param from the version the migrated values are of, at least 1
   * @param to the version the values are migrated to, greater than {@code from}
   * @return version range
   * @throws IllegalArgumentException if {@code from} is below 1 or {@code to} is not greater
   */
  static MigrationVersionRange of(int from, int to) {
    return new MigrationVersionRangeImpl(from, to);
  }

  /**
   * Returns the version the migrated values are of.
   *
   * @return version
   */
  int from();

  /**
   * Returns the version the values are migrated to.
   *
   * @return version
   */
  int to();
}
