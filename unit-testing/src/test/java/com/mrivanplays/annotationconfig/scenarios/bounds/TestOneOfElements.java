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
package com.mrivanplays.annotationconfig.scenarios.bounds;

import com.mrivanplays.annotationconfig.core.annotations.OneOf;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code @OneOf} on the element types of lists, maps and record components, in every format with
 * sections. Arrays are in {@code validation.TestOneOf}.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SECTIONS)
class TestOneOfElements extends Scenario {

  TestOneOfElements(Format format) {
    super(format, "bounds");
  }

  private void assertRejected(String input, String position) {
    String message = rejected(new Subject(), input);
    Assertions.assertTrue(message.contains(position), message);
  }

  @Test
  void listedElementsAreLoaded() {
    Subject subject = load(new Subject(), "oneof-in");
    Assertions.assertEquals(Map.of("eu", "small", "us", "large"), subject.servers);
    Assertions.assertEquals(List.of(List.of("a"), List.of("a", "b")), subject.grid);
    Assertions.assertEquals(new Region(List.of("eu")), subject.region);
  }

  @Test
  void theElementsOfAListAreChecked() {
    Assertions.assertEquals(
        List.of("read", "write"), load(new Permissions(), "oneof-permissions-listed").permissions);
    String message = rejected(new Permissions(), "oneof-permissions-unlisted");
    Assertions.assertTrue(message.contains("permissions[1]"), message);
    Assertions.assertTrue(message.contains("'delete'"), message);
  }

  @Test
  void anUnlistedElementIsRejectedWithItsPosition() {
    assertRejected("oneof-key-unlisted", "servers key 'asia'");
    assertRejected("oneof-value-unlisted", "servers.eu");
    assertRejected("oneof-grid-unlisted", "grid[1][1]");
    assertRejected("oneof-record-unlisted", "zones[1]");
  }

  static class Subject {

    Map<@OneOf({"eu", "us"}) String, @OneOf({"small", "large"}) String> servers =
        Map.of("eu", "small");
    List<List<@OneOf({"a", "b"}) String>> grid = List.of(List.of("a"));
    Region region = new Region(List.of("eu"));
  }

  static class Permissions {

    List<@OneOf({"read", "write"}) String> permissions = List.of("read");
  }

  record Region(List<@OneOf({"eu", "us"}) String> zones) {}
}
