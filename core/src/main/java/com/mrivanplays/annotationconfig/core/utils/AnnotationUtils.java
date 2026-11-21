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

package com.mrivanplays.annotationconfig.core.utils;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.RawConfig;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comments;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.resolver.CommentPlaceholderRegistry;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * A utility class with utilities about AnnotationConfig's default annotations.
 *
 * @author Ivan Pekov
 * @since 2.1.0
 */
@NullMarked
public final class AnnotationUtils {

  /**
   * Returns the key of the specified {@link Field}
   *
   * @param field the field you want the key of
   * @return the key, always a non-null value
   */
  public static String getKey(Field field) {
    Objects.requireNonNull(field, "field");
    if (field.isAnnotationPresent(Key.class)) {
      return field.getDeclaredAnnotation(Key.class).value();
    }
    return field.getName();
  }

  /**
   * Returns whether the specified {@link Field} is ignored upon generation.
   *
   * @param field the field you want to check if ignored
   * @return true or false
   */
  public static boolean isIgnored(Field field) {
    Objects.requireNonNull(field, "field");
    return field.isAnnotationPresent(Ignore.class);
  }

  /**
   * Returns whether the specified {@link Field} is a config object.
   *
   * @param field the field you want to check if config object
   * @return true or false
   */
  public static boolean isConfigObject(Field field) {
    Objects.requireNonNull(field, "field");
    return field.isAnnotationPresent(ConfigObject.class);
  }

  /**
   * Returns whether the specified {@link Field} is an accessor of the raw config.
   *
   * <p>The raw config in the terminology of AnnotationConfig is the raw values, read from the
   * config.
   *
   * @param field the field you want to check if raw config
   * @return true or false
   */
  public static boolean isRawConfigAccess(Field field) {
    Objects.requireNonNull(field, "field");
    return field.isAnnotationPresent(RawConfig.class);
  }

  /**
   * Returns whether the specified {@link Field} has been annotated with the {@code @Min}
   * annotation.
   *
   * @param field field to check
   * @return true if present, false otherwise
   */
  public static boolean hasMinimumRequirement(Field field) {
    Objects.requireNonNull(field, "field");
    return field.isAnnotationPresent(Min.class);
  }

  /**
   * Returns whether the specified {@link Field} has been annotated with the {@code @Max}
   * annotation.
   *
   * @param field field to check
   * @return true if present, false otherwise
   */
  public static boolean hasMaximumRequirement(Field field) {
    Objects.requireNonNull(field, "field");
    return field.isAnnotationPresent(Max.class);
  }

  /**
   * Returns the comments of the specified {@link Field}, and does not apply placeholders.
   *
   * @param field the field you want to get the comments of
   * @return comments, either an empty list or a list with comments
   * @deprecated use {@link #getComments(AnnotationAccessor)}
   */
  @Deprecated(since = "3.0.4")
  public static List<String> getComments(Field field) {
    Objects.requireNonNull(field, "field");
    return getComments(AnnotationAccessor.createFromField(field), false);
  }

  /**
   * Returns the comments of the specified {@link AnnotationAccessor annotation accessor}, and does
   * not apply placeholders.
   *
   * @param annotationAccessor the annotation accessor for which the comments to get
   * @return comments, either an empty list or list with comments
   * @since 3.0.4
   */
  public static List<String> getComments(AnnotationAccessor annotationAccessor) {
    return getComments(annotationAccessor, false);
  }

  /**
   * Returns the comments of the specified {@link AnnotationAccessor annotation accessor}.
   *
   * @param annotationAccessor the annotation accessor for which the comments to get
   * @param applyPlaceholders whether to apply placeholders
   * @return comments, either an empty list or list with comments
   * @since 3.0.4
   */
  public static List<String> getComments(
      AnnotationAccessor annotationAccessor, boolean applyPlaceholders) {
    Objects.requireNonNull(annotationAccessor, "annotationAccessor");
    if (!annotationAccessor.hasAnnotation(AnnotationType.COMMENT)
        && !annotationAccessor.hasAnnotation(AnnotationType.COMMENTS)) {
      return Collections.emptyList();
    }
    // optional abomination
    return annotationAccessor
        .getAnnotation(Comment.class)
        .map(comment ->
            List.of(
                applyPlaceholders
                    ? CommentPlaceholderRegistry.INSTANCE.applyPlaceholders(comment)
                    : comment.value()))
        .orElseGet(() ->
            annotationAccessor
                .getAnnotation(Comments.class)
                .map(comments -> {
                  if (applyPlaceholders) {
                    return CommentPlaceholderRegistry.INSTANCE.applyPlaceholders(comments);
                  }
                  return Arrays.stream(comments.value()).map(Comment::value).toList();
                })
                .orElse(Collections.emptyList()));
  }

  private AnnotationUtils() {
    throw new IllegalArgumentException("Initialization of utility-type class");
  }
}
