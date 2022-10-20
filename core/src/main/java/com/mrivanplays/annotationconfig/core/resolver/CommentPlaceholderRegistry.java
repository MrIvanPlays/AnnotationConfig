package com.mrivanplays.annotationconfig.core.resolver;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Represents a registry for placeholders for {@link
 * com.mrivanplays.annotationconfig.core.annotations.comment.Comment comments} content. These are
 * applied before the comments are pushed to the {@link ValueWriter}.
 *
 * @since 3.0.1
 * @author MrIvanPlays
 */
public enum CommentPlaceholderRegistry {
  INSTANCE;

  private PlaceholderMap placeholderMap = new PlaceholderMap();

  /**
   * Register a placeholder.
   *
   * @param toReplace value to replace
   * @param replacement replacement value
   */
  public void registerPlaceholder(String toReplace, String replacement) {
    this.placeholderMap.put(toReplace, replacement);
  }

  /**
   * Returns a mutable {@link LinkedList} copy of the inputted {@code comments} with all the
   * registered placeholders applied.
   *
   * @param comments comments to apply placeholders to
   * @return comments with applied placeholders
   */
  public List<String> applyPlaceholders(List<String> comments) {
    if (this.placeholderMap.empty()) {
      return comments;
    }
    List<String> ret = new LinkedList<>();

    if (this.placeholderMap.size() == 1) {
      Entry<String, String> placeholder = this.placeholderMap.first();
      for (String comment : comments) {
        ret.add(comment.replace(placeholder.getKey(), placeholder.getValue()));
      }
      return ret;
    }

    if (this.placeholderMap.size() < comments.size()) {
      for (String comment : comments) {
        for (Entry<String, String> placeholder : this.placeholderMap) {
          ret.add(comment.replace(placeholder.getKey(), placeholder.getValue()));
        }
      }
    } else {
      for (Entry<String, String> placeholder : this.placeholderMap) {
        for (String comment : comments) {
          ret.add(comment.replace(placeholder.getKey(), placeholder.getValue()));
        }
      }
    }
    return ret;
  }
}
