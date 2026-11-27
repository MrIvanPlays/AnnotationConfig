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

import com.mrivanplays.annotationconfig.core.annotations.OneOf;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Validates a deserialized value against the {@link OneOf} annotation of its field, and of the
 * element types of the field's type. On an array field, {@code @OneOf(...) String[]} restricts the
 * elements: Java puts such an annotation on the field and on the element type {@code String}, not
 * on the array type.
 *
 * <p>The annotation is checked here, when a value is validated, and not when the field is read:
 * an invalid one on a field that is never loaded breaks nothing.
 *
 * @hidden
 */
@NullMarked
final class OneOfValidator {

  private OneOfValidator() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * Validates {@code value}, the deserialized value of {@code field} read from {@code key}, and,
   * for a field with {@code @OneOf} on its element types, the elements of the value.
   *
   * @throws IllegalArgumentException if a value is not allowed, or an annotation is invalid
   */
  static void validate(FieldInfo field, String key, @Nullable Object value) {
    if (value == null) {
      return;
    }
    String where = field.genericType().getTypeName() + " " + key;
    OneOf oneOf = field.oneOf();
    // On an array field the annotation is the elements' too, and checked with them.
    boolean forElements = field.elementOneOf() && field.type().isArray();
    if (oneOf != null && !forElements) {
      check(oneOf, field.type(), where, value);
    }
    if (field.elementOneOf()) {
      ElementTypes.forEachAnnotated(
          field.field().getAnnotatedType(),
          value,
          where,
          OneOfValidator::hasOneOf,
          (type, element, elementWhere) ->
              check(type.getAnnotation(OneOf.class), type.getType(), elementWhere, element));
    }
  }

  /** Whether {@code type} has {@code @OneOf} on an element type, at any depth. */
  static boolean hasElementOneOf(AnnotatedType type) {
    return ElementTypes.hasAnnotatedElement(type, OneOfValidator::hasOneOf);
  }

  private static boolean hasOneOf(AnnotatedType type) {
    return type.isAnnotationPresent(OneOf.class);
  }

  /** Checks one value of the declared type {@code type}; {@code where} names it. */
  private static void check(OneOf oneOf, Type type, String where, Object value) {
    if (type != String.class) {
      String hint =
          value instanceof Collection<?> || value instanceof Map<?, ?>
              ? " ; to restrict the elements, put it on the element type, e.g."
                  + " List<@OneOf(...) String>"
              : "";
      throw new IllegalArgumentException(
          where + " ; @OneOf annotation placed on invalid field type, it needs a String" + hint);
    }
    String[] allowed = oneOf.value();
    if (allowed.length == 0) {
      throw new IllegalArgumentException(
          where + " ; invalid @OneOf specified - it should list at least one value");
    }
    if (!(value instanceof String string)) {
      // only possible from a broken custom serializer; setting it on the String field fails
      return;
    }
    for (String candidate : allowed) {
      if (oneOf.ignoreCase() ? candidate.equalsIgnoreCase(string) : candidate.equals(string)) {
        return;
      }
    }
    throw new IllegalArgumentException(
        where
            + " ; deserialized value is not one of the allowed values "
            + Arrays.toString(allowed)
            + (oneOf.ignoreCase() ? " (ignoring case)" : "")
            + " ; got: '"
            + string
            + "'");
  }
}
