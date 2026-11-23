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

package com.mrivanplays.annotationconfig.core.internal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class TestMinMaxValidator {

  private static FieldInfo field(String name) throws NoSuchFieldException {
    return new FieldInfo(Subject.class.getDeclaredField(name));
  }

  private static void ok(String field, Object value) throws Exception {
    FieldInfo f = field(field);
    assertDoesNotThrow(() -> MinMaxValidator.validate(f, field, value), field + " = " + value);
  }

  private static String fails(String field, Object value) throws Exception {
    FieldInfo f = field(field);
    String message =
        assertThrows(
                IllegalArgumentException.class,
                () -> MinMaxValidator.validate(f, field, value),
                field + " = " + value)
            .getMessage();
    assertTrue(message.contains(field), message);
    return message;
  }

  @Test
  void decimalBoundsAreNotTruncated() throws Exception {
    fails("decimal", 1.9);
    fails("decimal", 0.2);
    ok("decimal", 1.5);
    ok("decimal", 0.5);
  }

  @Test
  void longsAreNotOverflowed() throws Exception {
    fails("bigLong", 3_000_000_000L);
    ok("bigLong", 10L);
  }

  @Test
  void bigNumbersCompareExactly() throws Exception {
    fails("bigLong", new BigInteger("100000000000000000000"));
    fails("decimal", new BigDecimal("1.5000000000000001"));
  }

  @Test
  void floatBoundUsesItsDecimalValue() throws Exception {
    ok("smallFloat", 0.1f);
    ok("smallFloat", 0.1);
    fails("smallFloat", 0.09);
  }

  @Test
  void integerBounds() throws Exception {
    fails("plain", 1);
    ok("plain", 2);
    ok("plain", 5);
    fails("plain", 6);
  }

  @Test
  void minusOneIsAUsableBound() throws Exception {
    ok("minusOne", -1);
    fails("minusOne", -2);
    fails("minusOne", 0);
  }

  @Test
  void stringsAreCheckedByLength() throws Exception {
    fails("text", "ab");
    ok("text", "abc");
    ok("text", "abcdefg");
    fails("text", "abcdefgh");
  }

  @Test
  void invalidAnnotations() throws Exception {
    assertTrue(fails("twoMins", 3).contains("only one"));
    assertTrue(fails("noMin", 3).contains("should set an annotation member"));
    assertTrue(fails("twoMaxes", 0).contains("only one"));
    assertTrue(fails("misplaced", true).contains("invalid field type"));
  }

  @Test
  void nothingToCheck() throws Exception {
    ok("unannotated", 1_000);
    // a value read as null used to be reported as a misplaced annotation
    ok("plain", null);
  }

  @SuppressWarnings("unused")
  private static final class Subject {

    @Min(minDouble = 0.5)
    @Max(maxDouble = 1.5)
    double decimal;

    @Max(maxLong = 10)
    long bigLong;

    @Min(minFloat = 0.1f)
    float smallFloat;

    @Min(minInt = 2)
    @Max(maxInt = 5)
    int plain;

    @Min(minInt = -1)
    @Max(maxInt = -1)
    int minusOne;

    @Min(minInt = 3)
    @Max(maxInt = 7)
    String text;

    @Min(minInt = 1, minLong = 2)
    int twoMins;

    @Min int noMin;

    @Max(maxByte = 1, maxShort = 2)
    int twoMaxes;

    @Min(minInt = 1)
    boolean misplaced;

    int unannotated;
  }
}
