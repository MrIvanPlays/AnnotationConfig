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

package com.mrivanplays.annotationconfig.core.resolver.settings;

/**
 * Represents a simple 2 option enum, which is telling the resolver how to handle null values, which
 * have been deserialized, but the deserialized value returned null.
 *
 * @since 2.0.0
 * @author Ivan Pekov
 */
public enum NullReadHandleOption {
  /**
   * If this constant is set, it tells the resolver that if we get a null value it will skip binding
   * it to the {@link java.lang.reflect.Field}, which is representing that value, which in tern
   * means that it will return the default value.
   */
  USE_DEFAULT_VALUE,

  /**
   * If this constant is set, it tells the resolver that if we get a null value it will bind it to
   * the {@link java.lang.reflect.Field}, which is representing that value.
   */
  SET_NULL
}
