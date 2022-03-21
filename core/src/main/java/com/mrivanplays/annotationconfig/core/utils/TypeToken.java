package com.mrivanplays.annotationconfig.core.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Represents a helper of capturing types of {@link java.util.List Lists} or other generics for ease
 * of registration in {@link com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry}
 *
 * @param <T> type to capture
 * @author MrIvanPlays
 * @since v2.1.1
 */
public abstract class TypeToken<T> {

  private final Type type;

  public TypeToken(Type type) {
    this.type = type;
  }

  public TypeToken() {
    Type superclass = getClass().getGenericSuperclass();
    this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
  }

  /**
   * Returns the captured generic type.
   *
   * @return generic type
   */
  public Type getType() {
    return type;
  }
}
