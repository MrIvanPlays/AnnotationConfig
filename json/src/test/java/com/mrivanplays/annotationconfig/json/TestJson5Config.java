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

package com.mrivanplays.annotationconfig.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.WritableObject;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The resolver returned by {@link JsonConfig#getJson5ConfigResolver()}. How values are written is
 * covered by {@link TestJson5ValueWriter}; this checks how the resolver is set up.
 */
class TestJson5Config {

  private static final ConfigResolver JSON5 = JsonConfig.getJson5ConfigResolver();

  private static String dump(ConfigResolver resolver, Object config) {
    StringWriter writer = new StringWriter();
    resolver.dump(config, writer);
    return writer.toString();
  }

  @Test
  void returnsOneResolverSeparateFromJson() {
    Assertions.assertSame(JSON5, JsonConfig.getJson5ConfigResolver());
    Assertions.assertNotSame(JsonConfig.getConfigResolver(), JSON5);
  }

  @Test
  void dumpsJson5WithComments() {
    // the class comment uses the resolver's "// " prefix; fields are in declaration order and
    // dotted keys are nested
    String expected =
        """
        // Server config

        {
          // Where to listen
          host: "localhost",
          "max-players": 20,
          motd: {
            text: "Hello"
          },
          // Database
          database: {
            // Seconds
            timeout: 30
          }
        }\
        """;
    Assertions.assertEquals(expected, dump(JSON5, new Server()));
  }

  @Test
  void jsonResolverStillWritesPlainJson() {
    // creating the JSON5 resolver must not change the JSON one
    JsonConfig.getJson5ConfigResolver();
    String expected =
        """
        {
          "host": "localhost",
          "max-players": 20,
          "motd": {
            "text": "Hello"
          },
          "database": {
            "timeout": 30
          }
        }\
        """;
    Assertions.assertEquals(expected, dump(JsonConfig.getConfigResolver(), new Server()));
  }

  @Test
  void fileRoundTrip(@TempDir Path dir) throws IOException {
    Server config = new Server();
    config.host = "example.org";
    config.maxPlayers = 100;
    config.motd = "Welcome";
    config.database.timeout = 5;
    Path file = dir.resolve("server.json5");
    JSON5.dump(config, file);

    Server loaded = new Server();
    JSON5.load(loaded, file);
    Assertions.assertEquals("example.org", loaded.host);
    Assertions.assertEquals(100, loaded.maxPlayers);
    Assertions.assertEquals("Welcome", loaded.motd);
    Assertions.assertEquals(5, loaded.database.timeout);
    Assertions.assertTrue(Files.readString(file).contains("// Where to listen"));
  }

  @Test
  void readsFilesWrittenAsJson(@TempDir Path dir) {
    // JSON is JSON5, so switching a config from the JSON resolver to the JSON5 one keeps its values
    Server config = new Server();
    config.host = "example.org";
    config.database.timeout = 5;
    Path file = dir.resolve("server.json");
    JsonConfig.getConfigResolver().dump(config, file);

    Server loaded = new Server();
    JSON5.load(loaded, file);
    Assertions.assertEquals("example.org", loaded.host);
    Assertions.assertEquals(5, loaded.database.timeout);
  }

  @Test
  void resolveMultipleLoadsJson5Files(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("a.json5"), "{name: 'a'}");
    Files.writeString(dir.resolve("b.json"), "{\"name\": \"b\"}");
    Files.writeString(dir.resolve("c.txt"), "{name: 'c'}");
    WritableObject defaults = WritableObject.createFromPath(dir.resolve("default.json5"));

    Map<String, Named> json5 = JSON5.resolveMultiple(dir, Named::new, defaults);
    Assertions.assertEquals(List.of("a.json5"), List.copyOf(json5.keySet()));
    Assertions.assertEquals("a", json5.get("a.json5").name);

    // and the JSON resolver does not pick up .json5 files
    Map<String, Named> json =
        JsonConfig.getConfigResolver().resolveMultiple(dir, Named::new, defaults);
    Assertions.assertEquals(List.of("b.json"), List.copyOf(json.keySet()));
  }

  @Test
  void readsWithTheGsonInstanceSetting() {
    String input = "{value: 1.5}";

    Untyped byDefault = new Untyped();
    JSON5.load(byDefault, new StringReader(input));
    Assertions.assertEquals(new BigDecimal("1.5"), byDefault.value);

    // Gson's default number policy reads untyped numbers as Double
    Gson gson = new GsonBuilder().setStrictness(Strictness.LENIENT).create();
    Untyped custom = new Untyped();
    JSON5.load(custom, new StringReader(input), new Settings().put(JsonConfig.GSON_INSTANCE, gson));
    Assertions.assertEquals(1.5, custom.value);
  }

  @Comment("Server config")
  static class Server {

    @Comment("Where to listen")
    public String host = "localhost";

    @Key("max-players")
    public int maxPlayers = 20;

    @Key("motd.text")
    public String motd = "Hello";

    @Comment("Database")
    @ConfigObject
    public Database database = new Database();
  }

  public static class Database {

    @Comment("Seconds")
    public int timeout = 30;
  }

  static class Named {

    public String name = "default";
  }

  static class Untyped {

    public Object value = 1;
  }
}
