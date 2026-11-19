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

package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Represents a reader of values from a {@link Reader} to a {@link Map} of primitive options (using
 * {@link String} as a key). When it is implemented, it should implement 1 of the 2 methods,
 * otherwise a {@link IllegalArgumentException} is thrown if none of the methods is implemented.
 *
 * @author Ivan Pekov
 * @since 2.0.0
 */
@FunctionalInterface
public interface ValueReader {

  /**
   * Should read the specified reader to a Map. Can use the specified {@link Settings} to manipulate
   * the output of this method, or the ways the reader is parsed to the needed output.
   *
   * @param reader the reader we need read
   * @param settings the settings
   * @return the values read, represented as a map, or empty map if no values have been read
   * @throws IOException if an io occurs
   */
  Map<String, Object> read(Reader reader, Settings settings) throws IOException;
}
