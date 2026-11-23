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

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Validates a deserialized value against the {@link Min} and {@link Max} annotations of its field.
 * Numbers are checked by value, strings by their length.
 *
 * @hidden
 */
@NullMarked
final class MinMaxValidator {

  private MinMaxValidator() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * Validates {@code value}, the deserialized value of {@code field} read from {@code key}.
   *
   * @throws IllegalArgumentException if the value is out of bounds, or an annotation is invalid
   */
  static void validate(FieldInfo field, String key, @Nullable Object value) {
    Min min = field.min();
    Max max = field.max();
    if ((min == null && max == null) || value == null) {
      // a value read as null has nothing to compare; it used to be reported as a misplaced @Min
      return;
    }
    Number compared;
    String subject;
    if (value instanceof Number number) {
      compared = number;
      subject = "value";
    } else if (value instanceof String string) {
      compared = string.length();
      subject = "String's length";
    } else {
      throw new IllegalArgumentException(
          where(field, key)
              + " ; @"
              + (min != null ? "Min" : "Max")
              + " annotation placed on invalid field type");
    }
    if (min != null) {
      Number bound = minBound(field, key, min);
      if (compare(compared, bound) < 0) {
        throw new IllegalArgumentException(
            where(field, key)
                + " ; deserialized "
                + subject
                + " is under the minimum allowed ("
                + bound
                + ") ; got: "
                + compared);
      }
    }
    if (max != null) {
      Number bound = maxBound(field, key, max);
      if (compare(compared, bound) > 0) {
        throw new IllegalArgumentException(
            where(field, key)
                + " ; deserialized "
                + subject
                + " is above the maximum allowed ("
                + bound
                + ") ; got: "
                + compared);
      }
    }
  }

  // A member left at its default (the type's lowest value for @Min, highest for @Max) is not set.

  /** The start of every error message, only built when one is thrown. */
  private static String where(FieldInfo field, String key) {
    return field.genericType().getTypeName() + " " + key;
  }

  private static Number minBound(FieldInfo field, String key, Min min) {
    return singleBound(
        field,
        key,
        "@Min",
        "@Min(minInt = 1)",
        min.minByte() != Byte.MIN_VALUE ? min.minByte() : null,
        min.minShort() != Short.MIN_VALUE ? min.minShort() : null,
        min.minInt() != Integer.MIN_VALUE ? min.minInt() : null,
        min.minLong() != Long.MIN_VALUE ? min.minLong() : null,
        min.minFloat() != Float.NEGATIVE_INFINITY ? min.minFloat() : null,
        min.minDouble() != Double.NEGATIVE_INFINITY ? min.minDouble() : null);
  }

  private static Number maxBound(FieldInfo field, String key, Max max) {
    return singleBound(
        field,
        key,
        "@Max",
        "@Max(maxInt = 3)",
        max.maxByte() != Byte.MAX_VALUE ? max.maxByte() : null,
        max.maxShort() != Short.MAX_VALUE ? max.maxShort() : null,
        max.maxInt() != Integer.MAX_VALUE ? max.maxInt() : null,
        max.maxLong() != Long.MAX_VALUE ? max.maxLong() : null,
        max.maxFloat() != Float.POSITIVE_INFINITY ? max.maxFloat() : null,
        max.maxDouble() != Double.POSITIVE_INFINITY ? max.maxDouble() : null);
  }

  private static Number singleBound(
      FieldInfo field, String key, String annotation, String example, @Nullable Number... members) {
    Number bound = null;
    for (Number member : members) {
      if (member == null) {
        continue;
      }
      if (bound != null) {
        throw new IllegalArgumentException(
            where(field, key)
                + " ; invalid "
                + annotation
                + " specified - it should set only one annotation member");
      }
      bound = member;
    }
    if (bound == null) {
      throw new IllegalArgumentException(
          where(field, key)
              + " ; invalid "
              + annotation
              + " specified - it should set an annotation member (e.g. "
              + example
              + ")");
    }
    return bound;
  }

  /** Compares two numbers by their exact value, whatever their types. */
  private static int compare(Number a, Number b) {
    if (isNonFinite(a) || isNonFinite(b)) {
      // BigDecimal has no NaN or infinity; Double.compare orders them consistently
      return Double.compare(a.doubleValue(), b.doubleValue());
    }
    return toBigDecimal(a).compareTo(toBigDecimal(b));
  }

  private static boolean isNonFinite(Number number) {
    return (number instanceof Double d && !Double.isFinite(d))
        || (number instanceof Float f && !Float.isFinite(f));
  }

  private static BigDecimal toBigDecimal(Number number) {
    if (number instanceof BigDecimal decimal) {
      return decimal;
    }
    if (number instanceof BigInteger integer) {
      return new BigDecimal(integer);
    }
    if (number instanceof Byte
        || number instanceof Short
        || number instanceof Integer
        || number instanceof Long) {
      return BigDecimal.valueOf(number.longValue());
    }
    if (number instanceof Float f) {
      // the float's shortest decimal form, so that @Min(minFloat = 0.1f) accepts 0.1 and not
      // only values from 0.100000001490116... on
      return new BigDecimal(Float.toString(f));
    }
    if (number instanceof Double d) {
      return BigDecimal.valueOf(d);
    }
    // other Number implementations, such as AtomicLong
    try {
      return new BigDecimal(number.toString());
    } catch (NumberFormatException e) {
      return BigDecimal.valueOf(number.doubleValue());
    }
  }
}
