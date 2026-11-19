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
