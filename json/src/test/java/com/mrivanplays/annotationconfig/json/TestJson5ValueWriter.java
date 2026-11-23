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
import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
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

/**
 * {@link Json5ValueWriter} through {@link JsonConfig#getJson5ConfigResolver()}, which reads the
 * written JSON5 back with Gson in lenient mode.
 */
class TestJson5ValueWriter {

  private static final ConfigResolver RESOLVER = JsonConfig.getJson5ConfigResolver();

  private static String dump(Object config) {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(config, writer);
    return writer.toString();
  }

  private static <T> T load(T config, String json5) {
    RESOLVER.load(config, new StringReader(json5));
    return config;
  }

  // <editor-fold desc="Comments">

  @Test
  void commentsAreLineCommentsAboveTheirKeys() {
    String expected =
        """
        // Class comment

        {
          // The name
          name: "Ivan",
          // Server settings
          server: {
            // ip comment
            ip: "localhost",
            port: 25565,
            limits: {
              // players comment
              players: 20
            }
          },
          // first line
          // second line
          multi: 1,
          items: [
            {
              name: "a",
              amount: 1
            }
          ]
        }\
        """;
    Assertions.assertEquals(expected, dump(new Commented()));
  }

  @Test
  void commentedOutputLoadsBack() {
    Commented config = new Commented();
    config.name = "Pekov";
    config.server.port = 1;
    config.server.limits.players = 2;
    config.items = List.of(new Item("b", 3), new Item("c", 4));

    Commented loaded = load(new Commented(), dump(config));
    Assertions.assertEquals("Pekov", loaded.name);
    Assertions.assertEquals("localhost", loaded.server.host);
    Assertions.assertEquals(1, loaded.server.port);
    Assertions.assertEquals(2, loaded.server.limits.players);
    Assertions.assertEquals(2, loaded.items.size());
    Assertions.assertEquals("c", loaded.items.get(1).name);
    Assertions.assertEquals(4, loaded.items.get(1).amount);
  }

  @Test
  void sectionsGetTheirClassComment() {
    Assertions.assertEquals(
        """
        {
          // Section without its own comment
          section: {
            value: 1
          }
        }\
        """,
        dump(new SectionCommentHolder()));
  }

  @Test
  void identifierKeysAreUnquoted() {
    String expected =
        """
        {
          identifier_1: 1,
          $dollar: 2,
          "world-name": 3,
          "has space": 4,
          "1st": 5,
          "quo\\"te": 6,
          dotted: {
            key: 7
          }
        }\
        """;
    String json5 = dump(new Keys());
    Assertions.assertEquals(expected, json5);

    Keys loaded =
        load(
            new Keys(),
            "{identifier_1: 11, $dollar: 12, \"world-name\": 13, \"has space\": 14, \"1st\": 15,"
                + " \"quo\\\"te\": 16, dotted: {key: 17}}");
    Assertions.assertEquals(11, loaded.identifier_1);
    Assertions.assertEquals(12, loaded.dollar);
    Assertions.assertEquals(13, loaded.dash);
    Assertions.assertEquals(14, loaded.space);
    Assertions.assertEquals(15, loaded.digitFirst);
    Assertions.assertEquals(16, loaded.quote);
    Assertions.assertEquals(17, loaded.dotted);
  }

  @Test
  void valuesUseJson5Syntax() {
    String expected =
        """
        {
          plain: "Hello",
          escaped: "say \\"hi\\" \\\\ back",
          control: "line1\\nline2\\ttab\\rcr\\u0007bell\\u2028sep",
          unicode: "caf\u00e9 \u2603",
          empty: "",
          character: "x",
          multiline: "first\\nsecond",
          flag: true,
          mode: "fast",
          integer: -42,
          longValue: 9223372036854775807,
          decimal: 2.5,
          single: 3.11,
          notANumber: NaN,
          positiveInfinity: Infinity,
          negativeInfinity: -Infinity,
          bigDecimal: 0.12345678901234567890,
          bigInteger: 123456789012345678901234567890
        }\
        """;
    Assertions.assertEquals(expected, dump(new Values()));
  }

  @Test
  void valuesRoundTrip() {
    Values target = new Values();
    target.plain = null;
    target.escaped = null;
    target.control = null;
    target.unicode = null;
    target.empty = null;
    target.character = ' ';
    target.multiline = null;
    target.flag = false;
    target.mode = Mode.SLOW;
    target.integer = 0;
    target.longValue = 0;
    target.decimal = 0;
    target.single = 0;
    target.notANumber = 0;
    target.positiveInfinity = 0;
    target.negativeInfinity = 0;
    target.bigDecimal = null;
    target.bigInteger = null;

    Values original = new Values();
    load(target, dump(original));

    Assertions.assertEquals(original.plain, target.plain);
    Assertions.assertEquals(original.escaped, target.escaped);
    Assertions.assertEquals(original.control, target.control);
    Assertions.assertEquals(original.unicode, target.unicode);
    Assertions.assertEquals("", target.empty);
    Assertions.assertEquals('x', target.character);
    Assertions.assertEquals(original.multiline, target.multiline);
    Assertions.assertTrue(target.flag);
    Assertions.assertEquals(Mode.FAST, target.mode);
    Assertions.assertEquals(-42, target.integer);
    Assertions.assertEquals(Long.MAX_VALUE, target.longValue);
    Assertions.assertEquals(2.5, target.decimal);
    Assertions.assertEquals(3.11f, target.single);
    Assertions.assertTrue(Double.isNaN(target.notANumber));
    Assertions.assertEquals(Double.POSITIVE_INFINITY, target.positiveInfinity);
    Assertions.assertEquals(Float.NEGATIVE_INFINITY, target.negativeInfinity);
    Assertions.assertEquals(original.bigDecimal, target.bigDecimal);
    Assertions.assertEquals(original.bigInteger, target.bigInteger);
  }

  @Test
  void arraysMapsAndRecords() {
    // a null entry of an object is left out, as Gson does for JSON
    String expected =
        """
        {
          numbers: [
            1,
            2
          ],
          decimals: [],
          nested: [
            [
              "a",
              "b"
            ],
            []
          ],
          map: {
            key: "value",
            empty: {},
            list: [
              {
                x: 1
              }
            ]
          },
          location: {
            "world-name": "w",
            x: 3,
            z: 1.5
          }
        }\
        """;
    Assertions.assertEquals(expected, dump(new Structures()));
  }

  @Test
  void arraysMapsAndRecordsLoad() {
    Structures loaded =
        load(
            new Structures(),
            """
            {
              numbers: [3, 4, 5],
              decimals: [0.5],
              nested: [["x"]],
              map: {key: "other"},
              location: {"world-name": "nether", x: -1, z: 2.5}
            }\
            """);
    Assertions.assertArrayEquals(new int[] {3, 4, 5}, loaded.numbers);
    Assertions.assertArrayEquals(new double[] {0.5}, loaded.decimals);
    Assertions.assertEquals(List.of(List.of("x")), loaded.nested);
    Assertions.assertEquals(Map.of("key", "other"), loaded.map);
    Assertions.assertEquals(new Location("nether", -1, 2.5), loaded.location);
  }

  @Test
  void configWithoutOptionsIsAnEmptyObject() {
    Assertions.assertEquals("{}", dump(new Empty()));
  }

  // </editor-fold>

  // <editor-fold desc="Keys">

  @Test
  void handWrittenJson5Loads() {
    HandWritten loaded =
        load(
            new HandWritten(),
            """
            // a line comment
            {
              /* a block
                 comment */
              name: 'single quoted',
              "port": 8080, // comment after a value
              tags: ["a", 'b']
            }\
            """);
    Assertions.assertEquals("single quoted", loaded.name);
    Assertions.assertEquals(8080, loaded.port);
    Assertions.assertEquals(List.of("a", "b"), loaded.tags);
  }

  @Test
  void missingOptionsAreAddedWithTheirComments(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config.json5");
    RESOLVER.dump(new Version1(), file);

    Version2 config = new Version2();
    RESOLVER.load(config, file);

    Assertions.assertEquals("foo", config.foo);
    Assertions.assertTrue(config.added);
    String expected =
        """
        {
          // foo comment
          foo: "foo",
          // added comment
          added: true
        }\
        """;
    Assertions.assertEquals(expected, Files.readString(file));
  }

  // </editor-fold>

  // <editor-fold desc="Values">

  enum Mode {
    FAST,
    SLOW
  }

  @Comment("Class comment")
  static class Commented {

    @Comment("The name")
    public String name = "Ivan";

    @Comment("Server settings")
    @ConfigObject
    public Server server = new Server();

    @Comment("first line\nsecond line")
    public int multi = 1;

    public List<Item> items = List.of(new Item("a", 1));
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

  // </editor-fold>

  // <editor-fold desc="Arrays, maps and records">

  public static class Item {

    @Comment("not written: comments inside arrays are not supported")
    public String name;

    public int amount;

    public Item() {}

    Item(String name, int amount) {
      this.name = name;
      this.amount = amount;
    }
  }

  @Comment("Section without its own comment")
  public static class SectionComment {

    public int value = 1;
  }

  static class SectionCommentHolder {

    @ConfigObject public SectionComment section = new SectionComment();
  }

  static class Keys {

    public int identifier_1 = 1;

    @Key("$dollar")
    public int dollar = 2;

    @Key("world-name")
    public int dash = 3;

    @Key("has space")
    public int space = 4;

    @Key("1st")
    public int digitFirst = 5;

    @Key("quo\"te")
    public int quote = 6;

    @Key("dotted.key")
    public int dotted = 7;
  }

  static class Values {

    public String plain = "Hello";
    public String escaped = "say \"hi\" \\ back";
    public String control = "line1\nline2\ttab\rcr\u0007bell\u2028sep";
    public String unicode = "caf\u00e9 \u2603";
    public String empty = "";
    public char character = 'x';

    @Multiline('|')
    public String multiline = "first\nsecond";

    public boolean flag = true;
    public Mode mode = Mode.FAST;
    public int integer = -42;
    public long longValue = Long.MAX_VALUE;
    public double decimal = 2.5;
    public float single = 3.11f;
    public double notANumber = Double.NaN;
    public double positiveInfinity = Double.POSITIVE_INFINITY;
    public float negativeInfinity = Float.NEGATIVE_INFINITY;
    public BigDecimal bigDecimal = new BigDecimal("0.12345678901234567890");
    public BigInteger bigInteger = new BigInteger("123456789012345678901234567890");
  }

  record Location(@Key("world-name") String world, int x, double z) {}

  // </editor-fold>

  // <editor-fold desc="Reading hand-written JSON5">

  static class Structures {

    public int[] numbers = {1, 2};
    public double[] decimals = {};
    public List<List<String>> nested = List.of(List.of("a", "b"), List.of());
    public Map<String, Object> map = map();
    public Location location = new Location("w", 3, 1.5);

    private static Map<String, Object> map() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("key", "value");
      map.put("missing", null);
      map.put("empty", new LinkedHashMap<>());
      map.put("list", List.of(Map.of("x", 1)));
      return map;
    }
  }

  static class Empty {

    @Ignore public int ignored = 1;
  }

  // </editor-fold>

  // <editor-fold desc="Files">

  static class HandWritten {

    public String name = "default";
    public int port = 0;
    public List<String> tags = List.of();
  }

  static class Version1 {

    @Comment("foo comment")
    public String foo = "foo";
  }

  // Version1 with a changed default and an option added
  static class Version2 {

    @Comment("foo comment")
    public String foo = "changed";

    @Comment("added comment")
    public boolean added = true;
  }

  // </editor-fold>
}
