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

package com.mrivanplays.annotationconfig.core.annotations.type;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.ConfigVersion;
import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.annotations.OneOf;
import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import com.mrivanplays.annotationconfig.core.annotations.RawConfig;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comments;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

/**
 * Represents an annotation type, holding utility methods for the held raw annotation type.
 *
 * @author Ivan Pekov
 * @since 2.0.0
 */
@NullMarked
public enum AnnotationType {
  COMMENT(Comment.class),
  COMMENTS(Comments.class),
  KEY(Key.class),
  CONFIG_OBJECT(ConfigObject.class),
  IGNORE(Ignore.class),
  MAX(Max.class),
  MIN(Min.class),
  RAW_CONFIG(RawConfig.class),
  MULTILINE(Multiline.class),
  POLYMORPHIC(Polymorphic.class),
  CONFIG_VERSION(ConfigVersion.class),
  ONE_OF(OneOf.class);

  private static final Map<Class<? extends Annotation>, AnnotationType> BY_ANNOTATION_CLASS =
      new HashMap<>();

  static {
    for (AnnotationType type : AnnotationType.values()) {
      BY_ANNOTATION_CLASS.put(type.getRawType(), type);
    }
  }

  private final Class<? extends Annotation> annotationType;

  AnnotationType(Class<? extends Annotation> annotationType) {
    this.annotationType = annotationType;
  }

  /**
   * Returns an {@link Optional} value, which may contain the {@link AnnotationType} matched to the
   * specified {@code annotationType}.
   *
   * @param annotationType the annotation type wanted
   * @return annotation type or an empty optional
   */
  public static Optional<AnnotationType> match(Class<? extends Annotation> annotationType) {
    return Optional.ofNullable(BY_ANNOTATION_CLASS.get(annotationType));
  }

  /**
   * Returns the raw annotation type, held by this {@link AnnotationType}.
   *
   * @return raw annotation class
   */
  public Class<? extends Annotation> getRawType() {
    return annotationType;
  }

  /**
   * Returns if the raw annotation type specified meets this {@link AnnotationType}.
   *
   * @param anno the raw annotation type you want to check
   * @return boolean value, representing the outcome of this check
   */
  public boolean is(Class<? extends Annotation> anno) {
    Objects.requireNonNull(anno, "anno");
    // annotation types can't extend each other
    return anno == annotationType;
  }

  /**
   * Returns if the specified annotation type is this one.
   *
   * @param type annotation type
   * @return boolean value, representing the outcome of this check
   * @see #is(Class)
   */
  public boolean is(AnnotationType type) {
    Objects.requireNonNull(type, "type");
    return this == type;
  }

  @Override
  public String toString() {
    return "AnnotationType{" + "rawType=" + annotationType.getSimpleName() + '}';
  }
}
