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
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * How config classes map to TOML: top-level options are separated by an empty line, config objects
 * and other objects become dotted keys, arrays and lists become arrays with inline tables for
 * objects. Every output is also parsed by a separate TOML parser.
 */
public class TestTomlStructure {

  // <editor-fold desc="Config objects and comments">

  @Comment("Class comment")
  static class Sections {

    @Comment("Section comment")
    @ConfigObject
    public Server server = new Server();

    @ConfigObject public Server second = new Server();

    @Comment("Plain field comment")
    public String name = "Ivan";
  }

  @Comment("Server class comment")
  public static class Server {

    @Comment("ip comment")
    @Key("ip")
    public String host = "localhost";

    public int port = 25565;

    @ConfigObject public Limits limits = new Limits();
  }

  public static class Limits {

    @Comment("players comment")
    public int players = 20;
  }

  @Test
  public void configObjectsAreDottedKeysWithTheirComments() {
    String expected =
        """
        # Class comment

        # Section comment
        # ip comment
        server.ip = 'localhost'
        server.port = 25565
        # players comment
        server.limits.players = 20

        # Server class comment
        # ip comment
        second.ip = 'localhost'
        second.port = 25565
        # players comment
        second.limits.players = 20

        # Plain field comment
        name = 'Ivan'
        """;
    Assertions.assertEquals(expected, dump(new Sections()));
  }

  @Test
  public void configObjectsLoad() {
    Sections config =
        load(
            new Sections(),
            """
            name = 'Pekov'

            [server]
            ip = 'example.org'
            port = 25577
            limits.players = 50
            """);
    Assertions.assertEquals("Pekov", config.name);
    Assertions.assertEquals("example.org", config.server.host);
    Assertions.assertEquals(25577, config.server.port);
    Assertions.assertEquals(50, config.server.limits.players);
  }

  static class SimilarNames {

    @Comment("portal comment")
    public int portal = 1;

    public int port = 2;
  }

  @Test
  public void commentsBelongToExactlyOneKey() {
    // "port" has no comment and is a prefix of "portal", so it used to get portal's comment
    Assertions.assertEquals(
        "# portal comment\nportal = 1\n\nport = 2\n", dump(new SimilarNames()));
  }

  static class MultilineComment {

    @Comment("first line\nsecond line")
    public int value = 1;
  }

  @Test
  public void everyCommentLineIsAComment() {
    Assertions.assertEquals(
        "# first line\n# second line\nvalue = 1\n", dump(new MultilineComment()));
  }

  // </editor-fold>

  // <editor-fold desc="Keys">

  static class Keys {

    @Key("bare_key-1")
    public int bare = 1;

    @Key("has space")
    public int spaced = 2;

    @Key("dotted.key")
    public int dotted = 3;

    @Key("quo'te")
    public int quote = 4;
  }

  @Test
  public void keysAreQuotedWhenTheyAreNotBare() {
    String expected =
        """
        bare_key-1 = 1

        'has space' = 2

        'dotted.key' = 3

        "quo'te" = 4
        """;
    String toml = dump(new Keys());
    Assertions.assertEquals(expected, toml);
    // the dotted key is one key, not a table
    Assertions.assertEquals(3, parse(toml).get("dotted.key"));

    Keys config = load(new Keys(), "bare_key-1 = 5\n'has space' = 6\n'dotted.key' = 7\n\"quo'te\" = 8");
    Assertions.assertEquals(5, config.bare);
    Assertions.assertEquals(6, config.spaced);
    Assertions.assertEquals(7, config.dotted);
    Assertions.assertEquals(8, config.quote);
  }

  // </editor-fold>

  // <editor-fold desc="Arrays and lists">

  public static class Item {

    public String name;
    public int amount;

    public Item() {}

    Item(String name, int amount) {
      this.name = name;
      this.amount = amount;
    }
  }

  static class ArrayFields {

    public int[] numbers = {1, 2};
    public double[] decimals = {1.1, 2.2};
    public List<String> names = List.of("Hello", "bla bla");
    public List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3));
    public Item[] items = {new Item("a", 1), new Item("b", 2)};
    public List<String> empty = List.of();
  }

  @Test
  public void arraysAreArraysWithInlineTablesForObjects() {
    String expected =
        """
        numbers = [1, 2]

        decimals = [1.1, 2.2]

        names = ['Hello', 'bla bla']

        nested = [[1, 2], [3]]

        items = [{name = 'a', amount = 1}, {name = 'b', amount = 2}]

        empty = []
        """;
    Assertions.assertEquals(expected, dump(new ArrayFields()));
  }

  @Test
  public void arraysLoad() {
    ArrayFields config =
        load(
            new ArrayFields(),
            """
            numbers = [3, 4, 5]
            decimals = []
            names = ['x']
            nested = [[9]]
            empty = ['now', 'filled']

            [[items]]
            name = 'c'
            amount = 7
            """);
    Assertions.assertArrayEquals(new int[] {3, 4, 5}, config.numbers);
    Assertions.assertArrayEquals(new double[0], config.decimals);
    Assertions.assertEquals(List.of("x"), config.names);
    Assertions.assertEquals(List.of(List.of(9)), config.nested);
    Assertions.assertEquals(List.of("now", "filled"), config.empty);
    // an array of tables is read like an array of inline tables
    Assertions.assertEquals(1, config.items.length);
    Assertions.assertEquals("c", config.items[0].name);
    Assertions.assertEquals(7, config.items[0].amount);
  }

  // </editor-fold>

  // <editor-fold desc="Maps, records and DataObject">

  record Location(@Key("world-name") String world, int x, double z) {}

  static class ObjectFields {

    public Map<String, Object> map = map();
    // an empty map is left out like other empty values, so it is not a field of its own here
    public Location location = new Location("w", 3, 1.5);
    public DataObject data = data();

    private static Map<String, Object> map() {
      Map<String, Object> nested = new LinkedHashMap<>();
      nested.put("deep", true);
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("key", "value");
      map.put("nested", nested);
      map.put("empty", new LinkedHashMap<>());
      map.put("list", List.of(Map.of(), Map.of("x", 1)));
      return map;
    }

    private static DataObject data() {
      DataObject data = new DataObject();
      data.put("foo", 2);
      data.put("bar", "baz");
      return data;
    }
  }

  @Test
  public void objectsAreDottedKeys() {
    String expected =
        """
        map.key = 'value'
        map.nested.deep = true
        map.empty = {}
        map.list = [{}, {x = 1}]

        location.world-name = 'w'
        location.x = 3
        location.z = 1.5

        data.foo = 2
        data.bar = 'baz'
        """;
    Assertions.assertEquals(expected, dump(new ObjectFields()));
  }

  @Test
  public void objectsLoad() {
    ObjectFields config =
        load(
            new ObjectFields(),
            """
            map = {key = 'other'}
            location = {world-name = 'nether', x = -1, z = 2.5}

            [data]
            foo = 3
            bar = 'qux'
            """);
    Assertions.assertEquals(Map.of("key", "other"), config.map);
    Assertions.assertEquals(new Location("nether", -1, 2.5), config.location);
    Assertions.assertEquals(3, config.data.get("foo").getAsInt());
    Assertions.assertEquals("qux", config.data.get("bar").getAsString());
  }

  static class NullInMap {

    public Map<String, Object> map = new LinkedHashMap<>();

    NullInMap() {
      map.put("missing", null);
    }
  }

  @Test
  public void nullValuesAreRejected() {
    IllegalArgumentException e =
        Assertions.assertThrows(IllegalArgumentException.class, () -> dump(new NullInMap()));
    Assertions.assertTrue(e.getMessage().contains("map.missing"), e.getMessage());
  }

  // </editor-fold>
}
