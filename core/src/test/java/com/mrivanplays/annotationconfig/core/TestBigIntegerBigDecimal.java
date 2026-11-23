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

package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestBigIntegerBigDecimal {

  @Test
  void testSerialize() {
    StringWriter writer = new StringWriter();
    PropertyConfig.getConfigResolver().dump(new Subject(), writer);

    String expected = "integer=1111111111\n" + "\n" + "decimal=1111111111.22222222\n";
    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  void testDeserialize() {
    Subject config = new Subject();
    PropertyConfig.getConfigResolver()
        .load(
            config,
            getClass().getClassLoader().getResourceAsStream("big-int-big-dec-test.properties"));

    // exact values: both are larger than an int, and the decimal has more digits than a double
    Assertions.assertEquals(new BigDecimal("3333333333.22222222"), config.decimal);
    Assertions.assertEquals(new BigInteger("3333333333"), config.integer);
  }

  @Test
  void dataObjectMapKeepsBigNumbers() {
    // getAsMap() turned a BigDecimal into a double and a BigInteger into an int, which threw
    // beyond the int range
    BigDecimal decimal = new BigDecimal("0.12345678901234567890");
    BigInteger integer = new BigInteger("123456789012345678901234567890");
    Map<String, Object> values = Map.of("decimal", decimal, "integer", integer);
    Map<String, Object> map = new DataObject(values).getAsMap();
    Assertions.assertEquals(decimal, map.get("decimal"));
    Assertions.assertEquals(integer, map.get("integer"));
  }

  static class Subject {

    private BigDecimal decimal = new BigDecimal("1111111111.22222222");
    private BigInteger integer = new BigInteger("1111111111");
  }
}
