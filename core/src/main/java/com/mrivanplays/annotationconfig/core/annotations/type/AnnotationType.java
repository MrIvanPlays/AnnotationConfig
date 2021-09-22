package com.mrivanplays.annotationconfig.core.annotations.type;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Retrieve;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comments;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Represents an annotation type, holding utility methods for the held raw annotation type. */
public enum AnnotationType {
  COMMENT(Comment.class),
  COMMENTS(Comments.class),
  KEY(Key.class),
  CONFIG_OBJECT(ConfigObject.class),
  RETRIEVE(Retrieve.class);

  private final Class<? extends Annotation> annotationType;

  private static Map<Class<? extends Annotation>, AnnotationType> BY_ANNOTATION_CLASS =
      new HashMap<>();

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
