package com.mrivanplays.annotationconfig.core.serialization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which can be used on objects you want serialized with the default serializer, but want
 * the serializer to use a constructor rather than to set the fields 1 by 1, so this constructor has
 * to be annotated with this annotation.
 *
 * @author MrIvanPlays
 * @since 2.0.0
 */
@Documented
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
public @interface DeserializeConstructor {}
