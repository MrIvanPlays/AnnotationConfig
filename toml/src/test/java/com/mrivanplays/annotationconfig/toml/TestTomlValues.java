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

package com.mrivanplays.annotationconfig.toml;

import static com.mrivanplays.annotationconfig.toml.TomlTesting.dump;
import static com.mrivanplays.annotationconfig.toml.TomlTesting.load;
import static com.mrivanplays.annotationconfig.toml.TomlTesting.parse;
import static com.mrivanplays.annotationconfig.toml.TomlTesting.roundTrip;

import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Writing and reading values of the different types as TOML 1.0. */
class TestTomlValues {

  // <editor-fold desc="Strings">

  @Test
  void stringsAreLiteralUnlessTheyNeedEscaping() {
    // literal strings can't hold ' or control characters other than tab: those are basic strings
    String expected =
        """
        plain = 'Hello world'

        backslash = 'C:\\path\\to'

        apostrophe = "it's"

        quotes = 'say "hi"'

        newline = "line1\\nline2"

        tab = 'a\tb'

        control = "bell\\u0007"

        unicode = 'caf\u00e9 \u2603'

        character = 'x'
        """;
    Assertions.assertEquals(expected, dump(new Strings()));
  }

  @Test
  void stringsRoundTrip() {
    Strings config = new Strings();
    config.plain = "\"both' kinds\\";
    config.character = '\'';
    Strings loaded = roundTrip(config, new Strings());
    Assertions.assertEquals("\"both' kinds\\", loaded.plain);
    Assertions.assertEquals("C:\\path\\to", loaded.backslash);
    Assertions.assertEquals("it's", loaded.apostrophe);
    Assertions.assertEquals("say \"hi\"", loaded.quotes);
    Assertions.assertEquals("line1\nline2", loaded.newline);
    Assertions.assertEquals("a\tb", loaded.tab);
    Assertions.assertEquals("bell\u0007", loaded.control);
    Assertions.assertEquals("caf\u00e9 \u2603", loaded.unicode);
    Assertions.assertEquals('\'', loaded.character);
  }

  @Test
  void emptyStringsRoundTrip() {
    Strings config = new Strings();
    config.plain = "";
    MultilineStrings multiline = new MultilineStrings();
    multiline.text = "";

    String toml = dump(config);
    Assertions.assertTrue(toml.contains("plain = ''\n"), toml);
    Assertions.assertEquals("", load(new Strings(), toml).plain);
    Assertions.assertEquals("", roundTrip(multiline, new MultilineStrings()).text);
  }

  @Test
  void multilineStringsAreMultiLineBasicStrings() {
    String expected =
        "text = \"\"\"\n"
            + "first line\n"
            + "second line\n"
            + "\"\"\"\n"
            + "\n"
            // the third quote in a row is escaped, so it does not close the string
            + "tricky = \"\"\"\n"
            + "\"\"\\\" quotes \\\\ backslash\\r\n"
            + "end\"\"\"\"\n";
    String toml = dump(new MultilineStrings());
    Assertions.assertEquals(expected, toml);

    MultilineStrings loaded = load(new MultilineStrings(), toml);
    Assertions.assertEquals("first line\nsecond line\n", loaded.text);
    Assertions.assertEquals("\"\"\" quotes \\ backslash\r\nend\"", loaded.tricky);
  }

  @Test
  void numbersUseTomlSyntax() {
    String expected =
        """
        integer = -42

        longValue = 1234567890123

        decimal = 2.5

        single = 3.11

        big = 1.0E300

        positiveInfinity = inf

        negativeInfinity = -inf

        notANumber = nan

        floatInfinity = inf

        exact = 0.12345678901234567890

        fitting = 42
        """;
    Assertions.assertEquals(expected, dump(new Numbers()));
  }

  @Test
  void numbersRoundTrip() {
    Numbers loaded = roundTrip(new Numbers(), new Numbers());
    Assertions.assertEquals(-42, loaded.integer);
    Assertions.assertEquals(1234567890123L, loaded.longValue);
    Assertions.assertEquals(2.5, loaded.decimal);
    Assertions.assertEquals(3.11f, loaded.single);
    Assertions.assertEquals(1e300, loaded.big);
    Assertions.assertEquals(Double.POSITIVE_INFINITY, loaded.positiveInfinity);
    Assertions.assertEquals(Double.NEGATIVE_INFINITY, loaded.negativeInfinity);
    Assertions.assertTrue(Double.isNaN(loaded.notANumber));
    Assertions.assertEquals(Float.POSITIVE_INFINITY, loaded.floatInfinity);
    Assertions.assertEquals(new BigDecimal("0.12345678901234567890"), loaded.exact);
    Assertions.assertEquals(BigInteger.valueOf(42), loaded.fitting);
  }

  // </editor-fold>

  // <editor-fold desc="Numbers">

  @Test
  void integersBeyond64BitsAreStrings() {
    // TOML integers are 64 bit: the limits of long are integers, anything larger is a string
    String toml = dump(new OutOfRange());
    Assertions.assertEquals(
        """
        max = 9223372036854775807

        min = -9223372036854775808

        huge = '123456789012345678901234567890'
        """,
        toml);

    OutOfRange target = new OutOfRange();
    target.max = 0;
    target.min = 0;
    target.huge = BigInteger.ZERO;
    load(target, toml);
    Assertions.assertEquals(Long.MAX_VALUE, target.max);
    Assertions.assertEquals(Long.MIN_VALUE, target.min);
    Assertions.assertEquals(new BigInteger("123456789012345678901234567890"), target.huge);
  }

  @Test
  void nineteenDigitIntegersAreReadExactly() {
    // Jackson's TOML parser before 2.23 dropped the first 9 digits of these
    OutOfRange loaded =
        load(new OutOfRange(), "max = 9223372036854775807\nmin = -1000000000000000000");
    Assertions.assertEquals(Long.MAX_VALUE, loaded.max);
    Assertions.assertEquals(-1_000_000_000_000_000_000L, loaded.min);
  }

  @Test
  void datesAreTomlDateTimes() {
    // seconds are always written: TOML requires them, though toString() leaves out ":00"
    String expected =
        """
        offsetDateTime = 2021-11-10T17:31:00+02:00

        localDateTime = 2021-11-10T17:31:48.5

        localDate = 2021-11-10

        localTime = 17:31:00

        dates = [2000-01-01]
        """;
    String toml = dump(new Dates());
    Assertions.assertEquals(expected, toml);
    Assertions.assertInstanceOf(OffsetDateTime.class, parse(toml).get("offsetDateTime"));
  }

  @Test
  void datesRoundTrip() {
    Dates loaded = roundTrip(new Dates(), new Dates());
    Assertions.assertEquals(
        OffsetDateTime.parse("2021-11-10T17:31:00+02:00"), loaded.offsetDateTime);
    Assertions.assertEquals(LocalDateTime.parse("2021-11-10T17:31:48.5"), loaded.localDateTime);
    Assertions.assertEquals(LocalDate.parse("2021-11-10"), loaded.localDate);
    Assertions.assertEquals(LocalTime.parse("17:31"), loaded.localTime);
    Assertions.assertEquals(List.of(LocalDate.parse("2000-01-01")), loaded.dates);
  }

  @Test
  void datesWrittenAsStringsStillLoad() {
    // files written before dates were written natively
    Dates loaded =
        load(
            new Dates(),
            """
            offsetDateTime = '2020-01-02T03:04:05Z'
            localDateTime = '2020-01-02T03:04'
            localDate = '2020-01-02'
            localTime = '03:04'
            """);
    Assertions.assertEquals(OffsetDateTime.parse("2020-01-02T03:04:05Z"), loaded.offsetDateTime);
    Assertions.assertEquals(LocalDateTime.parse("2020-01-02T03:04"), loaded.localDateTime);
    Assertions.assertEquals(LocalDate.parse("2020-01-02"), loaded.localDate);
    Assertions.assertEquals(LocalTime.parse("03:04"), loaded.localTime);
  }

  @Test
  @SuppressWarnings("unchecked")
  void otherFormatsStillGetDatesAsStrings() {
    // the serializers are shared by all formats; only the TOML resolver has MAPPER_KEY
    TomlConfig.getConfigResolver(); // registers the date serializers
    FieldTypeSerializer<OffsetDateTime> serializer =
        (FieldTypeSerializer<OffsetDateTime>)
            SerializerRegistry.INSTANCE.getSerializer(OffsetDateTime.class).orElseThrow();
    OffsetDateTime value = OffsetDateTime.parse("2021-11-10T17:31:48+02:00");
    DataObject serialized =
        serializer.serialize(
            value,
            SerializationContext.of(
                OffsetDateTime.class,
                OffsetDateTime.class,
                new Object(),
                ACDefaultSettings.getDefault().copy()),
            AnnotationAccessor.EMPTY);
    Assertions.assertEquals("2021-11-10T17:31:48+02:00", serialized.getAsObject());
  }

  // </editor-fold>

  // <editor-fold desc="Date and time">

  @Test
  void booleansAndEnums() {
    String expected =
        """
        enabled = true

        mode = 'fast'

        modes = ['slow', 'fast']
        """;
    Assertions.assertEquals(expected, dump(new Misc()));

    Misc loaded = load(new Misc(), "enabled = false\nmode = 'SLOW'\nmodes = ['fast']");
    Assertions.assertFalse(loaded.enabled);
    Assertions.assertEquals(Mode.SLOW, loaded.mode);
    Assertions.assertEquals(List.of(Mode.FAST), loaded.modes);
  }

  enum Mode {
    FAST,
    SLOW
  }

  static class Strings {

    public String plain = "Hello world";
    public String backslash = "C:\\path\\to";
    public String apostrophe = "it's";
    public String quotes = "say \"hi\"";
    public String newline = "line1\nline2";
    public String tab = "a\tb";
    public String control = "bell\u0007";
    public String unicode = "caf\u00e9 \u2603";
    public char character = 'x';
  }

  static class MultilineStrings {

    @Multiline public String text = "first line\nsecond line\n";

    @Multiline public String tricky = "\"\"\" quotes \\ backslash\r\nend\"";
  }

  static class Numbers {

    public int integer = -42;
    public long longValue = 1234567890123L;
    public double decimal = 2.5;
    public float single = 3.11f;
    public double big = 1e300;
    public double positiveInfinity = Double.POSITIVE_INFINITY;
    public double negativeInfinity = Double.NEGATIVE_INFINITY;
    public double notANumber = Double.NaN;
    public float floatInfinity = Float.POSITIVE_INFINITY;
    public BigDecimal exact = new BigDecimal("0.12345678901234567890");
    public BigInteger fitting = BigInteger.valueOf(42);
  }

  // </editor-fold>

  // <editor-fold desc="Booleans and enums">

  static class OutOfRange {

    public long max = Long.MAX_VALUE;
    public long min = Long.MIN_VALUE;
    public BigInteger huge = new BigInteger("123456789012345678901234567890");
  }

  static class Dates {

    public OffsetDateTime offsetDateTime = OffsetDateTime.parse("2021-11-10T17:31:00+02:00");
    public LocalDateTime localDateTime = LocalDateTime.parse("2021-11-10T17:31:48.5");
    public LocalDate localDate = LocalDate.parse("2021-11-10");
    public LocalTime localTime = LocalTime.parse("17:31");
    public List<LocalDate> dates = List.of(LocalDate.parse("2000-01-01"));
  }

  static class Misc {

    public boolean enabled = true;
    public Mode mode = Mode.FAST;
    public List<Mode> modes = List.of(Mode.SLOW, Mode.FAST);
  }

  // </editor-fold>
}
