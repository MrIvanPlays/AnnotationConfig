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

package com.mrivanplays.annotationconfig.scenarios.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.mrivanplays.annotationconfig.core.annotations.ConfigVersion;
import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.OneOf;
import com.mrivanplays.annotationconfig.core.annotations.RawConfig;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigSchema;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The schema of a config without sections, in every format. Sections, collections and the values
 * of the default serializer are in {@link TestSchemaSections}.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestSchema extends Scenario {

  TestSchema(Format format) {
    super(format, "schema");
  }

  /** Asserts that the JSON of {@code schema} reads back as {@link ConfigSchema#asMap()}. */
  static void assertReadsBack(ConfigSchema schema) {
    Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    Map<?, ?> read = gson.fromJson(schema.toJson(), Map.class);
    Assertions.assertEquals(exactNumbers(schema.asMap()), read);
  }

  /** {@code value} with every number as the {@link BigDecimal} Gson reads its JSON as. */
  private static Object exactNumbers(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> converted = new LinkedHashMap<>();
      map.forEach((key, entry) -> converted.put(key, exactNumbers(entry)));
      return converted;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(TestSchema::exactNumbers).toList();
    }
    if (value instanceof Float f) {
      return new BigDecimal(Float.toString(f));
    }
    return value instanceof Number number ? new BigDecimal(number.toString()) : value;
  }

  // <editor-fold desc="Values">

  @Test
  void optionsAreDescribedWithTheirAnnotations() {
    ConfigSchema schema = this.resolver.schema(new Server());

    assertOutput("flat", schema.toJson());
    assertReadsBack(schema);
  }

  @Test
  void theDefaultsAreTheValuesOfTheConfig() {
    Server server = new Server();
    server.port = 8080;
    server.mode = "creative";
    Map<String, Object> properties = properties(this.resolver.schema(server).asMap());

    Assertions.assertEquals(8080, ((Map<?, ?>) properties.get("port")).get("default"));
    Assertions.assertEquals("creative", ((Map<?, ?>) properties.get("mode")).get("default"));
  }

  @Test
  void theSettingsPassedAreUsed() {
    // in reverse declaration order, as .properties writes them
    Settings settings = new Settings().put(ACDefaultSettings.SHOULD_REVERSE_FIELDS, false);
    Map<String, Object> properties =
        properties(this.resolver.schema(new Ordered(), settings).asMap());

    Assertions.assertEquals(List.of("second", "first"), List.copyOf(properties.keySet()));
  }

  @Test
  void theSchemaIsWrittenToAFile(@TempDir Path dir) throws Exception {
    Path file = dir.resolve("schemas").resolve("server.schema.json");
    ConfigSchema schema = this.resolver.schema(new Server());
    schema.write(file);

    Assertions.assertEquals(schema.toJson(), Files.readString(file));
  }

  // </editor-fold>

  @SuppressWarnings("unchecked")
  static Map<String, Object> properties(Map<String, Object> schema) {
    return (Map<String, Object>) schema.get("properties");
  }

  // <editor-fold desc="Values: subjects">

  enum Level {
    EASY,
    HARD
  }

  @Comment("The server's settings")
  @ConfigVersion(value = 2, comments = "Don't change this")
  static class Server {

    @Comment("The port to listen on")
    @Comment("between 1 and 65535")
    @Min(minInt = 1)
    @Max(maxInt = 65535)
    int port = 25565;

    @OneOf({"survival", "creative"})
    String mode = "survival";

    @OneOf(value = {"easy", "hard"}, ignoreCase = true)
    String difficulty = "easy";

    @Comment("Quotes \"and\" a back\\slash")
    @Min(minInt = 3)
    @Max(maxInt = 16)
    String name = "server";

    Level level = Level.HARD;

    boolean whitelist = true;

    @Min(minDouble = 0.5)
    double ratio = 1.5;

    @Max(maxFloat = 0.9f)
    float speed = 0.25f;

    long seed = 42;

    BigInteger big = BigInteger.valueOf(7);

    char marker = '#';

    @Max(maxInt = 5)
    int[] levels = {1, 2};

    @Key("dotted.key")
    String dotted = "value";

    @Ignore int ignored = 1;

    @RawConfig DataObject raw;
  }

  static class Ordered {

    int first = 1;

    int second = 2;
  }

  // </editor-fold>
}
