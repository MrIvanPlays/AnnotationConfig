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

import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a serialization context: what is known about the value that's being (de)serialized.
 * AnnotationConfig can not always get the {@link Field} the value belongs to, so this context holds
 * the information instead.
 *
 * @param <T> generic type of information that's being (de)serialized
 * @author Ivan Pekov
 * @since 3.0.0
 */
@NullMarked
public final class SerializationContext<T> {

  @Nullable private final String name;
  @Nullable private final T def;
  private final Class<?> classType;
  private final Type genericType;
  @Nullable private final Object annotatedConfig;
  private final Settings settings;

  private SerializationContext(
      @Nullable String name,
      @Nullable T def,
      Class<?> classType,
      Type genericType,
      @Nullable Object annotatedConfig,
      @Nullable Settings settings) {
    this.name = name;
    this.def = def;
    this.classType = Objects.requireNonNull(classType, "classType");
    this.genericType = Objects.requireNonNull(genericType, "genericType");
    this.annotatedConfig = annotatedConfig;
    this.settings = Objects.requireNonNullElse(settings, ACDefaultSettings.getDefault());
  }

  /**
   * Creates a new {@link SerializationContext} from a {@link Field}
   *
   * @param field field to create context from
   * @param annotatedConfig annotated config
   * @param <T> type
   * @return new serialization context
   * @throws IllegalAccessException see {@link Field#get(Object)}
   */
  public static <T> SerializationContext<T> fromField(Field field, @Nullable Object annotatedConfig)
      throws IllegalAccessException {
    return fromField(field, annotatedConfig, null);
  }

  /**
   * Creates a new {@link SerializationContext} from a {@link Field}
   *
   * @param field field to create context from
   * @param annotatedConfig annotated config
   * @param settings used settings
   * @param <T> type
   * @return new serialization context
   * @since 3.1.0
   * @throws IllegalAccessException see {@link Field#get(Object)}
   */
  public static <T> SerializationContext<T> fromField(
      Field field, @Nullable Object annotatedConfig, @Nullable Settings settings)
      throws IllegalAccessException {
    Objects.requireNonNull(field, "field");
    T def = null;
    if (annotatedConfig != null) {
      //noinspection unchecked
      def = (T) field.get(annotatedConfig);
    }
    return new SerializationContext<>(
        field.getName(), def, field.getType(), field.getGenericType(), annotatedConfig, settings);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig the annotated config, if any
   * @param <T> type
   * @return new serialization context
   */
  public static <T> SerializationContext<T> of(
      Class<?> classType, Type genericType, @Nullable Object annotatedConfig) {
    return of(null, classType, genericType, annotatedConfig);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig the annotated config, if any
   * @param settings used settings
   * @param <T> type
   * @return new serialization context
   * @since 3.1.0
   */
  public static <T> SerializationContext<T> of(
      Class<?> classType,
      Type genericType,
      @Nullable Object annotatedConfig,
      @Nullable Settings settings) {
    return of(null, classType, genericType, annotatedConfig, settings);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param def default value. can be null
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig the annotated config, if any
   * @param <T> type
   * @return new serialization context
   */
  public static <T> SerializationContext<T> of(
      @Nullable T def, Class<?> classType, Type genericType, @Nullable Object annotatedConfig) {
    return of(null, def, classType, genericType, annotatedConfig);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param def default value. can be null
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig the annotated config, if any
   * @param settings used settings
   * @param <T> type
   * @return new serialization context
   * @since 3.1.0
   */
  public static <T> SerializationContext<T> of(
      @Nullable T def,
      Class<?> classType,
      Type genericType,
      @Nullable Object annotatedConfig,
      @Nullable Settings settings) {
    return of(null, def, classType, genericType, annotatedConfig, settings);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param name name of field. can be null
   * @param def default value. can be null
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig the annotated config, if any
   * @param <T> type
   * @return new serialization context
   */
  public static <T> SerializationContext<T> of(
      @Nullable String name,
      @Nullable T def,
      Class<?> classType,
      Type genericType,
      @Nullable Object annotatedConfig) {
    return of(name, def, classType, genericType, annotatedConfig, null);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param name name of field. can be null
   * @param def default value. can be null
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig the annotated config, if any
   * @param settings used settings
   * @param <T> type
   * @return new serialization context
   * @since 3.1.0
   */
  public static <T> SerializationContext<T> of(
      @Nullable String name,
      @Nullable T def,
      Class<?> classType,
      Type genericType,
      @Nullable Object annotatedConfig,
      @Nullable Settings settings) {
    return new SerializationContext<>(name, def, classType, genericType, annotatedConfig, settings);
  }

  /**
   * Returns an {@link Optional} which may or may not be fulfilled with a name. In most of the
   * cases, this is a field name.
   *
   * @return name or empty optional
   */
  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  /**
   * Returns an {@link Optional} which may or may not be fulfilled with a default value, usually the
   * current value of the field.
   *
   * @return default value or empty optional
   */
  public Optional<T> getDefaultValue() {
    return Optional.ofNullable(def);
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
   * Returns the annotated config this context originated from. Will always return {@code null} if
   * this context originated from a record-type class.
   *
   * @return annotated config
   */
  @Nullable
  public Object getAnnotatedConfig() {
    return annotatedConfig;
  }

  /**
   * Returns the used {@link Settings settings} for serializing/deserializing.
   *
   * @return used settings
   * @since 3.1.0
   */
  public Settings getSettings() {
    return this.settings;
  }
}
