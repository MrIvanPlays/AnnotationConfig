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

package com.mrivanplays.annotationconfig.scenarios;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

/**
 * The base of the scenario classes: a topic run in one {@link Format}. A scenario class is a
 * {@code @ParameterizedClass} with a {@link Sources} {@code @MethodSource}, and passes the format
 * it gets to this constructor:
 *
 * <pre>{@code
 * @ParameterizedClass(name = "{0}")
 * @MethodSource(Sources.WITH_SECTIONS)
 * class TestSections extends Scenario {
 *
 *   TestSections(Format format) {
 *     super(format, "sections");
 *   }
 * }
 * }</pre>
 *
 * <p>Inputs and expected outputs are named by scenario; see {@link Format#text(String, String)}.
 */
public abstract class Scenario {

  protected final Format format;
  protected final ConfigResolver resolver;
  private final String topic;

  protected Scenario(Format format, String topic) {
    this.format = format;
    this.resolver = format.resolver();
    this.topic = topic;
  }

  /** New settings that turn {@code GENERATE_NEW_OPTIONS} on, which TOML keeps off by default. */
  protected static Settings generateNewOptions() {
    return new Settings().put(ACDefaultSettings.GENERATE_NEW_OPTIONS, true);
  }

  /**
   * {@code value} with every number, also in maps and lists, as a {@link BigDecimal}: the parsers
   * of the formats read the same number as different types.
   */
  protected static Object numbersAsDecimals(Object value) {
    if (value instanceof Number number) {
      return new BigDecimal(number.toString());
    }
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> converted = new LinkedHashMap<>();
      map.forEach((key, entry) -> converted.put(key, numbersAsDecimals(entry)));
      return converted;
    }
    if (value instanceof List<?> list) {
      List<Object> converted = new ArrayList<>();
      list.forEach(entry -> converted.add(numbersAsDecimals(entry)));
      return converted;
    }
    return value;
  }

  /** Dumps {@code config} with the resolver's settings. */
  protected String dump(Object config) {
    StringWriter writer = new StringWriter();
    this.resolver.dump(config, writer);
    return writer.toString();
  }

  /** Dumps {@code config} with {@code settings}. */
  protected String dump(Object config, Settings settings) {
    StringWriter writer = new StringWriter();
    this.resolver.dump(config, writer, settings);
    return writer.toString();
  }

  /** The text of scenario {@code name} of this topic in this format. */
  protected String text(String name) {
    return this.format.text(this.topic, name);
  }

  /** Loads the input {@code name} into {@code config}. */
  protected <T> T load(T config, String name) {
    return this.loadText(config, this.text(name));
  }

  /** Loads the input {@code name} into {@code config} with {@code settings}. */
  protected <T> T load(T config, String name, Settings settings) {
    this.resolver.load(config, new StringReader(this.text(name)), settings);
    return config;
  }

  /** Loads {@code text} into {@code config}. */
  protected <T> T loadText(T config, String text) {
    this.resolver.load(config, new StringReader(text));
    return config;
  }

  /** Loads {@code text} into {@code config} with {@code settings}. */
  protected <T> T loadText(T config, String text, Settings settings) {
    this.resolver.load(config, new StringReader(text), settings);
    return config;
  }

  /** The message of the error loading the input {@code name} into {@code config} fails with. */
  protected String rejected(Object config, String name) {
    return Assertions.assertThrows(IllegalArgumentException.class, () -> load(config, name))
        .getMessage();
  }

  /** Asserts that {@code actual} is the expected output {@code name}. */
  protected void assertOutput(String name, String actual) {
    this.format.assertText(this.topic, name, actual);
  }

  /** Dumps {@code config}, asserts that it is the expected output {@code name} and returns it. */
  protected String assertDump(String name, Object config) {
    String written = this.dump(config);
    this.assertOutput(name, written);
    return written;
  }

  /** Dumps {@code config} and loads what was written into {@code target}. */
  protected <T> T roundTrip(Object config, T target) {
    return this.loadText(target, this.dump(config));
  }
}
