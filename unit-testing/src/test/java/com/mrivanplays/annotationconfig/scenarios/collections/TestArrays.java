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
package com.mrivanplays.annotationconfig.scenarios.collections;

import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** Arrays of each element type, in every format, also those without sections. */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestArrays extends Scenario {

  TestArrays(Format format) {
    super(format, "collections");
  }

  @Test
  void arraysAreWrittenAndReadBack() {
    final String written = assertDump("arrays", new ArrayTypes());
    ArrayTypes read = new ArrayTypes();
    read.charArray = new char[0];
    read.boolArray = new boolean[0];
    read.intArray = new int[0];
    read.bigInteger = new BigInteger[0];
    read.bigDecimal = new BigDecimal[0];
    read.stringArray = new String[0];
    loadText(read, written);
    ArrayTypes expected = new ArrayTypes();
    Assertions.assertArrayEquals(expected.charArray, read.charArray);
    Assertions.assertArrayEquals(expected.boolArray, read.boolArray);
    Assertions.assertArrayEquals(expected.intArray, read.intArray);
    Assertions.assertArrayEquals(expected.bigInteger, read.bigInteger);
    Assertions.assertArrayEquals(expected.bigDecimal, read.bigDecimal);
    Assertions.assertArrayEquals(expected.stringArray, read.stringArray);
  }

  @Test
  void arraysLoad() {
    ArrayTypes config = load(new ArrayTypes(), "arrays-read");
    Assertions.assertArrayEquals(new int[] {1, 2, 3, 4, 5, 6}, config.intArray);
    Assertions.assertArrayEquals(new char[] {'d', 'e', 'f'}, config.charArray);
    Assertions.assertArrayEquals(new boolean[] {false, true, false}, config.boolArray);
    Assertions.assertArrayEquals(
        new BigInteger[] {BigInteger.valueOf(3), BigInteger.valueOf(4)}, config.bigInteger);
    Assertions.assertArrayEquals(
        new BigDecimal[] {new BigDecimal("5.6"), new BigDecimal("7.8")}, config.bigDecimal);
    Assertions.assertArrayEquals(new String[] {"europe", "is", "peaceful"}, config.stringArray);
  }

  // <editor-fold desc="subjects">

  static class ArrayTypes {

    char[] charArray = {'a', 'b', 'c'};
    boolean[] boolArray = {true, false, true};
    int[] intArray = {7, 8, 9, 10, 11, 12};
    BigInteger[] bigInteger = {BigInteger.valueOf(1), BigInteger.valueOf(2)};
    BigDecimal[] bigDecimal = {new BigDecimal("1.2"), new BigDecimal("3.4")};
    String[] stringArray = {"make", "america", "great", "again"};
  }

  // </editor-fold>
}
