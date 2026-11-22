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

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * How config classes map to JSON: config objects and other objects become JSON objects, arrays and
 * lists become JSON arrays, dotted keys become nested objects. Comments are not written, as JSON
 * has none.
 */
public class TestJsonStructure {

  private static final ConfigResolver RESOLVER = JsonConfig.getConfigResolver();

  private static String dump(Object config) {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(config, writer);
    return writer.toString().replaceAll("\\R", "\n");
  }

  private static <T> T load(T config, String json) {
    RESOLVER.load(config, new StringReader(json));
    return config;
  }

  // <editor-fold desc="Config objects">

  @Comment("Comments are not written to JSON")
  static class Sections {

    @Comment("Section comment")
    @ConfigObject
    public Server server = new Server();

    @Comment("Plain field comment")
    public String name = "Ivan";

    public static class Server {

      @Key("ip")
      public String host = "localhost";

      public int port = 25565;

      @ConfigObject public Limits limits = new Limits();
    }

    public static class Limits {

      public int players = 20;
    }
  }

  @Test
  public void configObjectsAreNestedObjects() {
    String expected =
        """
        {
          "server": {
            "ip": "localhost",
            "port": 25565,
            "limits": {
              "players": 20
            }
          },
          "name": "Ivan"
        }""";
    Assertions.assertEquals(expected, dump(new Sections()));
  }

  @Test
  public void configObjectsLoad() {
    Sections config =
        load(
            new Sections(),
            """
            {
              "server": {"ip": "example.org", "port": 25577, "limits": {"players": 50}},
              "name": "Pekov"
            }""");
    Assertions.assertEquals("example.org", config.server.host);
    Assertions.assertEquals(25577, config.server.port);
    Assertions.assertEquals(50, config.server.limits.players);
    Assertions.assertEquals("Pekov", config.name);
  }

  // </editor-fold>

  // <editor-fold desc="Dotted keys">

  static class DottedKeys {

    @Key("commands.subcommands.help.foo")
    public String foo = "bar";

    @Key("commands.subcommands.this.bar")
    public String bar = "aaa";

    @Key("commands.subcommands.this.baz")
    public int baz = -1;
  }

  @Test
  public void dottedKeysAreNestedObjects() {
    String expected =
        """
        {
          "commands": {
            "subcommands": {
              "help": {
                "foo": "bar"
              },
              "this": {
                "bar": "aaa",
                "baz": -1
              }
            }
          }
        }""";
    Assertions.assertEquals(expected, dump(new DottedKeys()));
  }

  @Test
  public void dottedKeysLoad() {
    DottedKeys config =
        load(
            new DottedKeys(),
            """
            {"commands": {"subcommands": {
              "help": {"foo": "pepega"},
              "this": {"bar": "baz", "baz": 69420}
            }}}""");
    Assertions.assertEquals("pepega", config.foo);
    Assertions.assertEquals("baz", config.bar);
    Assertions.assertEquals(69420, config.baz);
  }

  // </editor-fold>

  // <editor-fold desc="Arrays and lists">

  static class Arrays {

    public int[] numbers = {1, 2};
    public BigDecimal[] decimals = {new BigDecimal("1111.2222"), new BigDecimal("3333.4444")};
    public Item[] items = {new Item("a", 1), new Item("b", 2)};
    public List<String> names = List.of("Hello", "bla bla");
    public List<Item> itemList = List.of(new Item("c", 3));
    public List<String> empty = Collections.emptyList();
  }

  public static class Item {

    public String name;
    public int amount;

    public Item() {}

    Item(String name, int amount) {
      this.name = name;
      this.amount = amount;
    }
  }

  @Test
  public void arraysAndListsAreJsonArrays() {
    String expected =
        """
        {
          "numbers": [
            1,
            2
          ],
          "decimals": [
            1111.2222,
            3333.4444
          ],
          "items": [
            {
              "name": "a",
              "amount": 1
            },
            {
              "name": "b",
              "amount": 2
            }
          ],
          "names": [
            "Hello",
            "bla bla"
          ],
          "itemList": [
            {
              "name": "c",
              "amount": 3
            }
          ],
          "empty": []
        }""";
    Assertions.assertEquals(expected, dump(new Arrays()));
  }

  @Test
  public void arraysAndListsLoad() {
    Arrays config =
        load(
            new Arrays(),
            """
            {
              "numbers": [3, 4, 5],
              "decimals": [2222.3333, 4444.5555],
              "items": [{"name": "x", "amount": 7}],
              "names": [],
              "itemList": [{"name": "y", "amount": 8}, {"name": "z", "amount": 9}],
              "empty": ["now", "filled"]
            }""");
    Assertions.assertArrayEquals(new int[] {3, 4, 5}, config.numbers);
    Assertions.assertArrayEquals(
        new BigDecimal[] {new BigDecimal("2222.3333"), new BigDecimal("4444.5555")},
        config.decimals);
    Assertions.assertEquals(1, config.items.length);
    Assertions.assertEquals("x", config.items[0].name);
    Assertions.assertEquals(7, config.items[0].amount);
    Assertions.assertEquals(List.of(), config.names);
    Assertions.assertEquals(2, config.itemList.size());
    Assertions.assertEquals(9, config.itemList.get(1).amount);
    Assertions.assertEquals(List.of("now", "filled"), config.empty);
  }

  static class SingleValueList {

    public List<String> foo;
  }

  @Test
  public void singleValueIsReadAsList() {
    SingleValueList config = load(new SingleValueList(), "{\"foo\": \"aabb\"}");
    Assertions.assertEquals(List.of("aabb"), config.foo);
  }

  // </editor-fold>

  // <editor-fold desc="Maps and DataObject">

  static class Maps {

    public Map<String, List<Map<String, Object>>> values = Map.of("entries", entries());

    private static List<Map<String, Object>> entries() {
      Map<String, Object> first = new LinkedHashMap<>();
      first.put("foo", 1);
      first.put("baz", List.of("baba", "yaga"));
      Map<String, Object> nested = new LinkedHashMap<>();
      nested.put("lorem", "ipsum");
      Map<String, Object> second = new LinkedHashMap<>();
      second.put("foo", 3);
      second.put("baz", nested);
      List<Map<String, Object>> entries = new ArrayList<>();
      entries.add(first);
      entries.add(second);
      return entries;
    }
  }

  @Test
  public void mapsAreJsonObjects() {
    String expected =
        """
        {
          "values": {
            "entries": [
              {
                "foo": 1,
                "baz": [
                  "baba",
                  "yaga"
                ]
              },
              {
                "foo": 3,
                "baz": {
                  "lorem": "ipsum"
                }
              }
            ]
          }
        }""";
    Assertions.assertEquals(expected, dump(new Maps()));
  }

  static class DataObjectField {

    public DataObject data = data();

    private static DataObject data() {
      DataObject data = new DataObject();
      data.put("foo", 2);
      data.put("bar", "baz");
      return data;
    }
  }

  @Test
  public void dataObjectRoundTrip() {
    Assertions.assertEquals(
        """
        {
          "data": {
            "foo": 2,
            "bar": "baz"
          }
        }""",
        dump(new DataObjectField()));

    DataObjectField config =
        load(new DataObjectField(), "{\"data\": {\"foo\": 3, \"bar\": \"foo\"}}");
    Assertions.assertEquals(3, config.data.get("foo").getAsInt());
    Assertions.assertEquals("foo", config.data.get("bar").getAsString());
  }

  // </editor-fold>

  // <editor-fold desc="Records">

  record Location(@Key("world-name") String world, int x, int y, double z) {

    // records with more than one constructor are supported
    Location(String world) {
      this(world, 0, 0, 0);
    }
  }

  static class Records {

    @ConfigObject public Location spawn = new Location("world", 20, 2, 1.5);

    // not a config object: serialized by the default serializer
    public Location home = new Location("nether", -1, 64, 3.0);
  }

  @Test
  public void recordsAreJsonObjects() {
    String expected =
        """
        {
          "spawn": {
            "world-name": "world",
            "x": 20,
            "y": 2,
            "z": 1.5
          },
          "home": {
            "world-name": "nether",
            "x": -1,
            "y": 64,
            "z": 3.0
          }
        }""";
    Assertions.assertEquals(expected, dump(new Records()));
  }

  @Test
  public void recordsLoad() {
    Records config =
        load(
            new Records(),
            """
            {
              "spawn": {"world-name": "end", "x": 1, "y": 2, "z": 3.5},
              "home": {"world-name": "overworld", "x": 4, "y": 5, "z": 6.0}
            }""");
    Assertions.assertEquals(new Location("end", 1, 2, 3.5), config.spawn);
    Assertions.assertEquals(new Location("overworld", 4, 5, 6.0), config.home);
  }

  // </editor-fold>
}
