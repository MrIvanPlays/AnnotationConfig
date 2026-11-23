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

package com.mrivanplays.annotationconfig.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.dataformat.toml.TomlReadFeature;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Helpers shared by the TOML tests. */
final class TomlTesting {

  static final ConfigResolver RESOLVER = TomlConfig.getConfigResolver();

  private static final TomlMapper PARSER =
      TomlMapper.builder().configure(TomlReadFeature.PARSE_JAVA_TIME, true).build();

  private TomlTesting() {}

  /** Dumps {@code config}, checks that the output is valid TOML and returns it with LF endings. */
  static String dump(Object config) {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(config, writer);
    String toml = writer.toString();
    parse(toml);
    return toml;
  }

  static <T> T load(T config, String toml) {
    RESOLVER.load(config, new StringReader(toml));
    return config;
  }

  /** Dumps {@code config} and loads the output into {@code target}. */
  static <T> T roundTrip(Object config, T target) {
    return load(target, dump(config));
  }

  /** Parses TOML with a parser of its own, independently of AnnotationConfig. */
  @SuppressWarnings("unchecked")
  static Map<String, Object> parse(String toml) {
    try {
      return PARSER.readValue(toml, LinkedHashMap.class);
    } catch (IOException e) {
      throw new UncheckedIOException("Not valid TOML:\n" + toml, e);
    }
  }
}
