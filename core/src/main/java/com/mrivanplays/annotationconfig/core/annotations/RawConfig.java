package com.mrivanplays.annotationconfig.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation, which can be put on a {@link java.lang.reflect.Field} with type {@link
 * com.mrivanplays.annotationconfig.core.serialization.DataObject} in order to inject the raw
 * configuration read.
 *
 * <p>The field annotated with this annotation shall not have any other annotations, otherwise
 * AnnotationConfig will throw an error.
 *
 * @since 2.1.0
 * @author MrIvanPlays
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RawConfig {}
