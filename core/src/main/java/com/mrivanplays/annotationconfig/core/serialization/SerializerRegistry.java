package com.mrivanplays.annotationconfig.core.serialization;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Represents a registry of all the serializers.
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
public enum SerializerRegistry {
  INSTANCE;

  private static final DefaultSerializer DEFAULT = new DefaultSerializer();

  private Map<Type, FieldTypeSerializer<?>> serializers;

  SerializerRegistry() {
    this.serializers = new ConcurrentHashMap<>();
  }

  /**
   * Registers a new serializer.
   *
   * @param serializedType the field type that should be serialized
   * @param typeSerializer the serializer of the serializedType
   * @param <T> generic
   * @throws IllegalArgumentException if a serializer for this type has been already registered
   */
  public <T> void registerSerializer(
      Class<T> serializedType, FieldTypeSerializer<T> typeSerializer) {
    if (this.serializers.containsKey(serializedType)) {
      throw new IllegalArgumentException(
          "Serializer for " + serializedType.getName() + " already registered");
    }
    this.serializers.put(serializedType, typeSerializer);
  }

  /**
   * Registers a new serializer.
   *
   * @param serializedType the generic field type that should be serialized
   * @param typeSerializer the serializer of the serialized type
   * @throws IllegalArgumentException if a serializer for this type has been already registered.
   */
  public void registerSerializer(Type serializedType, FieldTypeSerializer<?> typeSerializer) {
    if (this.serializers.containsKey(serializedType)) {
      throw new IllegalArgumentException(
          "Serializer for " + serializedType.getTypeName() + " already registered");
    }
    this.serializers.put(serializedType, typeSerializer);
  }

  /**
   * Register a new {@link FieldTypeSerializer} which can resolve data without a {@link
   * SerializationContext} and a {@link AnnotationAccessor}
   *
   * @param serializedType the field type that should be serialized
   * @param deserialize deserialization method of the serializedType
   * @param serialize serialization method of the serializedType
   * @param <T> generic
   */
  public <T> void registerSimpleSerializer(
      Class<T> serializedType,
      Function<DataObject, T> deserialize,
      Function<T, DataObject> serialize) {
    if (this.serializers.containsKey(serializedType)) {
      throw new IllegalArgumentException(
          "Serializer for " + serializedType.getName() + " already registered");
    }
    this.serializers.put(
        serializedType,
        new FieldTypeSerializer<T>() {

          @Override
          public T deserialize(
              DataObject data, SerializationContext<T> context, AnnotationAccessor annotations) {
            return deserialize.apply(data);
          }

          @Override
          public DataObject serialize(
              T value, SerializationContext<T> context, AnnotationAccessor annotations) {
            return serialize.apply(value);
          }
        });
  }

  /**
   * Unregisters the serializer of the specified serialized type
   *
   * @param serializedType the serialized type you want the serializer of unregistered
   * @throws IllegalArgumentException if there isn't a serializer for the type specified
   */
  public void unregisterSerializer(Class<?> serializedType) {
    if (!this.serializers.containsKey(serializedType)) {
      throw new IllegalArgumentException(
          "Cannot unregister "
              + serializedType.getName()
              + " because a serializer hasn't been registered.");
    }
    this.serializers.remove(serializedType);
  }

  /**
   * Unregisters the serializer of the specified serialized type
   *
   * @param serializedType the serialized type you want the serializer of unregistered
   * @throws IllegalArgumentException if there isn't a serializer for the type specified
   */
  public void unregisterSerializer(Type serializedType) {
    if (!this.serializers.containsKey(serializedType)) {
      throw new IllegalArgumentException(
          "Cannot unregister "
              + serializedType.getTypeName()
              + " because a serializer hasn't been registered.");
    }
    this.serializers.remove(serializedType);
  }

  /**
   * Returns whether the specified type has a serializer registered.
   *
   * @param serializedType the serialized type you want to check
   * @return a boolean value
   */
  public boolean hasSerializer(Class<?> serializedType) {
    return this.serializers.containsKey(serializedType);
  }

  /**
   * Returns whether the specified type has a serializer registered.
   *
   * @param serializedType the serialized type you want to check
   * @return a boolean value
   */
  public boolean hasSerializer(Type serializedType) {
    return this.serializers.containsKey(serializedType);
  }

  /**
   * Returns an {@link Optional} value, which may or may not be filled with a {@link
   * FieldTypeSerializer}, depending on if the {@code serializedType} has been registered or not.
   *
   * @param serializedType the type which is serialized you want the serializer of
   * @return an optional with value or an empty optional
   */
  public Optional<FieldTypeSerializer<?>> getSerializer(Class<?> serializedType) {
    return Optional.ofNullable(serializers.get(serializedType));
  }

  /**
   * Returns an {@link Optional} value, which may or may not be filled with a {@link
   * FieldTypeSerializer}, depending on if the {@code serializedType} has been registered or not.
   *
   * @param serializedType the type which is serialized you want the serializer of
   * @return an optional with value or an empty optional
   */
  public Optional<FieldTypeSerializer<?>> getSerializer(Type serializedType) {
    return Optional.ofNullable(serializers.get(serializedType));
  }

  /**
   * Returns the default serializer. This is used when there isn't a registered serializer available
   * for a specific value.
   *
   * @return default serializer
   */
  public FieldTypeSerializer<Object> getDefaultSerializer() {
    return DEFAULT;
  }
}
