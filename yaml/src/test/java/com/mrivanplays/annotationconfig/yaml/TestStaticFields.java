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

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestStaticFields {

  private static final ConfigResolver RESOLVER = YamlConfig.getConfigResolver();

  public static class Subject {

    public static int counter = 5;
    public int value = 1;
  }

  @AfterEach
  public void resetStatic() {
    Subject.counter = 5;
  }

  private static Settings findStatic() {
    return new Settings().put(ACDefaultSettings.FIND_STATIC_FIELDS, true);
  }

  @Test
  public void staticFieldsAreSkippedByDefault() {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(new Subject(), writer);
    String dumped = writer.toString();
    Assertions.assertFalse(dumped.contains("counter"), dumped);
    Assertions.assertTrue(dumped.contains("value: 1"), dumped);

    Subject loaded = new Subject();
    RESOLVER.load(loaded, new StringReader("counter: 9\nvalue: 3"));
    Assertions.assertEquals(5, Subject.counter);
    Assertions.assertEquals(3, loaded.value);
  }

  @Test
  public void staticFieldsAreOptionsWhenEnabled() {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(new Subject(), writer, findStatic());
    String dumped = writer.toString();
    Assertions.assertTrue(dumped.contains("counter: 5"), dumped);

    RESOLVER.load(new Subject(), new StringReader("counter: 9\nvalue: 3"), findStatic());
    Assertions.assertEquals(9, Subject.counter);
  }
}
