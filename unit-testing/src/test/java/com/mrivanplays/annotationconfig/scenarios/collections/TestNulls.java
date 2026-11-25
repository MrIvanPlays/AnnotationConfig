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

package com.mrivanplays.annotationconfig.scenarios.collections;

import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** Null values and elements, in every format that can write {@code null}; TOML has none. */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_NULLS)
class TestNulls extends Scenario {

  TestNulls(Format format) {
    super(format, "nulls");
  }

  @Test
  void nullElementsAreKept() {
    final String written = assertDump("null-elements", new NullElements());
    NullElements read = new NullElements();
    read.items = new TestCollections.Item[0];
    read.names = List.of();
    loadText(read, written);
    // the null used to be left out, so the array came back shorter
    Assertions.assertArrayEquals(new NullElements().items, read.items);
    // writing a list with a null threw a NullPointerException
    Assertions.assertEquals(Arrays.asList("a", null, "b"), read.names);
  }

  @Test
  void aNullOptionIsAMissingOption() {
    // as in 3.0.x, an option whose value is null counts as missing, so every field keeps its
    // value, also with SET_NULL, the default; an empty value is read as no value instead, see
    // TestValues
    Settings useDefault =
        new Settings()
            .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.USE_DEFAULT_VALUE);
    for (NullValues config :
        new NullValues[] {
          load(new NullValues(), "null-values"), load(new NullValues(), "null-values", useDefault)
        }) {
      Assertions.assertEquals(1, config.number);
      Assertions.assertTrue(config.flag);
      Assertions.assertEquals(2, config.boxed);
      Assertions.assertEquals("aa", config.text);
      // and so does a field of a custom data type; it was set to null with SET_NULL
      Assertions.assertEquals(2, config.holder.boxed);
      Assertions.assertEquals("aa", config.holder.text);
    }
  }

  static class NullValues {

    public int number = 1;
    public boolean flag = true;
    public Integer boxed = 2;
    public String text = "aa";
    public Holder holder = new Holder();
  }

  public static class Holder {

    public Integer boxed = 2;
    public String text = "aa";

    public Holder() {}
  }

  static class NullElements {

    TestCollections.Item[] items = {
      new TestCollections.Item("a", 1), null, new TestCollections.Item("b", 2)
    };
    List<String> names = Arrays.asList("a", null, "b");
  }
}
