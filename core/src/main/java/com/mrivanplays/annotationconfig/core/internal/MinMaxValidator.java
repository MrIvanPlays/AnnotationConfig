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
import java.lang.reflect.AnnotatedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Validates a deserialized value against the {@link Min} and {@link Max} annotations of its field.
 * Numbers are checked by value, strings by their length.
 *
 * <p>The annotations can also be put on the element types of the field's type: {@code
 * List<@Max(maxInt = 10) Integer>}, {@code Map<String, @Min(minInt = 0) Integer>}, nested ones
 * ({@code List<List<@Max(...) Integer>>}) and arrays. On an array field, {@code @Max(...) int[]}
 * bounds the elements: Java puts such an annotation on the field and on the element type {@code
 * int}, not on the array type.
 *
 * @hidden
 */
@NullMarked
final class MinMaxValidator {

  private MinMaxValidator() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * Validates {@code value}, the deserialized value of {@code field} read from {@code key}, and,
   * for a field with bounds on its element types, the elements of the value.
   *
   * @throws IllegalArgumentException if a value is out of bounds, or an annotation is invalid
   */
  static void validate(FieldInfo field, String key, @Nullable Object value) {
    if (value == null) {
      // a value read as null has nothing to compare; it used to be reported as a misplaced @Min
      return;
    }
    String where = field.genericType().getTypeName() + " " + key;
    Min min = field.min();
    Max max = field.max();
    // On an array field the annotation is the elements' too, and checked with them.
    boolean forElements = field.elementBounds() && value.getClass().isArray();
    if ((min != null || max != null) && !forElements) {
      check(min, max, where, value);
    }
    if (field.elementBounds()) {
      ElementTypes.forEachAnnotated(
          field.field().getAnnotatedType(),
          value,
          where,
          MinMaxValidator::hasBounds,
          (type, element, elementWhere) ->
              check(
                  type.getAnnotation(Min.class),
                  type.getAnnotation(Max.class),
                  elementWhere,
                  element));
    }
  }

  /** Whether {@code type} has {@code @Min} or {@code @Max} on an element type, at any depth. */
  static boolean hasElementBounds(AnnotatedType type) {
    return ElementTypes.hasAnnotatedElement(type, MinMaxValidator::hasBounds);
  }

  private static boolean hasBounds(AnnotatedType type) {
    return type.isAnnotationPresent(Min.class) || type.isAnnotationPresent(Max.class);
  }

  /** Checks one value, a number or a string, against the bounds; {@code where} names it. */
  private static void check(@Nullable Min min, @Nullable Max max, String where, Object value) {
    Number compared;
    String subject;
    if (value instanceof Number number) {
      compared = number;
      subject = "value";
    } else if (value instanceof String string) {
      compared = string.length();
      subject = "String's length";
    } else {
      String annotation = min != null ? "Min" : "Max";
      String hint =
          value instanceof Collection<?> || value instanceof Map<?, ?>
              ? " ; to bound the elements, put it on the element type, e.g. List<@"
                  + annotation
                  + "(...) Integer>"
              : "";
      throw new IllegalArgumentException(
          where + " ; @" + annotation + " annotation placed on invalid field type" + hint);
    }
    if ((compared instanceof Double d && d.isNaN()) || (compared instanceof Float f && f.isNaN())) {
      // NaN is in no range; Double.compare puts it above every number, so it passed any @Min
      throw new IllegalArgumentException(where + " ; deserialized value is not a number (NaN)");
    }
    if (min != null) {
      Number bound = minBound(where, min);
      if (compare(compared, bound) < 0) {
        throw new IllegalArgumentException(
            where
                + " ; deserialized "
                + subject
                + " is under the minimum allowed ("
                + bound
                + ") ; got: "
                + compared);
      }
    }
    if (max != null) {
      Number bound = maxBound(where, max);
      if (compare(compared, bound) > 0) {
        throw new IllegalArgumentException(
            where
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

  static Number minBound(String where, Min min) {
    return singleBound(
        where,
        "@Min",
        "@Min(minInt = 1)",
        min.minByte() != Byte.MIN_VALUE ? min.minByte() : null,
        min.minShort() != Short.MIN_VALUE ? min.minShort() : null,
        min.minInt() != Integer.MIN_VALUE ? min.minInt() : null,
        min.minLong() != Long.MIN_VALUE ? min.minLong() : null,
        min.minFloat() != Float.NEGATIVE_INFINITY ? min.minFloat() : null,
        min.minDouble() != Double.NEGATIVE_INFINITY ? min.minDouble() : null);
  }

  static Number maxBound(String where, Max max) {
    return singleBound(
        where,
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
      String where, String annotation, String example, @Nullable Number... members) {
    Number bound = null;
    for (Number member : members) {
      if (member == null) {
        continue;
      }
      if (bound != null) {
        throw new IllegalArgumentException(
            where
                + " ; invalid "
                + annotation
                + " specified - it should set only one annotation member");
      }
      bound = member;
    }
    if (bound == null) {
      throw new IllegalArgumentException(
          where
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
