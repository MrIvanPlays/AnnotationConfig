package com.mrivanplays.annotationconfig.core.utils;

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comments;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A utility class with utilities about AnnotationConfig's default annotations.
 *
 * @author MrIvanPlays
 * @since 2.1.0
 */
public final class AnnotationUtils {

  /**
   * Returns the key of the specified {@link Field}
   *
   * @param field the field you want the key of
   * @return the key, always a non-null value
   */
  public static String getKey(Field field) {
    String ret = field.getName();
    if (field.getDeclaredAnnotations().length > 0) {
      Key annotationKey = field.getDeclaredAnnotation(Key.class);
      if (annotationKey != null) {
        ret = annotationKey.value();
      }
    }
    return ret;
  }

  /**
   * Returns the comments of the specified {@link Field}
   *
   * @param field the field you want to get the comments of
   * @return comments, either an empty list or a list with comments
   */
  public static List<String> getComments(Field field) {
    List<String> ret = new ArrayList<>();
    if (field.getDeclaredAnnotations().length > 0) {
      for (Annotation annotation : field.getDeclaredAnnotations()) {
        Optional<AnnotationType> typeOpt = AnnotationType.match(annotation.annotationType());
        if (!typeOpt.isPresent()) {
          continue;
        }
        AnnotationType type = typeOpt.get();
        if (type.is(AnnotationType.COMMENT)) {
          ret.add(field.getDeclaredAnnotation(Comment.class).value());
        }
        if (type.is(AnnotationType.COMMENTS)) {
          ret.addAll(
              Arrays.stream(field.getDeclaredAnnotation(Comments.class).value())
                  .map(Comment::value)
                  .collect(Collectors.toList()));
        }
      }
    }
    return ret;
  }

  private AnnotationUtils() {
    throw new IllegalArgumentException("Initialization of utility-type class");
  }
}
