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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Reading and writing values of the different supported types as JSON. */
public class TestJsonValues {

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
