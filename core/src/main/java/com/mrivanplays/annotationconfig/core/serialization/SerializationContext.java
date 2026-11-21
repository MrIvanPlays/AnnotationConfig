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

package com.mrivanplays.annotationconfig.core.serialization;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a serialization context. This holds information about the {@link Field} that's being
 * (de)serialized, but since AnnotationConfig can not always get a {@link Field} instance, this
 * context class will hold information.
 *
 * @param <T> generic type of information that's being (de)serialized
 * @author Ivan Pekov
 * @since 3.0.0
 */
@SuppressWarnings({"unchecked", "OptionalUsedAsFieldOrParameterType"})
@NullMarked
public final class SerializationContext<T> {

  /**
   * Creates a new {@link SerializationContext} from a {@link Field}
   *
   * @param field field to create context from
   * @param annotatedConfig annotated config
   * @param <T> type
   * @return new serialization context
   * @throws IllegalAccessException see {@link Field#get(Object)}
   */
  public static <T> SerializationContext<T> fromField(Field field, Object annotatedConfig)
      throws IllegalAccessException {
    // todo: rework serialization context to support record types
    // since in records we cannot have a originated from annotatedConfig instance.
    Optional<T> def;
    // this is just a hacky workaround
    // see above
    try {
      def = Optional.ofNullable((T) field.get(annotatedConfig));
    } catch (IllegalArgumentException e) {
      def = Optional.empty();
    }
    return new SerializationContext<>(
        Optional.of(field.getName()),
        def,
        field.getType(),
        field.getGenericType(),
        annotatedConfig);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig annotatedConfig
   * @param <T> type
   * @return new serialization context
   */
  public static <T> SerializationContext<T> of(
      Class<?> classType, Type genericType, Object annotatedConfig) {
    return of(null, classType, genericType, annotatedConfig);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param def default value. can be null
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig annotatedConfig
   * @param <T> type
   * @return new serialization context
   */
  public static <T> SerializationContext<T> of(
      @Nullable T def, Class<?> classType, Type genericType, Object annotatedConfig) {
    return of(null, def, classType, genericType, annotatedConfig);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param name name of field. can be null
   * @param def default value. can be null
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig annotatedConfig
   * @param <T> type
   * @return new serialization context
   */
  public static <T> SerializationContext<T> of(
      @Nullable String name,
      @Nullable T def,
      Class<?> classType,
      Type genericType,
      Object annotatedConfig) {
    //noinspection NullableProblems
    return new SerializationContext<>(
        Optional.ofNullable(name),
        Optional.ofNullable(def),
        classType,
        genericType,
        annotatedConfig);
  }

  private final Optional<String> name;
  private final Optional<T> def;
  private final Class<?> classType;
  private final Type genericType;
  private final Object annotatedConfig;

  private SerializationContext(
      Optional<String> name,
      Optional<T> def,
      Class<?> classType,
      Type genericType,
      Object annotatedConfig) {
    this.name = name;
    this.def = def;
    this.classType = classType;
    this.genericType = genericType;
    this.annotatedConfig = annotatedConfig;
  }

  /**
   * Returns an {@link Optional} which may or may not be fulfilled with a name. In most of the
   * cases, this is a field name.
   *
   * @return name or empty optional
   */
  public Optional<String> getName() {
    return name;
  }

  /**
   * Returns an {@link Optional} which may or may not be fulfilled with a default value. In most of
   * the cases, this is fulfilled.
   *
   * @return default value or empty optional
   */
  public Optional<T> getDefaultValue() {
    return def;
  }

  /**
   * Returns the {@link Class} type of the (de)serialized object.
   *
   * @return class type
   */
  public Class<?> getClassType() {
    return classType;
  }

  /**
   * Returns the generic {@link Type} of the (de)serialized object.
   *
   * @return generic type
   */
  public Type getGenericType() {
    return genericType;
  }

  /**
   * Returns the annotated config this context originated from.
   *
   * @return annotated config
   */
  public Object getAnnotatedConfig() {
    return annotatedConfig;
  }
}
