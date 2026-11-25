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

package com.mrivanplays.annotationconfig.scenarios.values;

import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Numbers in maps and lists of {@code Object}, whose type is what the parser reads, in every format
 * that writes integers beyond 64 bits as numbers; TOML writes them as strings.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_BIG_INTEGERS)
class TestUntypedNumbers extends Scenario {

  private static final BigInteger HUGE = new BigInteger("123456789012345678901234567890");

  TestUntypedNumbers(Format format) {
    super(format, "values");
  }

  @Test
  void numbersInMapsAndListsAreWrittenAndReadBack() {
    // numbers in maps were read as doubles (1 came back as 1.0), and numbers in lists were written
    // as doubles, or as strings beyond the long range
    final String written = assertDump("untyped-numbers", new UntypedNumbers());
    UntypedNumbers read = new UntypedNumbers();
    read.map = Map.of();
    read.objects = List.of();
    read.integers = List.of();
    loadText(read, written);
    UntypedNumbers expected = new UntypedNumbers();
    Assertions.assertEquals(numbersAsDecimals(expected.map), numbersAsDecimals(read.map));
    Assertions.assertEquals(numbersAsDecimals(expected.objects), numbersAsDecimals(read.objects));
    Assertions.assertEquals(expected.integers, read.integers);
  }

  @Test
  void bigIntegersInMapsAreRead() {
    // values in maps were turned into ints, which threw beyond the int range
    UntypedNumbers config = load(new UntypedNumbers(), "untyped-numbers-read");
    Assertions.assertEquals(new BigDecimal(HUGE), numbersAsDecimals(config.map.get("big")));
    Assertions.assertEquals(BigDecimal.ONE, numbersAsDecimals(config.map.get("small")));
  }

  static class UntypedNumbers {

    public Map<String, Object> map = map();
    public List<Object> objects = List.of(1, 2.5);
    public List<BigInteger> integers = List.of(HUGE);

    private static Map<String, Object> map() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("whole", 1);
      map.put("decimal", 2.5);
      // YAML read it back as the double 0.12345678901234568
      map.put("exact", new BigDecimal("0.12345678901234567890"));
      map.put("big", HUGE);
      return map;
    }
  }
}
