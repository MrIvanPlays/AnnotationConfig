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

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Walks the element types of a field's type next to its value, for annotations put on element types
 * ({@code List<@Max(...) Integer>}, {@code Map<@OneOf(...) String, Integer>}, nested types,
 * wildcard bounds and arrays).
 *
 * @hidden
 */
@NullMarked
final class ElementTypes {

  private ElementTypes() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /** Whether an element type of {@code type}, at any depth, is {@code annotated}. */
  static boolean hasAnnotatedElement(AnnotatedType type, Predicate<AnnotatedType> annotated) {
    for (AnnotatedType element : elementTypes(type)) {
      if (annotated.test(element) || hasAnnotatedElement(element, annotated)) {
        return true;
      }
    }
    return false;
  }

  /** The element types of an array, a parameterized type or a wildcard's bounds. */
  private static AnnotatedType[] elementTypes(AnnotatedType type) {
    if (type instanceof AnnotatedArrayType array) {
      return new AnnotatedType[] {array.getAnnotatedGenericComponentType()};
    }
    if (type instanceof AnnotatedParameterizedType parameterized) {
      return parameterized.getAnnotatedActualTypeArguments();
    }
    if (type instanceof AnnotatedWildcardType wildcard) {
      return wildcard.getAnnotatedUpperBounds();
    }
    return new AnnotatedType[0];
  }

  /**
   * Passes every element of {@code value}, whose type is {@code type}, at any depth, whose type is
   * {@code annotated} to {@code check}. Null elements are skipped.
   */
  static void forEachAnnotated(
      AnnotatedType type,
      Object value,
      String where,
      Predicate<AnnotatedType> annotated,
      ElementCheck check) {
    if (type instanceof AnnotatedArrayType array && value.getClass().isArray()) {
      AnnotatedType component = array.getAnnotatedGenericComponentType();
      int length = Array.getLength(value);
      for (int i = 0; i < length; i++) {
        element(component, Array.get(value, i), where + "[" + i + "]", annotated, check);
      }
    } else if (type instanceof AnnotatedParameterizedType parameterized) {
      AnnotatedType[] arguments = parameterized.getAnnotatedActualTypeArguments();
      if (value instanceof Collection<?> collection && arguments.length == 1) {
        int index = 0;
        for (Object element : collection) {
          element(arguments[0], element, where + "[" + index + "]", annotated, check);
          index++;
        }
      } else if (value instanceof Map<?, ?> map && arguments.length == 2) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
          String keyWhere = where + " key '" + entry.getKey() + "'";
          element(arguments[0], entry.getKey(), keyWhere, annotated, check);
          element(arguments[1], entry.getValue(), where + "." + entry.getKey(), annotated, check);
        }
      }
    }
  }

  private static void element(
      AnnotatedType type,
      @Nullable Object element,
      String where,
      Predicate<AnnotatedType> annotated,
      ElementCheck check) {
    if (element == null) {
      return;
    }
    // List<? extends @Max(...) Integer>: the annotation is on the wildcard's bound
    AnnotatedType checked = type;
    if (!annotated.test(type)
        && type instanceof AnnotatedWildcardType wildcard
        && wildcard.getAnnotatedUpperBounds().length > 0) {
      checked = wildcard.getAnnotatedUpperBounds()[0];
    }
    if (annotated.test(checked)) {
      check.check(checked, element, where);
    }
    forEachAnnotated(checked, element, where, annotated, check);
  }

  /** Checks one element whose type carries an annotation; {@code where} names it. */
  @FunctionalInterface
  interface ElementCheck {

    void check(AnnotatedType type, Object element, String where);
  }
}
