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

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestArray {

  static class Subject {

    char[] charArray = new char[] {'a', 'b', 'c'};
    boolean[] boolArray = new boolean[] {true, false, true};
    int[] intArray = new int[] {7, 8, 9, 10, 11, 12};
    BigInteger[] bigInteger = new BigInteger[] {BigInteger.valueOf(1), BigInteger.valueOf(2)};
    BigDecimal[] bigDecimal = new BigDecimal[] {BigDecimal.valueOf(1.2), BigDecimal.valueOf(3.4)};
    String[] stringArray = new String[] {"make", "america", "great", "again"};
  }

  @Test
  public void testRead() {
    Subject config = new Subject();
    PropertyConfig.getConfigResolver()
        .load(config, getClass().getClassLoader().getResourceAsStream("array-test.properties"));

    Assertions.assertArrayEquals(new int[] {1, 2, 3, 4, 5, 6}, config.intArray);
    Assertions.assertArrayEquals(new char[] {'d', 'e', 'f'}, config.charArray);
    Assertions.assertArrayEquals(new boolean[] {false, true, false}, config.boolArray);
    Assertions.assertArrayEquals(
        new BigInteger[] {BigInteger.valueOf(3), BigInteger.valueOf(4)}, config.bigInteger);
    Assertions.assertArrayEquals(
        new BigDecimal[] {new BigDecimal("5.6"), new BigDecimal("7.8")}, config.bigDecimal);
    Assertions.assertArrayEquals(new String[] {"europe", "is", "peaceful"}, config.stringArray);
  }

  @Test
  public void testWrite() {
    StringWriter writer = new StringWriter();
    PropertyConfig.getConfigResolver().dump(new Subject(), writer);
    String expected =
        "stringArray=make, america, great, again\n"
            + "\n"
            + "bigDecimal=1.2, 3.4\n"
            + "\n"
            + "bigInteger=1, 2\n"
            + "\n"
            + "intArray=7, 8, 9, 10, 11, 12\n"
            + "\n"
            + "boolArray=true, false, true\n"
            + "\n"
            + "charArray=a, b, c\n";
    Assertions.assertEquals(expected, writer.toString().replaceAll("\\R", "\n"));
  }
}
