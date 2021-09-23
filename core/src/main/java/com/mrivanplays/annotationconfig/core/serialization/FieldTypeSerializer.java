package com.mrivanplays.annotationconfig.core.serialization;

import java.lang.reflect.Field;

/**
 * Interface representing a custom serializer and deserializer of a field type. You should write a
 * custom one if you are not happy with how AnnotationConfig serializes and deserializes by default.
 * You will also need to register your newly created class, implementing this interface, through
 * {@link
 * com.mrivanplays.annotationconfig.core.serialization.registry.SerializerRegistry#registerSerializer(Class,
 * FieldTypeSerializer)}
 *
 * @param <T> type for which this serializer is being registered.
 * @since 2.0.0
 * @author MrIvanPlays
 */
public interface FieldTypeSerializer<T> {

  /**
   * AnnotationConfig invokes this call-back method during deserialization when it encounters a
   * field of the specified type.
   *
   * @param data the data we received from the config
   * @param field the field we will attach this information to later on
   * @return the generic value, the implementation of this interface has specified
   */
  T deserialize(ConfigDataObject data, Field field);

  /**
   * AnnotationConfig invokes this call-back method during serialization when it encounters a field
   * of the specified type.
   *
   * @param value the data we need serialized
   * @param field the field we got the value from
   * @return a serialized object which is useful for dumping into a configuration file
   */
  SerializedObject serialize(T value, Field field);
}
