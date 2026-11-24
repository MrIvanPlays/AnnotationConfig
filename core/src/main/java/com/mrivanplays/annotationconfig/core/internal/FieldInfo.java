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
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.resolver.CommentPlaceholderRegistry;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.utils.AnnotationUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * What AnnotationConfig needs to know about a field, read once per field by {@link ClassInfo}
 * instead of on every dump and load: its key, annotations, types and comments.
 *
 * @hidden
 */
@NullMarked
public final class FieldInfo {

  private final Field field;
  private final String key;
  private final Type genericType;
  private final boolean ignored;
  private final boolean configObject;
  private final boolean rawConfig;
  private final boolean isStatic;
  @Nullable private final Min min;
  @Nullable private final Max max;
  // @Min or @Max on an element type: List<@Max(...) Integer>, or @Max(...) int[]
  private final boolean elementBounds;
  @Nullable private final Multiline multiline;
  private final Annotation[] annotations;
  private final AnnotationAccessor accessor;
  // without placeholders: the placeholders that are registered can change between dumps
  private final List<String> comments;
  // set once the field is accessible; written by any thread, as setAccessible may be repeated
  private volatile boolean accessible;

  FieldInfo(Field field) {
    this.field = field;
    this.key = AnnotationUtils.getKey(field);
    this.genericType = field.getGenericType();
    this.ignored = AnnotationUtils.isIgnored(field);
    this.configObject = AnnotationUtils.isConfigObject(field);
    this.rawConfig = AnnotationUtils.isRawConfigAccess(field);
    this.isStatic = Modifier.isStatic(field.getModifiers());
    this.min = field.getDeclaredAnnotation(Min.class);
    this.max = field.getDeclaredAnnotation(Max.class);
    this.elementBounds = MinMaxValidator.hasElementBounds(field.getAnnotatedType());
    this.multiline = field.getDeclaredAnnotation(Multiline.class);
    this.annotations = field.getDeclaredAnnotations();
    this.accessor = AnnotationAccessor.createFromField(field);
    this.comments = List.copyOf(AnnotationUtils.getComments(this.accessor, false));
    // an inaccessible field fails with the reason in ensureAccessible(), when it is used
    this.accessible = field.trySetAccessible();
  }

  static List<String> applyPlaceholders(List<String> comments) {
    return comments.isEmpty() || !CommentPlaceholderRegistry.INSTANCE.hasPlaceholders()
        ? comments
        : CommentPlaceholderRegistry.INSTANCE.applyPlaceholders(comments);
  }

  public Field field() {
    return this.field;
  }

  public String name() {
    return this.field.getName();
  }

  public String key() {
    return this.key;
  }

  public Class<?> type() {
    return this.field.getType();
  }

  public Type genericType() {
    return this.genericType;
  }

  public boolean ignored() {
    return this.ignored;
  }

  public boolean configObject() {
    return this.configObject;
  }

  public boolean rawConfig() {
    return this.rawConfig;
  }

  public boolean isStatic() {
    return this.isStatic;
  }

  /**
   * Whether the field has {@code @Min} or {@code @Max}, on itself or on an element type, so its
   * value has to be validated.
   */
  public boolean minOrMax() {
    return this.min != null || this.max != null || this.elementBounds;
  }

  /** Whether {@code @Min} or {@code @Max} is on an element type of the field's type. */
  public boolean elementBounds() {
    return this.elementBounds;
  }

  public @Nullable Min min() {
    return this.min;
  }

  public @Nullable Max max() {
    return this.max;
  }

  public @Nullable Multiline multiline() {
    return this.multiline;
  }

  /** The field's annotations. Shared: don't modify the array. */
  public Annotation[] annotations() {
    return this.annotations;
  }

  public AnnotationAccessor accessor() {
    return this.accessor;
  }

  /** The comments with the registered placeholders applied. */
  public List<String> comments() {
    return applyPlaceholders(this.comments);
  }

  /** Makes the field accessible, throwing the reason if it can't be. */
  public Field ensureAccessible() {
    if (!this.accessible) {
      this.field.setAccessible(true);
      this.accessible = true;
    }
    return this.field;
  }
}
