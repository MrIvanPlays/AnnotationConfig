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

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** {@code @Min} and {@code @Max} on the element types of lists, maps and record components. */
class TestYamlElementBounds {

  private static final ConfigResolver RESOLVER = YamlConfig.getConfigResolver();

  private static Subject load(String yaml) {
    Subject subject = new Subject();
    RESOLVER.load(subject, new StringReader(yaml));
    return subject;
  }

  private static String rejected(String yaml) {
    return Assertions.assertThrows(IllegalArgumentException.class, () -> load(yaml), yaml)
        .getMessage();
  }

  @Test
  void elementsInBoundsAreLoaded() {
    Subject subject =
        load(
            """
            ports: [25565, 25566]
            names: {admin: Ivan}
            grid: [[1, 2], [3]]
            weights: [0.5]
            server: {ports: [80]}
            """);
    Assertions.assertEquals(List.of(25565, 25566), subject.ports);
    Assertions.assertEquals(Map.of("admin", "Ivan"), subject.names);
    Assertions.assertEquals(List.of(List.of(1, 2), List.of(3)), subject.grid);
    Assertions.assertEquals(new Server(List.of(80)), subject.server);
  }

  @Test
  void anElementOutOfBoundsIsRejectedWithItsPosition() {
    Assertions.assertTrue(rejected("ports: [1, 70000]").contains("ports[1]"));
    // a map value: its key in the message; strings by length
    Assertions.assertTrue(rejected("names: {admin: Iv}").contains("names.admin"));
    // nested lists
    Assertions.assertTrue(rejected("grid: [[1], [2, 11]]").contains("grid[1][1]"));
    // the bound of a wildcard
    Assertions.assertTrue(rejected("weights: [2.5]").contains("weights[0]"));
    // a list in a record component
    Assertions.assertTrue(rejected("server: {ports: [0]}").contains("ports[0]"));
  }

  @Test
  void mapKeysCanBeBounded() {
    Keys keys = new Keys();
    RESOLVER.load(keys, new StringReader("values: {ab: 1}"));
    Assertions.assertEquals(Map.of("ab", 1), keys.values);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> RESOLVER.load(new Keys(), new StringReader("values: {abcdef: 1}")));
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
