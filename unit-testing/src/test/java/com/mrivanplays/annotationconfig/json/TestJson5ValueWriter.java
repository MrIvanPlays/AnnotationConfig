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

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The JSON5 syntax of {@link Json5ValueWriter}, and the JSON5 that Gson's lenient reader reads.
 * What JSON5 shares with the other formats is in the scenarios.
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
        }
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
    // a line separator (U+2028) is escaped: JSON5 reads it as a line break in a string
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
        }
        """;
    Assertions.assertEquals(expected, dump(new Values()));
  }

  @Test
  void aNullEntryOfAMapIsLeftOut() {
    // as Gson does for JSON
    Assertions.assertEquals("{\n  map: {\n    key: \"value\"\n  }\n}\n", dump(new NullEntry()));
  }

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
            }
            """);
    Assertions.assertEquals("single quoted", loaded.name);
    Assertions.assertEquals(8080, loaded.port);
    Assertions.assertEquals(List.of("a", "b"), loaded.tags);
  }

  enum Mode {
    FAST,
    SLOW
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

  static class NullEntry {

    public Map<String, Object> map = new LinkedHashMap<>();

    NullEntry() {
      map.put("key", "value");
      map.put("missing", null);
    }
  }

  static class HandWritten {

    public String name = "default";
    public int port = 0;
    public List<String> tags = List.of();
  }
}
