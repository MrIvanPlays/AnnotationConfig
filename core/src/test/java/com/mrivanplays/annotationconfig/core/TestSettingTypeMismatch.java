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

import com.mrivanplays.annotationconfig.core.resolver.settings.Setting;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestSettingTypeMismatch {

  static final Setting<String> STR_S = Setting.of("string", String.class);
  static final Setting<Boolean> BOOL_S = Setting.of("string", Boolean.class);

  @Test
  void testMismatch() {
    Settings settings = new Settings();
    settings.put(STR_S, "foo");

    Assertions.assertThrows(IllegalArgumentException.class, () -> settings.put(BOOL_S, true));
  }

  @Test
  void testReplacements() {
    Settings settings = new Settings();
    settings.put(STR_S, "foo");

    Assertions.assertEquals("foo", settings.get(STR_S).orElseThrow());

    settings.put(STR_S, "bar");
    Assertions.assertEquals("bar", settings.get(STR_S).orElseThrow());
  }
}
