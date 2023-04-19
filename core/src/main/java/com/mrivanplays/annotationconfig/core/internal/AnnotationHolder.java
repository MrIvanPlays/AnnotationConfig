package com.mrivanplays.annotationconfig.core.internal;

import java.lang.reflect.Field;

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
