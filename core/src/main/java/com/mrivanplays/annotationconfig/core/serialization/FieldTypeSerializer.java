package com.mrivanplays.annotationconfig.core.serialization;

import java.lang.reflect.Field;

// todo: javadoc
public interface FieldTypeSerializer<T> {

  T deserialize(ConfigDataObject data, Field field) throws Exception;

  SerializedObject serialize(T value, Field field) throws Exception;
}
