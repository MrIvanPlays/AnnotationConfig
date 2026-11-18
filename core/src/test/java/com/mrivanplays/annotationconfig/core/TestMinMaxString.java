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
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMinMaxString {

  private static final class MinMaxTest {

    @Min(minInt = 3)
    @Max(maxInt = 7)
    private String value = "Hello";

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  private static final File file = new File("min-max-string.properties");
  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testMin() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          MinMaxTest configSubject = new MinMaxTest();
          resolver.dump(configSubject, file);

          configSubject.setValue("a");
          file.delete();
          resolver.dump(configSubject, file);
          resolver.load(configSubject, file);
        });
  }

  @Test
  public void testMax() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          MinMaxTest configSubject = new MinMaxTest();
          resolver.dump(configSubject, file);

          configSubject.setValue("This exceeds the max limit set");
          file.delete();
          resolver.dump(configSubject, file);
          resolver.load(configSubject, file);
        });
  }
}
