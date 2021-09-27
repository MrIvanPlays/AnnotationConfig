package com.mrivanplays.annotationconfig.core.serialization.registry;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * Represents a registry of all the serializers.
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
public enum SerializerRegistry {
  INSTANCE;

  private Map<Class<?>, FieldTypeSerializer<?>> serializers;

  SerializerRegistry() {
    this.serializers = new ConcurrentHashMap<>();
  }

  /**
   * Registers a new serializer.
   *
   * @param serializedType the field type that should be serialized
   * @param typeSerializer the serialized of the serializedType
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
   * @param serializedType the field type that should be serialized
   * @param deSerialize the deserialize method
   * @param serialize the serialize method
   * @param <T> generic
   */
  public <T> void registerSerializer(
      Class<T> serializedType,
      BiFunction<ConfigDataObject, Field, T> deSerialize,
      BiFunction<T, Field, SerializedObject> serialize) {
    this.registerSerializer(
        serializedType,
        new FieldTypeSerializer<T>() {
          @Override
          public T deserialize(ConfigDataObject data, Field field) {
            return deSerialize.apply(data, field);
          }

          @Override
          public SerializedObject serialize(T value, Field field) {
            return serialize.apply(value, field);
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
   * Returns whether the specified type has a serializer registered.
   *
   * @param serializedType the serialized type you want to check
   * @return a boolean value
   */
  public boolean hasSerializer(Class<?> serializedType) {
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
}
