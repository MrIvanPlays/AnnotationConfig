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

package com.mrivanplays.annotationconfig.yaml;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * SnakeYAML reads every decimal as a {@code double}; the YAML resolver reads one that a double
 * can't hold exactly as a {@code BigDecimal}, so that it reads back what was written. That a
 * {@code BigDecimal} field reads back is in the {@code values} scenarios.
 */
class TestYamlDecimals {

  @Test
  void onlyDecimalsADoubleCantHoldAreBigDecimals() {
    Subject subject = new Subject();
    YamlConfig.getConfigResolver()
        .load(
            subject,
            new StringReader(
                """
                values:
                  plain: 2.5
                  exact: 0.12345678901234567890
                  underscored: 1_000.5
                  exponent: 6.8523015e+5
                  sexagesimal: 190:20:30.15
                """));
    // read as before
    Assertions.assertEquals(2.5, subject.values.get("plain"));
    Assertions.assertEquals(1000.5, subject.values.get("underscored"));
    Assertions.assertEquals(685230.15, subject.values.get("exponent"));
    // a YAML 1.1 form that BigDecimal doesn't read
    Assertions.assertEquals(685230.15, subject.values.get("sexagesimal"));
    // read as the double 0.12345678901234568 before
    Assertions.assertEquals(
        new BigDecimal("0.12345678901234567890"), subject.values.get("exact"));
  }

  static class Subject {

    Map<String, Object> values = Map.of();
  }
}
