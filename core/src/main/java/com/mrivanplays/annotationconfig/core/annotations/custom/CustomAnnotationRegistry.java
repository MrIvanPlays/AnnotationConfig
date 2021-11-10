package com.mrivanplays.annotationconfig.core.annotations.custom;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a registry for custom annotations.
 *
 * @author MrIvanPlays
 * @since 2.0.0
 */
public enum CustomAnnotationRegistry {
  INSTANCE;

  private Map<Class<? extends Annotation>, AnnotationValidator<? extends Annotation>> registry =
      new HashMap<>();

  /**
   * Binds the specified {@link AnnotationValidator} {@code validator} to the specified {@code
   * annotation}.
   *
   * @param annotation the annotation you want this validator to be bound to
   * @param validator the bind validator
   * @param <T> annotation type
   * @throws IllegalArgumentException if the annotation already has a validator.
   */
  public <T extends Annotation> void register(
      Class<T> annotation, AnnotationValidator<T> validator) {
    if (registry.containsKey(annotation)) {
      throw new IllegalArgumentException(
          "Custom annotation " + annotation.getName() + " already registered.");
    }
    registry.put(annotation, validator);
  }

  /**
   * Removes the bound to the specified {@code annotation} {@link AnnotationValidator} if there is
   * any.
   *
   * @param annotation the annotation you want to remove the validator of
   * @throws IllegalArgumentException if no validator was present for the specified annotation
   */
  public void unregister(Class<? extends Annotation> annotation) {
    if (!registry.containsKey(annotation)) {
      throw new IllegalArgumentException(
          "Custom annotation " + annotation.getName() + " already not registered.");
    }
    registry.remove(annotation);
  }

  /**
   * Returns whether the specified {@code annotation} has a validator.
   *
   * @param annotation the annotation you want to check for validator
   * @return true if there is a validator, false otherwise
   */
  public boolean hasValidator(Class<? extends Annotation> annotation) {
    return registry.containsKey(annotation);
  }

  /**
   * Returns whether the registry contains any validators.
   *
   * @return true if empty, false otherwise.
   */
  public boolean isEmpty() {
    return registry.isEmpty();
  }

  /**
   * Returns the {@link AnnotationValidator} bound to the specified {@code annotationClass}.
   *
   * @param annotationClass the annotation of which you want the validator
   * @return annotation validator optional, which can be empty
   */
  public Optional<AnnotationValidator<? extends Annotation>> getValidator(
      Class<? extends Annotation> annotationClass) {
    return Optional.ofNullable(registry.get(annotationClass));
  }
}
