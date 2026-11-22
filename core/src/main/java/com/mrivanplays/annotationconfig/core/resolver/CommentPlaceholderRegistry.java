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

package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comments;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a registry for placeholders for {@link
 * com.mrivanplays.annotationconfig.core.annotations.comment.Comment comments} content. These are
 * applied before the comments are pushed to the {@link ValueWriter}.
 *
 * <p>Placeholders are applied in the order they were registered, and there is at most one
 * placeholder per replaced text: registering a placeholder that {@link
 * CommentPlaceholder#matches(CommentPlaceholder) matches} a registered one replaces it.
 *
 * <p>The registry is safe to use from several threads.
 *
 * @since 3.0.1
 * @author Ivan Pekov
 */
@NullMarked
public enum CommentPlaceholderRegistry {
  INSTANCE;

  // Applying placeholders is frequent and changing them rare, so the list is copy-on-write:
  // applying iterates a snapshot without locking. Changes lock on the list, so that finding an
  // entry and changing it is one step.
  private final List<CommentPlaceholder> placeholders = new CopyOnWriteArrayList<>();

  /**
   * Register a placeholder. <br>
   * Since this is a very limiting API, newer AnnotationConfig versions provide a {@link
   * CommentPlaceholder comment placeholder} object with an appropriate method {@link
   * #registerPlaceholder(CommentPlaceholder) to register}. Migration to using that is
   * <b>strongly</b> encouraged.
   *
   * @param toReplace value to replace
   * @param replacement replacement value
   * @deprecated use {@link #registerPlaceholder(CommentPlaceholder)}
   */
  @Deprecated(since = "3.1.0")
  public void registerPlaceholder(String toReplace, String replacement) {
    this.registerPlaceholder(CommentPlaceholder.simple(toReplace, replacement));
  }

  /**
   * Register a new {@link CommentPlaceholder comment placeholder}. If a registered placeholder
   * {@link CommentPlaceholder#matches(CommentPlaceholder) matches} it, that placeholder is replaced
   * and the new one takes its place in the order placeholders are applied in.
   *
   * @param placeholder placeholder to register
   * @since 3.1.0
   */
  public void registerPlaceholder(CommentPlaceholder placeholder) {
    Objects.requireNonNull(placeholder, "placeholder");
    synchronized (this.placeholders) {
      for (int i = 0; i < this.placeholders.size(); i++) {
        if (this.placeholders.get(i).matches(placeholder)) {
          this.placeholders.set(i, placeholder);
          return;
        }
      }
      this.placeholders.add(placeholder);
    }
  }

  /**
   * Unregister a placeholder
   *
   * @param placeholder placeholder to unregister
   * @since 3.1.0
   */
  public void unregisterPlaceholder(CommentPlaceholder placeholder) {
    Objects.requireNonNull(placeholder, "placeholder");
    synchronized (this.placeholders) {
      this.placeholders.removeIf(registered -> registered.matches(placeholder));
    }
  }

  /**
   * Check whether a placeholder has already been registered.
   *
   * @param placeholder placeholder to check
   * @return true if registered, false otherwise
   * @since 3.1.0
   */
  public boolean hasPlaceholder(CommentPlaceholder placeholder) {
    Objects.requireNonNull(placeholder, "placeholder");
    for (CommentPlaceholder registered : this.placeholders) {
      if (registered.equals(placeholder) || registered.matches(placeholder)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a new mutable list with the inputted {@code comments}, with all the registered
   * placeholders applied.
   *
   * @param comments comments to apply placeholders to
   * @return comments with applied placeholders
   */
  public List<String> applyPlaceholders(List<String> comments) {
    Objects.requireNonNull(comments, "comments");
    // always a copy, also when there is nothing to replace, so the input is never handed back
    List<String> ret = new ArrayList<>(comments.size());
    for (String comment : comments) {
      ret.add(this.applyPlaceholders(comment));
    }
    return ret;
  }

  /**
   * Applies (if there are any) placeholders to the specified {@link Comments comments} and maps to
   * a new mutable {@link List} of the comment values.
   *
   * @param annotation bulk representation of the {@link Comment comment} annotation
   * @return list of parsed comments
   * @since 3.1.0
   */
  public List<String> applyPlaceholders(Comments annotation) {
    Objects.requireNonNull(annotation, "annotation");
    List<String> ret = new ArrayList<>(annotation.value().length);
    for (Comment comment : annotation.value()) {
      ret.add(this.applyPlaceholders(comment.value()));
    }
    return ret;
  }

  /**
   * Helper method, accepting the {@link Comment comment} annotation, to parse placeholders on a
   * single comment.
   *
   * @param annotation the comment annotation
   * @return comment with parsed placeholders
   * @since 3.1.0
   */
  public String applyPlaceholders(Comment annotation) {
    Objects.requireNonNull(annotation, "annotation");
    return this.applyPlaceholders(annotation.value());
  }

  /**
   * Applies (if there are any) placeholders to the specified comment, in the order they were
   * registered.
   *
   * @param comment comment to apply placeholders to
   * @return comment with parsed placeholders
   * @since 3.1.0
   */
  public String applyPlaceholders(String comment) {
    Objects.requireNonNull(comment, "comment");
    String ret = comment;
    for (CommentPlaceholder placeholder : this.placeholders) {
      ret = placeholder.apply(ret);
    }
    return ret;
  }
}
