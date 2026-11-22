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
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDefaultSerializerTypes {

  enum Mode {
    fastMode,
    SPECIAL {
      @Override
      public String toString() {
        return "something else";
      }
    }
  }

  public static class Item {

    public String name;
    public int amount;
  }

  static class Subject {

    public List<Long> longs;
    public List<List<String>> nested;
    public List<? extends Number> wildcard;
    public Item[] items;
    public Mode mode;
    public Mode special;
    public BigInteger bigInteger;
    public BigDecimal bigDecimal;
    public int whole;
  }

  static class EnumSubject {

    public Mode mode = Mode.fastMode;
    public Mode special = Mode.SPECIAL;
  }

  private static final ConfigResolver RESOLVER = YamlConfig.getConfigResolver();

  private static Subject load(String yaml) {
    Subject subject = new Subject();
    RESOLVER.load(subject, new StringReader(yaml));
    return subject;
  }

  @Test
  public void listElementsAreConvertedToTheDeclaredType() {
    Subject subject = load("longs: [1, 2]");
    // snakeyaml reads these as Integer; they used to end up in the List<Long> unconverted
    Assertions.assertEquals(List.of(1L, 2L), subject.longs);
    Assertions.assertEquals(Long.class, ((Object) subject.longs.get(0)).getClass());
  }

  @Test
  public void parameterizedAndWildcardListElements() {
    Subject subject = load("nested: [[a, b], [c]]\nwildcard: [1, 2.5]");
    Assertions.assertEquals(List.of(List.of("a", "b"), List.of("c")), subject.nested);
    Assertions.assertEquals(2, subject.wildcard.size());
  }

  @Test
  public void objectArrayHasTheDeclaredComponentType() {
    Subject subject = load("items:\n  - name: a\n    amount: 1\n  - name: b\n    amount: 2");
    Assertions.assertEquals(Item[].class, subject.items.getClass());
    Assertions.assertEquals("b", subject.items[1].name);
    Assertions.assertEquals(2, subject.items[1].amount);
  }

  @Test
  public void enumsRoundTripByName() {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(new EnumSubject(), writer);
    String dumped = writer.toString();
    // name(), not the overridden toString()
    Assertions.assertTrue(dumped.contains("special: \"special\""), dumped);

    EnumSubject loaded = new EnumSubject();
    loaded.mode = null;
    loaded.special = null;
    RESOLVER.load(loaded, new StringReader(dumped));
    Assertions.assertEquals(Mode.fastMode, loaded.mode);
    Assertions.assertEquals(Mode.SPECIAL, loaded.special);
  }

  @Test
  public void unknownEnumValueNamesTheEnum() {
    Exception e = Assertions.assertThrows(Exception.class, () -> load("mode: nope"));
    Throwable cause = e;
    while (cause.getCause() != null && !(cause instanceof IllegalArgumentException)) {
      cause = cause.getCause();
    }
    Assertions.assertTrue(cause.getMessage().contains("Mode"), cause.getMessage());
  }

  @Test
  public void wholeNumberWithExponentIsReadAsItsValue() {
    // SnakeYAML reads this as a double, whose text form is "2.0E9"; it used to be read as 2
    Assertions.assertEquals(2_000_000_000, load("whole: 2.0e9").whole);
  }

  @Test
  public void bigNumbersKeepFullPrecision() {
    Subject subject =
        load("bigInteger: 123456789012345678901234567890\nbigDecimal: '0.12345678901234567890'");
    Assertions.assertEquals(new BigInteger("123456789012345678901234567890"), subject.bigInteger);
    Assertions.assertEquals(new BigDecimal("0.12345678901234567890"), subject.bigDecimal);
  }
}
