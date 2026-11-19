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

import java.lang.reflect.Field;

/**
 * @hidden
 */
public final class AnnotationHolder implements Comparable<AnnotationHolder> {
  private Field field;
  private int fieldOrder;
  private final boolean isClass;
  private Class<?> clazz;

  public AnnotationHolder(Class<?> clazz) {
    this.isClass = true;
    this.clazz = clazz;
  }

  public AnnotationHolder(Field field, int fieldOrder) {
    this.isClass = false;
    this.field = field;
    this.fieldOrder = fieldOrder;
  }

  public boolean isClass() {
    return isClass;
  }

  public int getFieldOrder() {
    return fieldOrder;
  }

  public Field getField() {
    return field;
  }

  public Class<?> getClazz() {
    return clazz;
  }

  @Override
  public String toString() {
    return "AnnotationHolder{"
        + "field="
        + field
        + ", fieldOrder="
        + fieldOrder
        + ", isClass="
        + isClass
        + '}';
  }

  @Override
  public int compareTo(AnnotationHolder o) {
    if (isClass) {
      return -1;
    } else if (o.isClass()) {
      return 1;
    } else {
      return Integer.compare(o.getFieldOrder(), fieldOrder);
    }
  }
}
