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
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Checks that loading applies {@link Min} and {@link Max}. The comparison rules themselves are
 * covered by {@code internal.TestMinMaxValidator}.
 */
class TestMinMax {

  private static void load(Object config, String properties) {
    PropertyConfig.getConfigResolver().load(config, new StringReader(properties));
  }

  private static void assertRejected(Object config, String properties) {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> load(config, properties), properties);
  }

  private static Object deserialize(Class<?> type, Map<String, ?> values) {
    return SerializerRegistry.INSTANCE
        .getDefaultSerializer()
        .deserialize(
            new DataObject((Object) values, true),
            SerializationContext.of(type, type, null),
            AnnotationAccessor.EMPTY);
  }

  @Test
  void numbersInRangeAreLoaded() {
    NumberSubject config = new NumberSubject();
    load(config, "doubleValue=-33.9");
    Assertions.assertEquals(-33.9, config.doubleValue);
    load(config, "doubleValue=44.4");
    Assertions.assertEquals(44.4, config.doubleValue);
  }

  @Test
  void numbersOutOfRangeAreRejected() {
    assertRejected(new NumberSubject(), "doubleValue=-34.6");
    assertRejected(new NumberSubject(), "doubleValue=45.2");
  }

  @Test
  void stringsAreCheckedByLength() {
    StringSubject config = new StringSubject();
    load(config, "value=abc");
    Assertions.assertEquals("abc", config.value);
    assertRejected(new StringSubject(), "value=a");
    assertRejected(new StringSubject(), "value=This exceeds the max limit set");
  }

  @Test
  void annotationOnInvalidTypeIsRejected() {
    assertRejected(new MisplacedSubject(), "value=false");
  }

  // In custom data types and records, as for the fields of a config and its config objects.

  @Test
  void theFieldOfAOneFieldDataTypeIsChecked() {
    load(new WithLimit(), "limit=5");
    assertRejected(new WithLimit(), "limit=50");
  }

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
  void theElementsOfAnArrayAreChecked() {
    Weights config = new Weights();
    load(config, "weights=1, 10");
    Assertions.assertArrayEquals(new int[] {1, 10}, config.weights);
    // an @Max on an array field failed with "annotation placed on invalid field type" before
    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> load(new Weights(), "weights=1, 11"));
    Assertions.assertTrue(error.getMessage().contains("weights[1]"), error.getMessage());
  }

  @Test
  void theElementsOfAListAreChecked() {
    Assertions.assertEquals(
        List.of(1, 2),
        ((Ports) deserialize(Ports.class, Map.of("ports", List.of(1, 2)))).ports);
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

  static class WithLimit {

    Limit limit = new Limit();
  }

  static class Limit {

    @Max(maxInt = 10)
    int value = 1;
  }

  static class Range {

    int low;

    @Max(maxInt = 100)
    int high;
  }

  record Bounds(@Min(minInt = 0) int min, int max) {}

  static class Weights {

    @Max(maxInt = 10)
    int[] weights = {1};
  }

  static class Ports {

    List<@Max(maxInt = 65535) Integer> ports = List.of(25565);

    Ports() {}
  }

  static class OnTheList {

    @Max(maxInt = 3)
    List<Integer> values = List.of(1);

    OnTheList() {}
  }

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
}
