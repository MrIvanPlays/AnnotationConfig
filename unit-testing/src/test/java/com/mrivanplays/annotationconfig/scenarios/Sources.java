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

package com.mrivanplays.annotationconfig.scenarios;

/** The {@code @MethodSource}s of the scenario classes: the formats with what a topic needs. */
public final class Sources {

  private static final String FORMAT = "com.mrivanplays.annotationconfig.scenarios.Format#";

  /** {@link Format#all()}. */
  public static final String ALL = FORMAT + "all";

  /** {@link Format#withSections()}. */
  public static final String WITH_SECTIONS = FORMAT + "withSections";

  /** {@link Format#withCommentedSections()}. */
  public static final String WITH_COMMENTED_SECTIONS = FORMAT + "withCommentedSections";

  /** {@link Format#withNulls()}. */
  public static final String WITH_NULLS = FORMAT + "withNulls";

  /** {@link Format#withBigIntegers()}. */
  public static final String WITH_BIG_INTEGERS = FORMAT + "withBigIntegers";

  /** {@link Format#withSpecialFloats()}. */
  public static final String WITH_SPECIAL_FLOATS = FORMAT + "withSpecialFloats";

  private Sources() {}
}
