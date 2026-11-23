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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TestDataObjectGetters {

  @Test
  void getListKeepsSubclassesOfTheTypeAndSkipsNulls() {
    DataObject data = new DataObject(Arrays.asList(1, 2L, "text", null, 3.5));
    assertEquals(List.of(1, 2L, 3.5), data.getList(Number.class));
    assertEquals(List.of("text"), data.getList(String.class));
  }

  @Test
  void getAsCharReadsOneCharacterStrings() {
    assertEquals('a', new DataObject("a").getAsChar());
    assertEquals('b', new DataObject('b').getAsChar());
    assertThrows(IllegalArgumentException.class, () -> new DataObject("ab").getAsChar());
  }

  @Test
  void bigNumbersAreReadExactly() {
    String decimal = "0.1000000000000000055511151231257827";
    assertEquals(new BigDecimal(decimal), new DataObject(decimal).getAsBigDecimal());
    String integer = "123456789012345678901234567890";
    assertEquals(new BigInteger(integer), new DataObject(integer).getAsBigInteger());
  }

  @Test
  void getAsMapOfASingleValueIsMutable() {
    Map<String, Object> map = new DataObject("value").getAsMap();
    map.put("key", 1);
    assertEquals(Map.of("key", 1), map);
  }
}
