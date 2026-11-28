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

package com.mrivanplays.annotationconfig.scenarios.validation;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.OneOf;
import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import com.mrivanplays.annotationconfig.core.resolver.ConfigLoadException;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** {@code REPORT_ALL_ERRORS} names the options of sections by their path. */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SECTIONS)
class TestReportAllErrorsInSections extends Scenario {

  TestReportAllErrorsInSections(Format format) {
    super(format, "validation");
  }

  private List<String> keys(Object config, String input) {
    ConfigLoadException e =
        Assertions.assertThrows(
            ConfigLoadException.class, () -> load(config, input, TestReportAllErrors.reportAll()));
    return TestReportAllErrors.keys(e);
  }

  // <editor-fold desc="Sections">

  @Test
  void theOptionsOfSectionsAreReportedByTheirPath() {
    Network config = new Network();

    Assertions.assertEquals(
        List.of("limits.players", "limits.inner.level", "shape", "name"),
        keys(config, "errors-sections"));
    // read although an option before it failed
    Assertions.assertEquals(3, config.limits.inner.retries);
  }

  @Test
  void theOptionsOfAPolymorphicSectionAreReportedOneByOne() {
    Assertions.assertEquals(
        List.of("shape.radius", "shape.label"), keys(new Network(), "errors-polymorphic"));
  }

  // </editor-fold>

  // <editor-fold desc="Sections: subjects">

  interface Shape {}

  static class Network {

    @ConfigObject Limits limits = new Limits();

    @Polymorphic(@Polymorphic.Subtype(name = "circle", type = Circle.class))
    Shape shape = new Circle();

    @OneOf({"a", "b"})
    String name = "a";
  }

  static class Limits {

    @Max(maxInt = 10)
    int players = 5;

    @ConfigObject Inner inner = new Inner();
  }

  static class Inner {

    TestReportAllErrors.Level level = TestReportAllErrors.Level.EASY;

    int retries = 1;
  }

  static class Circle implements Shape {

    @Min(minInt = 1)
    int radius = 1;

    @OneOf({"round", "flat"})
    String label = "round";
  }

  // </editor-fold>
}
