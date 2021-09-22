package com.mrivanplays.annotationconfig.core.serialization.registry;

import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a registry of all the serializers.
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
public final class SerializerRegistry {

  public static final SerializerRegistry INSTANCE = new SerializerRegistry();

  private Map<Class<?>, FieldTypeSerializer<?>> serializers;

  private SerializerRegistry() {
    this.serializers = new ConcurrentHashMap<>();
  }

  /**
   * Registers a new serializer.
   *
   * @param serializedType the field type that should be serialized
   * @param typeSerializer the serialized of the serializedType
   * @param <T> generic
   */
  public <T> void registerSerializer(
      Class<T> serializedType, FieldTypeSerializer<T> typeSerializer) {
    this.serializers.put(serializedType, typeSerializer);
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
