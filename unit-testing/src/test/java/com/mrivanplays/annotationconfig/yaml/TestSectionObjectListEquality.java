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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Two section object lists are equal when they hold the same type and the same entries. */
class TestSectionObjectListEquality {

  private static SectionObjectList<Kit> kits(String first, String second) {
    return SectionObjectList.newBuilderForType(Kit.class)
        .defaultValue(first, new Kit(first.length()))
        .defaultValue(second, new Kit(second.length()))
        .build();
  }

  @Test
  void listsWithTheSameEntriesAreEqual() {
    // identity only before
    Assertions.assertEquals(kits("starter", "vip"), kits("starter", "vip"));
    Assertions.assertEquals(kits("starter", "vip").hashCode(), kits("starter", "vip").hashCode());
    // the order of the entries is not compared, as with maps
    Assertions.assertEquals(kits("starter", "vip"), kits("vip", "starter"));
  }

  @Test
  void differentEntriesOrTypesAreNotEqual() {
    SectionObjectList<Kit> changed = kits("starter", "vip");
    changed.update("vip", new Kit(99));
    Assertions.assertNotEquals(kits("starter", "vip"), changed);
    Assertions.assertNotEquals(kits("starter", "vip"), kits("starter", "pro"));

    SectionObjectList<Object> objects =
        SectionObjectList.newBuilderForType(Object.class)
            .defaultValue("starter", new Kit(7))
            .defaultValue("vip", new Kit(3))
            .build();
    Assertions.assertNotEquals(kits("starter", "vip"), objects);
  }

  @Test
  void toStringShowsTheTypeAndEntries() {
    Assertions.assertEquals(
        "SectionObjectList{type="
            + Kit.class.getName()
            + ", values={starter=Kit[items=7], vip=Kit[items=3]}}",
        kits("starter", "vip").toString());
  }

  record Kit(int items) {}
}
