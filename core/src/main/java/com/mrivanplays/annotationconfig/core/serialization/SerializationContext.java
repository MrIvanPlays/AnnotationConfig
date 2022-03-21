package com.mrivanplays.annotationconfig.core.serialization;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Represents a serialization context. This holds information about the {@link Field} that's being
 * (de)serialized, but since AnnotationConfig can not always get a {@link Field} instance, this
 * context class will hold information.
 *
 * @param <T> generic type of information that's being (de)serialized
 * @author MrIvanPlays
 * @since 3.0.0
 */
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
    return new SerializationContext<>(
        Optional.of(field.getName()),
        (T) field.get(annotatedConfig),
        field.getType(),
        field.getGenericType(),
        annotatedConfig);
  }

  /**
   * Creates a new {@link SerializationContext}
   *
   * @param name name of field. can be null
   * @param def default value
   * @param classType class type
   * @param genericType generic class type
   * @param annotatedConfig annotatedConfig
   * @param <T> type
   * @return new serialization context
   */
  public static <T> SerializationContext<T> of(
      String name, T def, Class<?> classType, Type genericType, Object annotatedConfig) {
    return new SerializationContext<>(
        Optional.ofNullable(name), def, classType, genericType, annotatedConfig);
  }

  private final Optional<String> name;
  private final T def;
  private final Class<?> classType;
  private final Type genericType;
  private final Object annotatedConfig;

  private SerializationContext(
      Optional<String> name, T def, Class<?> classType, Type genericType, Object annotatedConfig) {
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
   * Returns the default value for the (de)serialized object.
   *
   * @return default value
   */
  public T getDefaultValue() {
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
