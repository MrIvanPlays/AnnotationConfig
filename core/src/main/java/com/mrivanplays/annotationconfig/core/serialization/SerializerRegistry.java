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
        FieldTypeSerializer.<T>functional(
            (data, context, annotations) -> deserialize.apply(data),
            (value, context, annotations) -> serialize.apply(value)));
  }

  /**
   * Registers a new {@link FieldTypeSerializer} with only a serialize method which can resolve
   * without a {@link SerializationContext} and a {@link AnnotationAccessor} .<br>
   * How is the deserialize method handled? If there is a registered serializer for ths {@code
   * serializedType}, then the method will forward deserialize calls to it, otherwise deserialize
   * calls are forwarded to the default serializer.
   *
   * @param serializedType the field type that should be serialized
   * @param serialize serialization method of the serializedType
   * @param <T> generic
   */
  public <T> void registerSimpleValueSerializer(
      Class<T> serializedType, Function<T, DataObject> serialize) {
    if (this.serializers.containsKey(serializedType)) {
      FieldTypeSerializer<T> serializer =
          (FieldTypeSerializer<T>) this.serializers.remove(serializedType);
      this.serializers.put(
          serializedType,
          FieldTypeSerializer.functional(
              serializer.deserializeAsFunction(),
              (value, context, annotations) -> serialize.apply(value)));
      return;
    }
    this.serializers.put(
        serializedType,
        FieldTypeSerializer.<T>functional(
            (data, context, annotations) ->
                SimpleValueSerializer.deserialize(
                    data, serializedType, context.getAnnotatedConfig()),
            (value, context, annotations) -> serialize.apply(value)));
  }

  /**
   * Registers a new {@link FieldTypeSerializer} with only a deserialize method which can resolve
   * without a {@link SerializationContext} and a {@link AnnotationAccessor}.<br>
   * How is the serialize method handled? If there is a registered serializer for this {@code
   * serializedType}, then the method will forward serialize calls to it, otherwise serialize calls
   * are forwarded to the default serializer.
   *
   * @param serializedType the field type that should be deserialized
   * @param deserialize deserialization method of the serialized type
   * @param <T> generic
   */
  public <T> void registerSimpleValueDeserializer(
      Class<T> serializedType, Function<DataObject, T> deserialize) {
    if (this.serializers.containsKey(serializedType)) {
      FieldTypeSerializer<T> serializer =
          (FieldTypeSerializer<T>) this.serializers.remove(serializedType);
      this.serializers.put(
          serializedType,
          FieldTypeSerializer.functional(
              (data, context, annotations) -> deserialize.apply(data),
              serializer.serializeAsFunction()));
      return;
    }
    this.serializers.put(
        serializedType,
        FieldTypeSerializer.<T>functional(
            (data, context, annotations) -> deserialize.apply(data),
            (value, context, annotations) ->
                SimpleValueSerializer.serialize(
                    value, serializedType, context.getAnnotatedConfig())));
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
    return DefaultSerializer.INSTANCE;
  }
}
