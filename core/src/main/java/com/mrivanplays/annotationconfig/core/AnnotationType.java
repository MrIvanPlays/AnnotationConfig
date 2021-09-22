package com.mrivanplays.annotationconfig.core;

import java.lang.annotation.Annotation;

/** Represents an annotation type, holding utility methods for the held raw annotation type. */
public final class AnnotationType {

  public static final AnnotationType COMMENT = new AnnotationType(Comment.class);
  public static final AnnotationType COMMENTS = new AnnotationType(Comments.class);
  public static final AnnotationType TYPE_RESOLVER = new AnnotationType(TypeResolver.class);
  public static final AnnotationType KEY = new AnnotationType(Key.class);
  public static final AnnotationType CONFIG_OBJECT = new AnnotationType(ConfigObject.class);
  public static final AnnotationType RETRIEVE = new AnnotationType(Retrieve.class);

  /**
   * Returns whether or not the annotation type specified is custom.
   *
   * @param type the type you want to check if its custom
   * @return boolean value, representing the outcome of the check.
   */
  public static boolean isCustom(AnnotationType type) {
    return !COMMENT.is(type)
        && !COMMENTS.is(type)
        && !TYPE_RESOLVER.is(type)
        && !KEY.is(type)
        && !CONFIG_OBJECT.is(type)
        && !RETRIEVE.is(type);
  }

  /**
   * Tries to match the annotation raw type to the built in annotation types.
   *
   * <p>If you're searching for a way to also match custom annotation types, use {@link
   * #match(Class, CustomAnnotationRegistry)}
   *
   * @param anno annotation raw type
   * @return matched annotation type, or null
   */
  public static AnnotationType match(Class<? extends Annotation> anno) {
    return COMMENT.is(anno)
        ? COMMENT
        : COMMENTS.is(anno)
            ? COMMENTS
            : KEY.is(anno)
                ? KEY
                : TYPE_RESOLVER.is(anno)
                    ? TYPE_RESOLVER
                    : CONFIG_OBJECT.is(anno) ? CONFIG_OBJECT : RETRIEVE.is(anno) ? RETRIEVE : null;
  }

  /**
   * Tries to match the annotation raw type to the annotation types in the {@link
   * CustomAnnotationRegistry} and the built in annotation types.
   *
   * @param anno annotation raw type
   * @param annoRegistry searched registry
   * @return matched annotation type, or null
   */
  public static AnnotationType match(
      Class<? extends Annotation> anno, CustomAnnotationRegistry annoRegistry) {
    if (annoRegistry == null || annoRegistry.registryMap().isEmpty()) {
      return match(anno);
    }
    for (AnnotationType type : annoRegistry.registryMap().keySet()) {
      if (type.is(anno)) {
        return type;
      }
    }

    return match(anno);
  }

  private final Class<? extends Annotation> rawType;

  public AnnotationType(Class<? extends Annotation> rawType) {
    this.rawType = rawType;
  }

  /**
   * Returns the raw type held by this annotation type.
   *
   * @return raw type
   */
  public Class<? extends Annotation> getRawType() {
    return rawType;
  }

  /**
   * Returns whether or not the specified raw annotation type exactly matches the held raw
   * annotation type in this instance.
   *
   * @param anno raw annotation type
   * @return boolean value, representing the outcome of this check
   */
  public boolean is(Class<? extends Annotation> anno) {
    return anno.isAssignableFrom(rawType);
  }

  /**
   * @param type annotation type
   * @return boolean value, representing the outcome of this check
   * @see #is(Class)
   */
  public boolean is(AnnotationType type) {
    return is(type.rawType);
  }

  @Override
  public String toString() {
    return "AnnotationType{" + "rawType=" + rawType.getSimpleName() + '}';
  }
}
