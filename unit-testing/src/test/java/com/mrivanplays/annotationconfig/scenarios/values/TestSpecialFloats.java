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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** NaN and the infinities, in every format that writes them. */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SPECIAL_FLOATS)
class TestSpecialFloats extends Scenario {

  TestSpecialFloats(Format format) {
    super(format, "values");
  }

  @Test
  void specialFloatsAreWrittenAndReadBack() {
    final String written = assertDump("special-floats", new SpecialFloats());
    SpecialFloats read = new SpecialFloats();
    read.notANumber = 0;
    read.positiveInfinity = 0;
    read.negativeInfinity = 0;
    read.floatInfinity = 0;
    loadText(read, written);
    Assertions.assertTrue(Double.isNaN(read.notANumber));
    Assertions.assertEquals(Double.POSITIVE_INFINITY, read.positiveInfinity);
    Assertions.assertEquals(Double.NEGATIVE_INFINITY, read.negativeInfinity);
    Assertions.assertEquals(Float.POSITIVE_INFINITY, read.floatInfinity);
  }

  static class SpecialFloats {

    public double notANumber = Double.NaN;
    public double positiveInfinity = Double.POSITIVE_INFINITY;
    public double negativeInfinity = Double.NEGATIVE_INFINITY;
    public float floatInfinity = Float.POSITIVE_INFINITY;
  }
}
