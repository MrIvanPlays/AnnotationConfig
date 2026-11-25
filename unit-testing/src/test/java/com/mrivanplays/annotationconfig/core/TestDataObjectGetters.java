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
  void numberGettersReadNumbersAsFieldsDo() {
    // a whole number that a format read as a decimal; the getters failed to parse "1.0" before
    assertEquals(1, new DataObject(1.0).getAsInt());
    assertEquals(2L, new DataObject(new BigDecimal("2.0")).getAsLong());
    assertEquals((short) 3, new DataObject("3.9").getAsShort());
    assertEquals((byte) 4, new DataObject(4.0f).getAsByte());
    assertEquals(BigInteger.valueOf(5), new DataObject(5.5).getAsBigInteger());
    assertEquals(2_000_000_000, new DataObject(2.0E9).getAsInt());
    assertEquals(0.1, new DataObject(new BigDecimal("0.1")).getAsDouble());
    assertEquals(3.11f, new DataObject(3.11f).getAsFloat());
    // no number is still an error
    assertThrows(NumberFormatException.class, () -> new DataObject("abc").getAsInt());
    assertThrows(NumberFormatException.class, () -> new DataObject("").getAsLong());
    assertThrows(NumberFormatException.class, () -> new DataObject((Object) null).getAsDouble());
  }

  @Test
  void getAsBooleanReadsYesAndOn() {
    assertEquals(true, new DataObject(true).getAsBoolean());
    assertEquals(true, new DataObject("TRUE").getAsBoolean());
    // yes and on were read as false before
    assertEquals(true, new DataObject("yes").getAsBoolean());
    assertEquals(true, new DataObject("On").getAsBoolean());
    assertEquals(false, new DataObject("no").getAsBoolean());
    assertEquals(false, new DataObject("off").getAsBoolean());
    // anything else is false, as it was
    assertEquals(false, new DataObject("ture").getAsBoolean());
  }

  @Test
  void anEmptyDataObjectIsAnEmptyMap() {
    DataObject empty = new DataObject();
    // it was a single value, null, and not empty before
    assertEquals(false, empty.isSingleValue());
    assertEquals(true, empty.isEmpty());
    assertEquals(Map.of(), empty.getAsMap());
    assertEquals(new DataObject(Map.of()), empty);
  }

  @Test
  void getAsMapOfASingleValueIsMutable() {
    Map<String, Object> map = new DataObject("value").getAsMap();
    map.put("key", 1);
    assertEquals(Map.of("key", 1), map);
  }
}
