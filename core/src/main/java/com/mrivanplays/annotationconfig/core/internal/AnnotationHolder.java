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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A commented class or a field of a config, in the order they are written: the classes first, the
 * class a config extends before the config's class (see {@code FIND_PARENT_FIELDS}), then the
 * fields by descending field order.
 *
 * @hidden
 */
@NullMarked
final class AnnotationHolder implements Comparable<AnnotationHolder> {
  @Nullable private final FieldInfo info;
  private final int fieldOrder;
  @Nullable private final Class<?> clazz;
  private final int classOrder;

  /**
   * @param classOrder how many classes up from the config's class {@code clazz} is, negated: 0 for
   *     the config's class, -1 for the class it extends, and so on, so a class sorts before the
   *     classes that extend it. The class holders of one set have different orders.
   */
  public AnnotationHolder(Class<?> clazz, int classOrder) {
    this.info = null;
    this.fieldOrder = 0;
    this.clazz = clazz;
    this.classOrder = classOrder;
  }

  public AnnotationHolder(FieldInfo info, int fieldOrder) {
    this.info = info;
    this.fieldOrder = fieldOrder;
    this.clazz = null;
    this.classOrder = 0;
  }

  public boolean isClass() {
    return this.clazz != null;
  }

  /** The field of a field holder. */
  public Field getField() {
    return getInfo().field();
  }

  /** The field of a field holder. */
  public FieldInfo getInfo() {
    if (this.info == null) {
      throw new IllegalStateException("Not a field holder: " + this);
    }
    return this.info;
  }

  /** The class of a class holder. */
  public Class<?> getClazz() {
    if (this.clazz == null) {
      throw new IllegalStateException("Not a class holder: " + this);
    }
    return this.clazz;
  }

  @Override
  public String toString() {
    return this.clazz != null
        ? "AnnotationHolder{class=" + this.clazz.getName() + ", classOrder=" + this.classOrder + '}'
        : "AnnotationHolder{field=" + getField() + ", fieldOrder=" + this.fieldOrder + '}';
  }

  @Override
  public int compareTo(AnnotationHolder o) {
    boolean isClass = isClass();
    if (isClass && o.isClass()) {
      // the class a config extends before the config's class
      return Integer.compare(this.classOrder, o.classOrder);
    }
    if (isClass) {
      return -1;
    }
    if (o.isClass()) {
      return 1;
    }
    // descending: with SHOULD_REVERSE_FIELDS this gives declaration order, without it the reverse
    return Integer.compare(o.fieldOrder, this.fieldOrder);
  }
}
