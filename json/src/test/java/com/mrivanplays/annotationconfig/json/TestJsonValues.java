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

import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Reading and writing values of the different supported types as JSON. */
public class TestJsonValues {

  private static final ConfigResolver RESOLVER = JsonConfig.getConfigResolver();

  private static String dump(Object config) {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(config, writer);
    return writer.toString();
  }

  private static <T> T load(T config, String json) {
    RESOLVER.load(config, new StringReader(json));
    return config;
  }

  // <editor-fold desc="Numbers">

  static class Numbers {

    public int integer = 0;
    public long longValue = 0;
    public short shortValue = 0;
    public BigInteger bigInteger = BigInteger.ZERO;
    public BigDecimal bigDecimal = BigDecimal.ZERO;
    public List<Long> longs = List.of();
  }

  @Test
  public void largeWholeNumbersAreReadExactly() {
    // Gson reads every number as a double, whose text form uses an exponent from 10^7 on
    Numbers config =
        load(
            new Numbers(),
            """
            {
              "integer": 2000000000,
              "longValue": 22221111,
              "shortValue": -32768,
              "longs": [1, 10000000, 123456789]
            }""");
    Assertions.assertEquals(2_000_000_000, config.integer);
    Assertions.assertEquals(22_221_111L, config.longValue);
    Assertions.assertEquals(Short.MIN_VALUE, config.shortValue);
    Assertions.assertEquals(List.of(1L, 10_000_000L, 123_456_789L), config.longs);
  }

  @Test
  public void numbersBeyondDoublePrecisionAreReadExactly() {
    Numbers config =
        load(
            new Numbers(),
            """
            {
              "longValue": 9007199254740993,
              "bigInteger": 123456789012345678901234567890,
              "bigDecimal": 0.12345678901234567890
            }""");
    Assertions.assertEquals(9_007_199_254_740_993L, config.longValue);
    Assertions.assertEquals(new BigInteger("123456789012345678901234567890"), config.bigInteger);
    Assertions.assertEquals(new BigDecimal("0.12345678901234567890"), config.bigDecimal);
  }

  @Test
  public void numbersAreWrittenAsJsonNumbers() {
    Numbers config = new Numbers();
    config.integer = 2_000_000_000;
    config.longValue = Long.MAX_VALUE;
    config.bigInteger = new BigInteger("123456789012345678901234567890");
    config.bigDecimal = new BigDecimal("0.12345678901234567890");
    config.longs = List.of(1L, 2L);
    String expected =
        """
        {
          "integer": 2000000000,
          "longValue": 9223372036854775807,
          "shortValue": 0,
          "bigInteger": 123456789012345678901234567890,
          "bigDecimal": 0.12345678901234567890,
          "longs": [
            1,
            2
          ]
        }""";
    Assertions.assertEquals(expected, dump(config));
  }

  static class Floating {

    public double decimal = 0;
    public float single = 0;
  }

  @Test
  public void decimalsAreReadAsParsingTheirTextWould() {
    // Gson reads decimals as BigDecimal, which is converted with doubleValue() / floatValue()
    // instead of through its text; both must give the same value, also when rounding
    String[] texts = {
      "0.1",
      "2.5",
      "0.12345678901234567890",
      "9007199254740993",
      "2.4703282292062328e-324",
      "1.7976931348623159e308",
      "3.4028236e38",
      "7.0064923216240862e-46",
      "-1e-400"
    };
    for (String text : texts) {
      Floating config =
          load(new Floating(), "{\"decimal\": " + text + ", \"single\": " + text + "}");
      Assertions.assertEquals(Double.parseDouble(text), config.decimal, text);
      Assertions.assertEquals(Float.parseFloat(text), config.single, text);
    }
  }

  static class UntypedNumbers {

    public Map<String, Object> map = Map.of();
    public List<Object> objects = List.of();
    public List<BigDecimal> decimals = List.of();
    public List<BigInteger> integers = List.of();
  }

  @Test
  public void numbersInMapsAndListsRoundTripExactly() {
    // numbers in maps were read as doubles (1 came back as 1.0), and numbers in lists were written
    // as doubles, or as strings beyond the long range
    String json =
        """
        {
          "map": {
            "whole": 1,
            "decimal": 0.12345678901234567890,
            "big": 123456789012345678901234567890
          },
          "objects": [
            1,
            2.5
          ],
          "decimals": [
            0.12345678901234567890
          ],
          "integers": [
            123456789012345678901234567890
          ]
        }""";
    UntypedNumbers config = load(new UntypedNumbers(), json);
    Assertions.assertEquals(new BigDecimal("1"), config.map.get("whole"));
    Assertions.assertEquals(new BigDecimal("0.12345678901234567890"), config.map.get("decimal"));
    Assertions.assertEquals(List.of(new BigDecimal("1"), new BigDecimal("2.5")), config.objects);
    Assertions.assertEquals(List.of(new BigDecimal("0.12345678901234567890")), config.decimals);
    Assertions.assertEquals(
        List.of(new BigInteger("123456789012345678901234567890")), config.integers);
    Assertions.assertEquals(json, dump(config));
  }

  // </editor-fold>

  // <editor-fold desc="Strings and characters">

  static class Text {

    public String plain = "a=b & c<d> 'e'";
    public char character = 'x';

    @Multiline('|')
    public String multiline = "first line\nsecond line\n";
  }

  @Test
  public void textIsWrittenAsPlainJsonStrings() {
    // no HTML escaping (= as =), and @Multiline has no special form in JSON
    String expected =
        """
        {
          "plain": "a=b & c<d> 'e'",
          "character": "x",
          "multiline": "first line\\nsecond line\\n"
        }""";
    Assertions.assertEquals(expected, dump(new Text()));
  }

  @Test
  public void emptyStringsRoundTrip() {
    Text config = new Text();
    config.plain = "";
    config.multiline = "";
    String json = dump(config);
    Assertions.assertTrue(json.contains("\"plain\": \"\""), json);

    Text loaded = load(new Text(), json);
    Assertions.assertEquals("", loaded.plain);
    Assertions.assertEquals("", loaded.multiline);
  }

  @Test
  public void textLoads() {
    Text config =
        load(
            new Text(),
            """
            {"plain": "x=1&y=2", "character": "q", "multiline": "one\\ntwo"}""");
    Assertions.assertEquals("x=1&y=2", config.plain);
    Assertions.assertEquals('q', config.character);
    Assertions.assertEquals("one\ntwo", config.multiline);
  }

  // </editor-fold>

  // <editor-fold desc="Enums">

  enum Mode {
    fastMode,
    SPECIAL {
      @Override
      public String toString() {
        return "something else";
      }
    }
  }

  static class Enums {

    public Mode mode = Mode.fastMode;
    public Mode special = Mode.SPECIAL;
    public List<Mode> modes = List.of(Mode.SPECIAL, Mode.fastMode);
  }

  @Test
  public void enumsAreWrittenByLowerCaseName() {
    String expected =
        """
        {
          "mode": "fastmode",
          "special": "special",
          "modes": [
            "special",
            "fastmode"
          ]
        }""";
    Assertions.assertEquals(expected, dump(new Enums()));
  }

  @Test
  public void enumsAreReadCaseInsensitively() {
    Enums config =
        load(
            new Enums(),
            """
            {"mode": "SPECIAL", "special": "FastMode", "modes": ["fastmode"]}""");
    Assertions.assertEquals(Mode.SPECIAL, config.mode);
    Assertions.assertEquals(Mode.fastMode, config.special);
    Assertions.assertEquals(List.of(Mode.fastMode), config.modes);
  }

  @Test
  public void unknownEnumValueIsRejected() {
    Throwable error =
        Assertions.assertThrows(
            RuntimeException.class, () -> load(new Enums(), "{\"mode\": \"nope\"}"));
    while (error.getCause() != null) {
      error = error.getCause();
    }
    Assertions.assertTrue(error.getMessage().contains("Mode"), error.getMessage());
  }

  // </editor-fold>

  // <editor-fold desc="Generic lists">

  static class GenericLists {

    public List<List<String>> nested = List.of();
    public List<? extends Number> wildcard = List.of();
  }

  @Test
  public void nestedAndWildcardListsRoundTrip() {
    GenericLists config =
        load(new GenericLists(), "{\"nested\": [[\"a\", \"b\"], [\"c\"]], \"wildcard\": [1.5]}");
    Assertions.assertEquals(List.of(List.of("a", "b"), List.of("c")), config.nested);
    Assertions.assertEquals(1, config.wildcard.size());

    Assertions.assertEquals(
        """
        {
          "nested": [
            [
              "a",
              "b"
            ],
            [
              "c"
            ]
          ],
          "wildcard": [
            1.5
          ]
        }""",
        dump(config));
  }

  // </editor-fold>
}
