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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.ToNumberPolicy;
import com.mrivanplays.annotationconfig.core.PropertyConfig;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.json.Json5ValueWriter;
import com.mrivanplays.annotationconfig.json.JsonConfig;
import com.mrivanplays.annotationconfig.toml.TomlConfig;
import com.mrivanplays.annotationconfig.toml.TomlValueWriter;
import com.mrivanplays.annotationconfig.yaml.YamlConfig;
import com.mrivanplays.annotationconfig.yaml.YamlHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.yaml.snakeyaml.Yaml;

/**
 * A config format, as the shared scenarios see it: its resolver, what it supports, and the text of
 * each scenario in that format.
 *
 * <p>The scenario classes are {@code @ParameterizedClass}es that run once per format having the
 * features they need: their {@code @MethodSource} is one of the static methods here, such as
 * {@link #withSections()}. A new format is a constant here, with its features, and the texts of the
 * scenarios it runs.
 *
 * <p>The texts of a topic in one format are the resource {@code scenarios/<topic>.<extension>}:
 * the inputs its scenarios load and the exact outputs they expect from {@code dump}. Each text is a
 * section that starts with a line {@code # --- <name>} ({@code // --- <name>} in JSON and JSON5)
 * and ends with a blank line, which is not part of the text. JSON5 reads JSON, so a JSON5 scenario
 * without a section in {@code .json5} uses the {@code .json} one; outputs differ, so each has its
 * own. Run the tests with {@code -Dscenarios.record=<directory>} to write the outputs to that
 * directory instead of comparing them: each file there is the resource with the recorded outputs
 * replaced or added. Review them there, then copy them to the resources.
 */
public enum Format {
  PROPERTIES(
      "properties", PropertyConfig.class, "getConfigResolver",
      Feature.COMMENTS, Feature.GENERATES_NEW_OPTIONS, Feature.SPECIAL_FLOATS),
  YAML(
      "yml", YamlConfig.class, "getConfigResolver",
      Feature.SECTIONS, Feature.COMMENTS, Feature.GENERATES_NEW_OPTIONS, Feature.NULLS,
      Feature.BIG_INTEGERS, Feature.SPECIAL_FLOATS),
  JSON(
      "json", JsonConfig.class, "getConfigResolver",
      Feature.SECTIONS, Feature.GENERATES_NEW_OPTIONS, Feature.NULLS, Feature.BIG_INTEGERS),
  JSON5(
      "json5", JsonConfig.class, "getJson5ConfigResolver",
      Feature.SECTIONS, Feature.COMMENTS, Feature.GENERATES_NEW_OPTIONS, Feature.NULLS,
      Feature.BIG_INTEGERS, Feature.SPECIAL_FLOATS),
  TOML(
      "toml", TomlConfig.class, "getConfigResolver",
      Feature.SECTIONS, Feature.COMMENTS, Feature.SPECIAL_FLOATS);

  private final String extension;
  private final Class<?> entryPoint;
  private final String resolverGetter;
  private final Set<Feature> features;

  /** The sections of each resource read so far, by {@code <topic>.<extension>}. */
  private static final Map<String, Map<String, String>> SECTIONS = new ConcurrentHashMap<>();

  /** The sections of each file recorded so far, by {@code <topic>.<extension>}. */
  private static final Map<String, Map<String, String>> RECORDED = new LinkedHashMap<>();

  Format(String extension, Class<?> entryPoint, String resolverGetter, Feature... features) {
    this.extension = extension;
    this.entryPoint = entryPoint;
    this.resolverGetter = resolverGetter;
    this.features = features.length == 0 ? Set.of() : EnumSet.copyOf(Arrays.asList(features));
  }

  /** What a format may or may not support; scenarios run only for the formats with theirs. */
  public enum Feature {
    /** Nested sections: {@code @ConfigObject}, maps, objects and records. */
    SECTIONS,
    /** Comments are written. */
    COMMENTS,
    /** {@code GENERATE_NEW_OPTIONS} is on by default; TOML keeps it off. */
    GENERATES_NEW_OPTIONS,
    /** {@code null} can be written, e.g. as an element of an array. */
    NULLS,
    /** Integers beyond 64 bits are written as numbers, not as strings. */
    BIG_INTEGERS,
    /** NaN and the infinities are written as numbers. */
    SPECIAL_FLOATS
  }

  /** The formats having every one of {@code features}. */
  public static Stream<Format> having(Feature... features) {
    return Arrays.stream(values()).filter(format -> format.has(features));
  }

  /** Every format; a {@code @MethodSource} of the scenarios. */
  public static Stream<Format> all() {
    return having();
  }

  /** The formats with sections; a {@code @MethodSource} of the scenarios. */
  public static Stream<Format> withSections() {
    return having(Feature.SECTIONS);
  }

  /** The formats with sections and comments; a {@code @MethodSource} of the scenarios. */
  public static Stream<Format> withCommentedSections() {
    return having(Feature.SECTIONS, Feature.COMMENTS);
  }

  /** The formats with sections that can write null; a {@code @MethodSource} of the scenarios. */
  public static Stream<Format> withNulls() {
    return having(Feature.SECTIONS, Feature.NULLS);
  }

  /** The formats with sections and big integers; a {@code @MethodSource} of the scenarios. */
  public static Stream<Format> withBigIntegers() {
    return having(Feature.SECTIONS, Feature.BIG_INTEGERS);
  }

  /** The formats that write NaN and the infinities; a {@code @MethodSource} of the scenarios. */
  public static Stream<Format> withSpecialFloats() {
    return having(Feature.SPECIAL_FLOATS);
  }

  /** Whether the format has every one of {@code features}. */
  public boolean has(Feature... features) {
    for (Feature feature : features) {
      if (!this.features.contains(feature)) {
        return false;
      }
    }
    return true;
  }

  /** The shared resolver of the format. */
  public ConfigResolver resolver() {
    return switch (this) {
      case PROPERTIES -> PropertyConfig.getConfigResolver();
      case YAML -> YamlConfig.getConfigResolver();
      case JSON -> JsonConfig.getConfigResolver();
      case JSON5 -> JsonConfig.getJson5ConfigResolver();
      case TOML -> TomlConfig.getConfigResolver();
    };
  }

  /** The file extension of the format, without the dot. */
  public String extension() {
    return this.extension;
  }

  /** The class whose static method {@link #resolverGetter()} returns the shared resolver. */
  public Class<?> entryPoint() {
    return this.entryPoint;
  }

  /** The name of the static method of {@link #entryPoint()} that returns the shared resolver. */
  public String resolverGetter() {
    return this.resolverGetter;
  }

  /** The writer of the format, which takes the comments by path; formats with comments only. */
  public ValueWriter commentingWriter() {
    return switch (this) {
      case YAML -> new YamlHandler();
      case JSON5 -> new Json5ValueWriter();
      case TOML -> new TomlValueWriter();
      default -> throw new UnsupportedOperationException(this + " has no commenting writer");
    };
  }

  /** Parses {@code text} with the format's library, not with AnnotationConfig. */
  public Map<?, ?> parse(String text) {
    return switch (this) {
      case YAML -> new Yaml().load(text);
      case JSON, JSON5 ->
          new GsonBuilder()
              .setStrictness(Strictness.LENIENT)
              .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL)
              .create()
              .fromJson(text, Map.class);
      case TOML -> {
        try {
          yield new TomlMapper().readValue(text, Map.class);
        } catch (JsonProcessingException e) {
          throw new UncheckedIOException(e);
        }
      }
      case PROPERTIES -> throw new UnsupportedOperationException("not parsed as a map");
    };
  }

  /**
   * The text of scenario {@code name} of {@code topic} in this format: its section of the resource
   * {@code scenarios/<topic>.<extension>}.
   *
   * @throws AssertionError if the format has no text for it
   */
  public String text(String topic, String name) {
    String found = sections(topic, this.extension).get(name);
    if (found == null && this == JSON5) {
      found = sections(topic, JSON.extension).get(name);
    }
    if (found == null) {
      throw new AssertionError(
          "No section "
              + name
              + " in resource scenarios/"
              + topic
              + "."
              + this.extension
              + "; run with -Dscenarios.record=<directory> to record the outputs");
    }
    return found;
  }

  /**
   * Asserts that {@code actual}, the output of a dump, is the text of scenario {@code name}. With
   * {@code -Dscenarios.record=<directory>}, records it instead: see {@link #record}.
   */
  public void assertText(String topic, String name, String actual) {
    String recordTo = System.getProperty("scenarios.record");
    if (recordTo != null) {
      record(Path.of(recordTo), topic, this.extension, name, actual);
      return;
    }
    // not normalized: the line endings are part of what is tested
    Assertions.assertEquals(
        this.text(topic, name),
        actual,
        "section " + name + " of scenarios/" + topic + "." + this.extension);
  }

  /** The line that starts a section, followed by its name, in a file with {@code extension}. */
  private static String marker(String extension) {
    return extension.startsWith("json") ? "// --- " : "# --- ";
  }

  /** The sections of resource {@code scenarios/<topic>.<extension>} by name; empty if none. */
  private static Map<String, String> sections(String topic, String extension) {
    return SECTIONS.computeIfAbsent(
        topic + "." + extension,
        file -> {
          String path = "scenarios/" + file;
          try (InputStream in = Format.class.getClassLoader().getResourceAsStream(path)) {
            return in == null
                ? Map.of()
                : parseSections(
                    path, new String(in.readAllBytes(), StandardCharsets.UTF_8), marker(extension));
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }

  /**
   * Splits {@code text} into its sections. Each starts with a line of {@code marker} and its name,
   * and ends with the blank line that separates it from the next one, which is not part of it.
   */
  static Map<String, String> parseSections(String path, String text, String marker) {
    Map<String, String> sections = new LinkedHashMap<>();
    String name = null;
    int start = 0;
    int line = 0;
    while (line < text.length()) {
      int next = text.indexOf('\n', line) + 1;
      if (next == 0) {
        next = text.length();
      }
      if (text.startsWith(marker, line)) {
        if (name != null) {
          addSection(sections, path, name, text.substring(start, line));
        }
        name = text.substring(line + marker.length(), next).strip();
        start = next;
      } else if (name == null) {
        throw new AssertionError(path + ": text before the first '" + marker + "<name>' line");
      }
      line = next;
    }
    if (name != null) {
      addSection(sections, path, name, text.substring(start));
    }
    return sections;
  }

  private static void addSection(
      Map<String, String> sections, String path, String name, String text) {
    if (!text.endsWith("\n")) {
      throw new AssertionError(path + ": section " + name + " doesn't end with a blank line");
    }
    if (sections.put(name, text.substring(0, text.length() - 1)) != null) {
      throw new AssertionError(path + ": section " + name + " appears twice");
    }
  }

  /** {@code sections} as the text of a resource, the reverse of {@link #parseSections}. */
  static String joinSections(Map<String, String> sections, String marker) {
    StringBuilder text = new StringBuilder();
    sections.forEach(
        (name, section) ->
            text.append(marker).append(name).append('\n').append(section).append('\n'));
    return text.toString();
  }

  /**
   * Writes {@code directory/<topic>.<extension>}: the resource with the section {@code name} set to
   * {@code actual}, and every section recorded before in this run. Review it there, then copy it
   * to the resources.
   */
  private static synchronized void record(
      Path directory, String topic, String extension, String name, String actual) {
    String file = topic + "." + extension;
    Map<String, String> sections =
        RECORDED.computeIfAbsent(file, _ -> new LinkedHashMap<>(sections(topic, extension)));
    sections.put(name, actual);
    try {
      Files.createDirectories(directory);
      Files.writeString(directory.resolve(file), joinSections(sections, marker(extension)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
