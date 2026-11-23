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

package com.mrivanplays.annotationconfig.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation, representing the maximal string length or the maximal
 * int/double/byte/float/short/long value.
 *
 * <p>Exactly one member has to be set. Each member defaults to the highest value of its type
 * ({@code Integer.MAX_VALUE}, {@code Double.POSITIVE_INFINITY}, ...), which means "not set"; any
 * other value, including {@code -1}, is a bound.
 *
 * @since 2.0.0
 * @author Ivan Pekov
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Max {

  /**
   * The maximum {@code int} value, or the maximum length of a string.
   *
   * @return the bound
   */
  int maxInt() default Integer.MAX_VALUE;

  /**
   * The maximum {@code double} value.
   *
   * @return the bound
   */
  double maxDouble() default Double.POSITIVE_INFINITY;

  /**
   * The maximum {@code byte} value.
   *
   * @return the bound
   */
  byte maxByte() default Byte.MAX_VALUE;

  /**
   * The maximum {@code float} value.
   *
   * @return the bound
   */
  float maxFloat() default Float.POSITIVE_INFINITY;

  /**
   * The maximum {@code short} value.
   *
   * @return the bound
   */
  short maxShort() default Short.MAX_VALUE;

  /**
   * The maximum {@code long} value.
   *
   * @return the bound
   */
  long maxLong() default Long.MAX_VALUE;
}
