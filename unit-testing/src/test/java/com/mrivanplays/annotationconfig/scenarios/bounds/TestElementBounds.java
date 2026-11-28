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

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
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
 * {@code @Min} and {@code @Max} on the element types of lists, maps and record components, in every
 * format with sections.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SECTIONS)
class TestElementBounds extends Scenario {

  TestElementBounds(Format format) {
    super(format, "bounds");
  }

  private void assertRejected(String input, String position) {
    String message = rejected(new Subject(), input);
    Assertions.assertTrue(message.contains(position), message);
  }

  @Test
  void elementsInBoundsAreLoaded() {
    Subject subject = load(new Subject(), "in-bounds");
    Assertions.assertEquals(List.of(25565, 25566), subject.ports);
    Assertions.assertEquals(Map.of("admin", "Ivan"), subject.names);
    Assertions.assertEquals(List.of(List.of(1, 2), List.of(3)), subject.grid);
    Assertions.assertEquals(new Server(List.of(80)), subject.server);
  }

  @Test
  void anElementOutOfBoundsIsRejectedWithItsPosition() {
    assertRejected("port-out-of-bounds", "ports[1]");
    // a map value: its key in the message; strings by length
    assertRejected("name-too-short", "names.admin");
    // nested lists
    assertRejected("grid-out-of-bounds", "grid[1][1]");
    // the bound of a wildcard
    assertRejected("weight-out-of-bounds", "weights[0]");
    // a list in a record component
    assertRejected("record-port-out-of-bounds", "ports[0]");
  }

  @Test
  void mapKeysCanBeBounded() {
    Assertions.assertEquals(Map.of("ab", 1), load(new Keys(), "key-in-bounds").values);
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> load(new Keys(), "key-out-of-bounds"));
  }

  static class Subject {

    List<@Max(maxInt = 65535) Integer> ports = List.of(25565);
    Map<String, @Min(minInt = 3) String> names = Map.of("admin", "Ivan");
    List<List<@Max(maxInt = 10) Integer>> grid = List.of(List.of(1));
    List<? extends @Max(maxDouble = 1.0) Number> weights = List.of(0.5);
    Server server = new Server(List.of(80));
  }

  static class Keys {

    Map<@Max(maxInt = 4) String, Integer> values = Map.of("ab", 1);
  }

  record Server(List<@Min(minInt = 1) Integer> ports) {}
}
