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

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import java.io.StringReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Checks that loading applies {@link Min} and {@link Max}. The comparison rules themselves are
 * covered by {@code internal.MinMaxValidatorTest}.
 */
public class TestMinMax {

  static class NumberSubject {

    @Min(minDouble = -33.9)
    @Max(maxDouble = 44.4)
    private double doubleValue = 33.5;
  }

  static class StringSubject {

    @Min(minInt = 3)
    @Max(maxInt = 7)
    private String value = "Hello";
  }

  static class MisplacedSubject {

    @Min(minInt = 1)
    private boolean value = false;
  }

  private static void load(Object config, String properties) {
    PropertyConfig.getConfigResolver().load(config, new StringReader(properties));
  }

  private static void assertRejected(Object config, String properties) {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> load(config, properties), properties);
  }

  @Test
  public void numbersInRangeAreLoaded() {
    NumberSubject config = new NumberSubject();
    load(config, "doubleValue=-33.9");
    Assertions.assertEquals(-33.9, config.doubleValue);
    load(config, "doubleValue=44.4");
    Assertions.assertEquals(44.4, config.doubleValue);
  }

  @Test
  public void numbersOutOfRangeAreRejected() {
    assertRejected(new NumberSubject(), "doubleValue=-34.6");
    assertRejected(new NumberSubject(), "doubleValue=45.2");
  }

  @Test
  public void stringsAreCheckedByLength() {
    StringSubject config = new StringSubject();
    load(config, "value=abc");
    Assertions.assertEquals("abc", config.value);
    assertRejected(new StringSubject(), "value=a");
    assertRejected(new StringSubject(), "value=This exceeds the max limit set");
  }

  @Test
  public void annotationOnInvalidTypeIsRejected() {
    assertRejected(new MisplacedSubject(), "value=false");
  }
}
