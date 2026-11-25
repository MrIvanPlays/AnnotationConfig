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

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Gson reads every number as a {@code BigDecimal}, so numbers in maps and lists of {@code Object}
 * are read exactly, as that type. What JSON shares with the other formats is in the scenarios.
 */
class TestJsonValues {

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

  @Test
  void numbersInMapsAndListsRoundTripExactly() {
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
        }
        """;
    UntypedNumbers config = load(new UntypedNumbers(), json);
    Assertions.assertEquals(new BigDecimal("1"), config.map.get("whole"));
    Assertions.assertEquals(new BigDecimal("0.12345678901234567890"), config.map.get("decimal"));
    Assertions.assertEquals(List.of(new BigDecimal("1"), new BigDecimal("2.5")), config.objects);
    Assertions.assertEquals(List.of(new BigDecimal("0.12345678901234567890")), config.decimals);
    Assertions.assertEquals(
        List.of(new BigInteger("123456789012345678901234567890")), config.integers);
    Assertions.assertEquals(json, dump(config));
  }

  static class UntypedNumbers {

    public Map<String, Object> map = Map.of();
    public List<Object> objects = List.of();
    public List<BigDecimal> decimals = List.of();
    public List<BigInteger> integers = List.of();
  }
}
