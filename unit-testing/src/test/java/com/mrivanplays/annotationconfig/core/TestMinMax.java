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
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link Min} and {@link Max} on values the default serializer reads without a file: the fields of
 * custom data types, record components and list elements. The values a format reads are checked by
 * the {@code validation} scenarios, the comparison rules by {@code internal.TestMinMaxValidator}.
 */
class TestMinMax {

  private static Object deserialize(Class<?> type, Map<String, ?> values) {
    return SerializerRegistry.INSTANCE
        .getDefaultSerializer()
        .deserialize(
            new DataObject((Object) values, true),
            SerializationContext.of(type, type, null),
            AnnotationAccessor.EMPTY);
  }

  // In custom data types and records, as for the fields of a config and its config objects.

  @Test
  void theFieldsOfADataTypeAreChecked() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> deserialize(Range.class, Map.of("low", 1, "high", 500)));
    Range range = (Range) deserialize(Range.class, Map.of("low", 1, "high", 50));
    Assertions.assertEquals(50, range.high);
  }

  @Test
  void theComponentsOfARecordAreChecked() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> deserialize(Bounds.class, Map.of("min", -5, "max", 10)));
    Assertions.assertEquals(
        new Bounds(0, 10), deserialize(Bounds.class, Map.of("min", 0, "max", 10)));
  }

  // Bounds on element types, new in 3.1.0.

  @Test
  void theElementsOfAListAreChecked() {
    Assertions.assertEquals(
        List.of(1, 2), ((Ports) deserialize(Ports.class, Map.of("ports", List.of(1, 2)))).ports);
    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> deserialize(Ports.class, Map.of("ports", List.of(1, 70_000))));
    Assertions.assertTrue(error.getMessage().contains("ports[1]"), error.getMessage());
  }

  @Test
  void aBoundOnTheListItselfPointsToTheElementType() {
    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> deserialize(OnTheList.class, Map.of("values", List.of(1))));
    Assertions.assertTrue(
        error.getMessage().contains("List<@Max(...) Integer>"), error.getMessage());
  }

  static class Range {

    int low;

    @Max(maxInt = 100)
    int high;
  }

  record Bounds(@Min(minInt = 0) int min, int max) {}

  static class Ports {

    List<@Max(maxInt = 65535) Integer> ports = List.of(25565);

    Ports() {}
  }

  static class OnTheList {

    @Max(maxInt = 3)
    List<Integer> values = List.of(1);

    OnTheList() {}
  }
}
