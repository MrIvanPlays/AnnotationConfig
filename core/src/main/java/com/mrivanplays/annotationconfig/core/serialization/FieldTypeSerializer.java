package com.mrivanplays.annotationconfig.core.serialization;

/**
 * Interface representing a custom serializer and deserializer of a field type. You should write a
 * custom one if you are not happy with how AnnotationConfig serializes and deserializes by default.
 * You will also need to register your newly created class, implementing this interface, through
 * {@link SerializerRegistry#registerSerializer(Class, FieldTypeSerializer)}
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
   * @param context serialization context
   * @param annotations a way to access annotations of the object bound to the deserialized data
   * @return the generic value, the implementation of this interface has specified
   * @see SerializationContext
   * @see AnnotationAccessor
   */
  T deserialize(DataObject data, SerializationContext<T> context, AnnotationAccessor annotations);

  /**
   * AnnotationConfig invokes this call-back method during serialization when it encounters a field
   * of the specified type.
   *
   * @param value the data we need serialized
   * @param context serialization context
   * @param annotations a way to access annotations of the object bound to the serialized data
   * @return a serialized object which is useful for dumping into a configuration file
   * @see SerializationContext
   * @see AnnotationAccessor
   */
  DataObject serialize(T value, SerializationContext<T> context, AnnotationAccessor annotations);
}
