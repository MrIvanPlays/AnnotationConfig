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

import static com.mrivanplays.annotationconfig.toml.TomlTesting.dump;
import static com.mrivanplays.annotationconfig.toml.TomlTesting.load;
import static com.mrivanplays.annotationconfig.toml.TomlTesting.parse;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * What only TOML has: table headers, its forms of dotted keys and no {@code null}. What TOML shares
 * with the other formats is in the scenarios.
 */
class TestTomlStructure {

  // <editor-fold desc="Tables">

  @Test
  void tablesWithOnlyTablesGetAHeaderOnlyForTheirComment() {
    // [plain] and [plain.inner] would be empty: [plain.inner.deeper] defines them already
    String expected =
        """
        # outer comment
        [outer]

        [outer.inner]
        value = 1

        [plain.inner.deeper]
        value = 2
        """;
    String toml = dump(new TablesOnly());
    Assertions.assertEquals(expected, toml);
    Assertions.assertEquals(
        Map.of(
            "outer", Map.of("inner", Map.of("value", 1)),
            "plain", Map.of("inner", Map.of("deeper", Map.of("value", 2)))),
        parse(toml));
  }

  @Test
  void tableHeadersQuoteTheirKeys() {
    String toml = dump(new QuotedTable());
    Assertions.assertEquals("['has space'.'dotted.key']\nx = 1\n", toml);
    Assertions.assertEquals(Map.of("has space", Map.of("dotted.key", Map.of("x", 1))), parse(toml));
  }

  @Test
  void nullValuesAreRejected() {
    IllegalArgumentException e =
        Assertions.assertThrows(IllegalArgumentException.class, () -> dump(new NullInMap()));
    Assertions.assertTrue(e.getMessage().contains("map.missing"), e.getMessage());
  }

  // </editor-fold>

  // <editor-fold desc="Dotted keys">

  @Test
  void dottedKeysLoadFromEveryForm() {
    // a TOML dotted key is the same as a table
    DottedKeys config = load(new DottedKeys(), "server.ip = 'a'\nother.port = 1");
    Assertions.assertEquals("a", config.ip);
    Assertions.assertEquals(1, config.port);
  }

  @Test
  void quotedDottedKeysFromOlderVersionsStillLoad() {
    // before 3.1.0, @Key("server.ip") was written as one quoted key
    DottedKeys config =
        load(
            new DottedKeys(),
            """
            'server.ip' = 'b'
            'other.port' = 2
            server.players = 30
            """);
    Assertions.assertEquals("b", config.ip);
    Assertions.assertEquals(2, config.port);
    Assertions.assertEquals(30, config.server.players);
  }

  // </editor-fold>

  // <editor-fold desc="Tables: subjects">

  static class TablesOnly {

    @Comment("outer comment")
    public Map<String, Object> outer = Map.of("inner", Map.of("value", 1));

    public Map<String, Object> plain = Map.of("inner", Map.of("deeper", Map.of("value", 2)));
  }

  static class QuotedTable {

    @Key("has space")
    public Map<String, Object> table = Map.of("dotted.key", Map.of("x", 1));
  }

  static class NullInMap {

    public Map<String, Object> map = new LinkedHashMap<>();

    NullInMap() {
      map.put("missing", null);
    }
  }

  // </editor-fold>

  // <editor-fold desc="Dotted keys: subjects">

  public static class Limits {

    public int players = 20;
  }

  static class DottedKeys {

    @Key("server.ip")
    public String ip = "localhost";

    @ConfigObject public Limits server = new Limits();

    @Key("other.port")
    public int port = 25565;
  }

  // </editor-fold>
}
