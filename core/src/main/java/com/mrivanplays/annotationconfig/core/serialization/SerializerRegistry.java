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

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a registry of all the serializers.
 *
 * <p>The registry is safe to use from several threads: every method is atomic.
 *
 * @since 2.0.0
 * @author Ivan Pekov
 */
@SuppressWarnings("unchecked")
@NullMarked
public enum SerializerRegistry {
  INSTANCE;

  // a Class is a Type, so the Class and Type overloads share their entries
  private final Map<Type, FieldTypeSerializer<?>> serializers = new ConcurrentHashMap<>();

  private static <T> @Nullable T defaultDeserialize(
      DataObject data, SerializationContext<T> context, AnnotationAccessor annotations) {
    return (T)
        DefaultSerializer.INSTANCE.deserialize(
            data, (SerializationContext<Object>) context, annotations);
  }

  private static <T> DataObject defaultSerialize(
      T value, SerializationContext<T> context, AnnotationAccessor annotations) {
    return DefaultSerializer.INSTANCE.serialize(
        value, (SerializationContext<Object>) context, annotations);
  }

  /**
   * Registers a new serializer.
   *
   * @param serializedType the field type that should be serialized
   * @param typeSerializer the serializer of the serializedType
   * @param <T> the serialized type
   * @throws IllegalArgumentException if a serializer for this type has been already registered
   */
  public <T> void registerSerializer(
      Class<T> serializedType, FieldTypeSerializer<T> typeSerializer) {
    this.registerSerializer((Type) serializedType, typeSerializer);
  }

  /**
   * Registers a new serializer.
   *
   * @param serializedType the generic field type that should be serialized
   * @param typeSerializer the serializer of the serialized type
   * @throws IllegalArgumentException if a serializer for this type has been already registered.
   */
  public void registerSerializer(Type serializedType, FieldTypeSerializer<?> typeSerializer) {
    Objects.requireNonNull(serializedType, "serializedType");
    Objects.requireNonNull(typeSerializer, "typeSerializer");
    // putIfAbsent: checking and registering in one step, so two threads can't both register
    if (this.serializers.putIfAbsent(serializedType, typeSerializer) != null) {
      throw new IllegalArgumentException(
          "Serializer for " + serializedType.getTypeName() + " already registered");
    }
  }

  /**
   * Register a new {@link FieldTypeSerializer} which can resolve data without a {@link
   * SerializationContext} and an {@link AnnotationAccessor}.
   *
   * @param serializedType the field type that should be serialized
   * @param deserialize deserialization method of the serializedType
   * @param serialize serialization method of the serializedType
   * @param <T> the serialized type
   * @throws IllegalArgumentException if a serializer for this type has been already registered
   */
  public <T> void registerSimpleSerializer(
      Class<T> serializedType,
      Function<DataObject, T> deserialize,
      Function<T, DataObject> serialize) {
    Objects.requireNonNull(serializedType, "serializedType");
    Objects.requireNonNull(deserialize, "deserialize");
    Objects.requireNonNull(serialize, "serialize");
    this.registerSerializer(
        serializedType,
        new ContextFreeFieldTypeSerializer<T>() {
          @Override
          public @Nullable T deserializeSimple(DataObject data) {
            return deserialize.apply(data);
          }

          @Override
          public DataObject serializeSimple(T value) {
            return serialize.apply(value);
          }
        });
  }

  // The default serializer with the caller's context, so the settings, the generic type and the
  // default value are kept, and a value without an annotated config (e.g. a record component)
  // works as well.

  /**
   * Registers a new {@link FieldTypeSerializer} with only a serialize method which can resolve
   * without a {@link SerializationContext} and an {@link AnnotationAccessor}.<br>
   * How is the deserialize method handled? If there is a registered serializer for this {@code
   * serializedType}, then the method will forward deserialize calls to it, otherwise deserialize
   * calls are forwarded to the default serializer.
   *
   * <p>Unlike {@link #registerSerializer(Class, FieldTypeSerializer)}, this does not throw when a
   * serializer is registered for the type already: it replaces that serializer, and keeps its
   * deserialize method.
   *
   * @param serializedType the field type that should be serialized
   * @param serialize serialization method of the serializedType
   * @param <T> the serialized type
   */
  public <T> void registerSimpleValueSerializer(
      Class<T> serializedType, Function<T, DataObject> serialize) {
    Objects.requireNonNull(serializedType, "serializedType");
    Objects.requireNonNull(serialize, "serialize");
    // compute replaces the entry in one step: there is no moment without a serializer, and
    // concurrent calls for the same type are applied one after the other
    this.serializers.compute(
        serializedType,
        (_, existing) ->
            FieldTypeSerializer.<T>functional(
                existing != null
                    ? ((FieldTypeSerializer<T>) existing).deserializeAsFunction()
                    : SerializerRegistry::defaultDeserialize,
                (value, _, _) -> serialize.apply(value)));
  }

  /**
   * Registers a new {@link FieldTypeSerializer} with only a deserialize method which can resolve
   * without a {@link SerializationContext} and an {@link AnnotationAccessor}.<br>
   * How is the serialize method handled? If there is a registered serializer for this {@code
   * serializedType}, then the method will forward serialize calls to it, otherwise serialize calls
   * are forwarded to the default serializer.
   *
   * <p>Unlike {@link #registerSerializer(Class, FieldTypeSerializer)}, this does not throw when a
   * serializer is registered for the type already: it replaces that serializer, and keeps its
   * serialize method.
   *
   * @param serializedType the field type that should be deserialized
   * @param deserialize deserialization method of the serialized type
   * @param <T> the serialized type
   */
  public <T> void registerSimpleValueDeserializer(
      Class<T> serializedType, Function<DataObject, T> deserialize) {
    Objects.requireNonNull(serializedType, "serializedType");
    Objects.requireNonNull(deserialize, "deserialize");
    this.serializers.compute(
        serializedType,
        (_, existing) ->
            FieldTypeSerializer.<T>functional(
                (data, _, _) -> deserialize.apply(data),
                existing != null
                    ? ((FieldTypeSerializer<T>) existing).serializeAsFunction()
                    : SerializerRegistry::defaultSerialize));
  }

  /**
   * Unregisters the serializer of the specified serialized type.
   *
   * @param serializedType the serialized type you want the serializer of unregistered
   * @throws IllegalArgumentException if there isn't a serializer for the type specified
   */
  public void unregisterSerializer(Class<?> serializedType) {
    this.unregisterSerializer((Type) serializedType);
  }

  /**
   * Unregisters the serializer of the specified serialized type.
   *
   * @param serializedType the serialized type you want the serializer of unregistered
   * @throws IllegalArgumentException if there isn't a serializer for the type specified
   */
  public void unregisterSerializer(Type serializedType) {
    Objects.requireNonNull(serializedType, "serializedType");
    if (this.serializers.remove(serializedType) == null) {
      throw new IllegalArgumentException(
          "Cannot unregister "
              + serializedType.getTypeName()
              + " because a serializer hasn't been registered.");
    }
  }

  /**
   * Returns whether the specified type has a serializer registered.
   *
   * @param serializedType the serialized type you want to check
   * @return a boolean value
   */
  public boolean hasSerializer(Class<?> serializedType) {
    return this.hasSerializer((Type) serializedType);
  }

  /**
   * Returns whether the specified type has a serializer registered.
   *
   * @param serializedType the serialized type you want to check
   * @return a boolean value
   */
  public boolean hasSerializer(Type serializedType) {
    Objects.requireNonNull(serializedType, "serializedType");
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
    return this.getSerializer((Type) serializedType);
  }

  /**
   * Returns an {@link Optional} value, which may or may not be filled with a {@link
   * FieldTypeSerializer}, depending on if the {@code serializedType} has been registered or not.
   *
   * @param serializedType the type which is serialized you want the serializer of
   * @return an optional with value or an empty optional
   */
  public Optional<FieldTypeSerializer<?>> getSerializer(Type serializedType) {
    Objects.requireNonNull(serializedType, "serializedType");
    return Optional.ofNullable(this.serializers.get(serializedType));
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
