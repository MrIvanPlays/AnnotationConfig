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

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestProperties {

  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  // <editor-fold desc="Lists">

  @Test
  void aListFailsToLoadAsItFailsToDump() {
    // the array read from a value with a comma used to become one element, "[Ljava.lang...@..."
    IllegalArgumentException load =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> resolver.load(new WithList(), new StringReader("allowed-users=a, b\n")));
    Assertions.assertEquals(
        "'allowed-users': .properties does not support lists; use an array, such as String[]",
        load.getMessage());
    IllegalArgumentException dump =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> resolver.dump(new WithList(), new StringWriter()));
    Assertions.assertEquals(
        "'allowed-users': .properties does not support lists", dump.getMessage());
  }

  @Test
  void aListOfNumbersFailsToLoad() {
    IllegalArgumentException e =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> resolver.load(new WithNumbers(), new StringReader("ports=1, 2\n")));
    Assertions.assertEquals(
        "'ports': .properties does not support lists; use an array, such as Integer[]",
        e.getMessage());
  }

  @Test
  void aSingleValueStillLoadsIntoAList() {
    WithList read = new WithList();
    resolver.load(read, new StringReader("allowed-users=a\n"));
    Assertions.assertEquals(List.of("a"), read.users);
  }

  // </editor-fold>

  // <editor-fold desc="Lists: subjects">

  static class WithList {

    @Key("allowed-users")
    List<String> users = List.of("x");
  }

  static class WithNumbers {

    List<Integer> ports = List.of(1);
  }

  // </editor-fold>
}
