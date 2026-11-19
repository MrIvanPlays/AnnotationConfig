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
import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.annotations.RawConfig;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comments;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an annotation type, holding utility methods for the held raw annotation type.
 *
 * @author MrIvanPlays
 */
public enum AnnotationType {
  COMMENT(Comment.class),
  COMMENTS(Comments.class),
  KEY(Key.class),
  CONFIG_OBJECT(ConfigObject.class),
  IGNORE(Ignore.class),
  MAX(Max.class),
  MIN(Min.class),
  RAW_CONFIG(RawConfig.class),
  MULTILINE(Multiline.class);

  private final Class<? extends Annotation> annotationType;

  private static final Map<Class<? extends Annotation>, AnnotationType> BY_ANNOTATION_CLASS =
      new ConcurrentHashMap<>();

  static {
    for (AnnotationType type : AnnotationType.values()) {
      BY_ANNOTATION_CLASS.put(type.getRawType(), type);
    }
  }

  /**
   * Returns an {@link Optional} value, which may contain the {@link AnnotationType} matched to the
   * specified {@param annotationType}.
   *
   * @param annotationType the annotation type wanted
   * @return annotation type or an empty optional
   */
  public static Optional<AnnotationType> match(Class<? extends Annotation> annotationType) {
    return Optional.ofNullable(BY_ANNOTATION_CLASS.get(annotationType));
  }

  AnnotationType(Class<? extends Annotation> annotationType) {
    this.annotationType = annotationType;
  }

  /**
   * Returns the raw annotation type, held by this {@link AnnotationType}
   *
   * @return raw annotation class
   */
  public Class<? extends Annotation> getRawType() {
    return annotationType;
  }

  /**
   * Returns if the raw annotation type specified meets this {@link AnnotationType}
   *
   * @param anno the raw annotation type you want to check
   * @return boolean value, representing the outcome of this check
   */
  public boolean is(Class<? extends Annotation> anno) {
    return anno.isAssignableFrom(annotationType);
  }

  /**
   * @param type annotation type
   * @return boolean value, representing the outcome of this check
   * @see #is(Class)
   */
  public boolean is(AnnotationType type) {
    return is(type.getRawType());
  }

  @Override
  public String toString() {
    return "AnnotationType{" + "rawType=" + annotationType.getSimpleName() + '}';
  }
}
