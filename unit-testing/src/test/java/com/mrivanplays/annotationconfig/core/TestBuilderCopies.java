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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Setting;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.DataObjectBuilder;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Copies of builders are independent of the original, and settings compare by key and type. */
class TestBuilderCopies {

  @Test
  void dataObjectBuilderCopyIsIndependent() {
    DataObjectBuilder original =
        new DataObjectBuilder().with("a", 1).with("section", new DataObjectBuilder().with("b", 2));
    DataObjectBuilder copy = original.copy();
    copy.with("c", 3);
    DataObject section = copy.build().get("section");
    section.put("d", 4);

    assertEquals(Map.of("a", 1, "section", Map.of("b", 2)), original.build().getAsMap());
  }

  @Test
  void configResolverBuilderCopyIsIndependent() {
    ConfigResolver.Builder original =
        ConfigResolver.newBuilder()
            .withValueReader((reader, settings) -> Map.of())
            .withValueWriter((values, comments, writer, settings) -> {})
            .withSetting(ACDefaultSettings.FIND_STATIC_FIELDS, false)
            .withFileExtension(".a");
    original.copy().withSetting(ACDefaultSettings.FIND_STATIC_FIELDS, true);

    assertFalse(
        original.build().settings().get(ACDefaultSettings.FIND_STATIC_FIELDS).orElseThrow());
  }

  @Test
  void settingsWithTheSameKeyAndTypeAreEqual() {
    Setting<String> unnamed = Setting.of(String.class);
    assertEquals(unnamed, Setting.of(String.class));
    assertEquals(unnamed.hashCode(), Setting.of(String.class).hashCode());
    assertEquals(Setting.of("key", String.class), Setting.of("key", String.class));
    assertNotEquals(Setting.of("key", String.class), Setting.of("Key", String.class));
    assertNotEquals(Setting.of("key", String.class), Setting.of("key", Object.class));
    assertTrue(Setting.of("key", String.class).toString().contains("key"));
  }
}
