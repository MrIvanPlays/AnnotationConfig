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

package com.mrivanplays.annotationconfig.scenarios.values;

import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** Values of every type a field can have, in every format: numbers, text, booleans and enums. */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestValues extends Scenario {

  TestValues(Format format) {
    super(format, "values");
  }

  // <editor-fold desc="Numbers">

  @Test
  void numbersAreWrittenAndReadBack() {
    Numbers read = loadText(Numbers.zero(), assertDump("numbers", new Numbers()));
    Numbers expected = new Numbers();
    Assertions.assertEquals(expected.b, read.b);
    Assertions.assertEquals(expected.s, read.s);
    Assertions.assertEquals(expected.integer, read.integer);
    Assertions.assertEquals(expected.longValue, read.longValue);
    Assertions.assertEquals(expected.max, read.max);
    Assertions.assertEquals(expected.min, read.min);
    Assertions.assertEquals(expected.single, read.single);
    Assertions.assertEquals(expected.decimal, read.decimal);
    Assertions.assertEquals(expected.big, read.big);
    // YAML read it back as a double, 0.12345678901234568
    Assertions.assertEquals(expected.exact, read.exact);
    Assertions.assertEquals(expected.huge, read.huge);
  }

  @Test
  void numbersAreReadExactly() {
    // Gson reads every number as a double, whose text form uses an exponent from 10^7 on, and
    // Jackson's TOML parser before 2.23 dropped the first 9 digits of 19 digit integers
    Numbers config = load(Numbers.zero(), "exact-numbers");
    Assertions.assertEquals(2_000_000_000, config.integer);
    Assertions.assertEquals(9_007_199_254_740_993L, config.longValue);
    Assertions.assertEquals(Short.MIN_VALUE, config.s);
    Assertions.assertEquals(Long.MAX_VALUE, config.max);
    Assertions.assertEquals(-1_000_000_000_000_000_000L, config.min);
    Assertions.assertEquals(new BigDecimal("0.12345678901234567890"), config.exact);
    Assertions.assertEquals(new BigInteger("123456789012345678901234567890"), config.huge);
  }

  @Test
  void decimalsAreReadAsParsingTheirTextWould() {
    // parsers read decimals as BigDecimal or double, which is converted with doubleValue() /
    // floatValue() instead of through its text; both must give the same value, also when rounding
    String[] texts = {
      "0.1",
      "2.5",
      "0.12345678901234567890",
      "9007199254740993",
      "2.4703282292062328e-324",
      "1.7976931348623159e308",
      "3.4028236e38",
      "7.0064923216240862e-46",
      "-1e-400"
    };
    String template = text("decimal-template");
    for (String text : texts) {
      Floating config = loadText(new Floating(), template.replace("%s", text));
      Assertions.assertEquals(Double.parseDouble(text), config.decimal, text);
      Assertions.assertEquals(Float.parseFloat(text), config.single, text);
    }
  }

  @Test
  void aWholeNumberWithAnExponentIsReadAsItsValue() {
    // it is read as a double, whose text form is "2.0E9"; it used to be read as 2
    Assertions.assertEquals(2_000_000_000, load(new Floating(), "exponent").whole);
  }

  // </editor-fold>

  // <editor-fold desc="Text">

  @Test
  void textIsWrittenAndReadBack() {
    final String written = assertDump("text", new Text());
    Text read = new Text();
    read.plain = null;
    read.symbols = null;
    read.backslash = null;
    read.apostrophe = null;
    read.quotes = null;
    read.newline = null;
    read.tab = null;
    read.control = null;
    read.unicode = null;
    read.empty = null;
    read.character = ' ';
    loadText(read, written);
    Text expected = new Text();
    Assertions.assertEquals(expected.plain, read.plain);
    Assertions.assertEquals(expected.symbols, read.symbols);
    Assertions.assertEquals(expected.backslash, read.backslash);
    Assertions.assertEquals(expected.apostrophe, read.apostrophe);
    Assertions.assertEquals(expected.quotes, read.quotes);
    Assertions.assertEquals(expected.newline, read.newline);
    Assertions.assertEquals(expected.tab, read.tab);
    Assertions.assertEquals(expected.control, read.control);
    Assertions.assertEquals(expected.unicode, read.unicode);
    // an empty string is a value, not a missing one
    Assertions.assertEquals("", read.empty);
    Assertions.assertEquals('x', read.character);
  }

  @Test
  void anEmptyStringIsAValue() {
    // it used to be read as no value: null, or the default with USE_DEFAULT_VALUE
    Assertions.assertEquals("", load(new Text(), "empty-string").plain);
  }

  @Test
  void anEmptyValueIsNoValueForOtherTypes() {
    EmptyValues config = load(new EmptyValues(), "empty-values");
    // a primitive can't hold null, so it keeps its value; this threw an IllegalArgumentException
    Assertions.assertEquals(1, config.number);
    Assertions.assertTrue(config.flag);
    // SET_NULL, the default, sets the others to null
    Assertions.assertNull(config.boxed);
    // and text is a value of its own
    Assertions.assertEquals("", config.text);
    // USE_DEFAULT_VALUE keeps their value
    Settings useDefault =
        new Settings()
            .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.USE_DEFAULT_VALUE);
    Assertions.assertEquals(2, load(new EmptyValues(), "empty-values", useDefault).boxed);
  }

  @Test
  void multilineStringsAreWrittenAndReadBack() {
    final String written = assertDump("multiline", new MultilineText());
    MultilineText read = new MultilineText();
    read.text = null;
    read.tricky = null;
    loadText(read, written);
    Assertions.assertEquals(new MultilineText().text, read.text);
    Assertions.assertEquals(new MultilineText().tricky, read.tricky);
  }

  @Test
  void multilineOnlyFitsStrings() {
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class, () -> dump(new MultilineNumber()));
  }

  // </editor-fold>

  // <editor-fold desc="Booleans and enums">

  @Test
  void booleansAndEnumsAreWrittenAndReadBack() {
    // enums by name(), in lower case, not by the toString() that SPECIAL overrides
    final String written = assertDump("choices", new Choices());
    Choices read = new Choices();
    read.enabled = false;
    read.mode = null;
    read.special = null;
    loadText(read, written);
    Assertions.assertTrue(read.enabled);
    Assertions.assertEquals(Mode.fastMode, read.mode);
    Assertions.assertEquals(Mode.SPECIAL, read.special);
  }

  @Test
  void booleansReadYesAndNo() {
    Assertions.assertFalse(load(new Choices(), "enabled-no").enabled);
    Choices config = new Choices();
    config.enabled = false;
    // yes turned the option off before, in every format but YAML, which reads it as true itself
    Assertions.assertTrue(load(config, "enabled-yes").enabled);
  }

  @Test
  void enumsAreReadCaseInsensitively() {
    Choices config = load(new Choices(), "choices-read");
    Assertions.assertFalse(config.enabled);
    Assertions.assertEquals(Mode.SPECIAL, config.mode);
    Assertions.assertEquals(Mode.fastMode, config.special);
  }

  @Test
  void anUnknownEnumValueNamesTheEnumAndItsValues() {
    Throwable error =
        Assertions.assertThrows(RuntimeException.class, () -> load(new Choices(), "unknown-enum"));
    while (error.getCause() != null && !(error instanceof IllegalArgumentException)) {
      error = error.getCause();
    }
    Assertions.assertTrue(error.getMessage().contains("Mode"), error.getMessage());
    // the constants are listed as they are written, not by toString(), which SPECIAL overrides
    Assertions.assertTrue(error.getMessage().contains("[fastmode, special]"), error.getMessage());
  }

  // </editor-fold>

  // <editor-fold desc="Numbers: subjects">

  enum Mode {
    fastMode,
    SPECIAL {
      @Override
      public String toString() {
        return "something else";
      }
    }
  }

  static class Numbers {

    public byte b = 1;
    public short s = 5;
    public int integer = -42;
    public long longValue = 1234567890123L;
    public long max = Long.MAX_VALUE;
    public long min = Long.MIN_VALUE;
    public float single = 3.11f;
    public double decimal = 2.5;
    public double big = 1e300;
    public BigDecimal exact = new BigDecimal("0.12345678901234567890");
    public BigInteger huge = new BigInteger("123456789012345678901234567890");

    static Numbers zero() {
      Numbers numbers = new Numbers();
      numbers.b = 0;
      numbers.s = 0;
      numbers.integer = 0;
      numbers.longValue = 0;
      numbers.max = 0;
      numbers.min = 0;
      numbers.single = 0;
      numbers.decimal = 0;
      numbers.big = 0;
      numbers.exact = BigDecimal.ZERO;
      numbers.huge = BigInteger.ZERO;
      return numbers;
    }
  }

  // </editor-fold>

  // <editor-fold desc="Text: subjects">

  static class Floating {

    public double decimal = 0;
    public float single = 0;
    public int whole = 0;
  }

  static class EmptyValues {

    public int number = 1;
    public boolean flag = true;
    public Integer boxed = 2;
    public String text = "aa";
  }

  static class Text {

    public String plain = "Hello world";
    public String symbols = "a=b & c<d> 'e' #f :g";
    public String backslash = "C:\\path\\to";
    public String apostrophe = "it's";
    public String quotes = "say \"hi\"";
    public String newline = "line1\nline2";
    public String tab = "a\tb";
    public String control = "bell\u0007";
    public String unicode = "caf\u00e9 \u2603";
    public String empty = "";
    public char character = 'x';
  }

  static class MultilineText {

    @Multiline public String text = "first line\nsecond line\n";

    @Multiline public String tricky = "\"\"\" quotes \\ backslash\r\nend\"";
  }

  // </editor-fold>

  // <editor-fold desc="Booleans and enums: subjects">

  static class MultilineNumber {

    @Multiline public int number = 1;
  }

  static class Choices {

    public boolean enabled = true;
    public Mode mode = Mode.fastMode;
    public Mode special = Mode.SPECIAL;
  }

  // </editor-fold>
}
