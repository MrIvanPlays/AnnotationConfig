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

import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * TOML date-times: written natively when the settings hold {@code MAPPER_KEY}, as the TOML resolver
 * does, and as strings in the other formats.
 */
class TestTomlDates {

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

  // <editor-fold desc="subjects">

  static class Dates {

    public OffsetDateTime offsetDateTime = OffsetDateTime.parse("2021-11-10T17:31:00+02:00");
    public LocalDateTime localDateTime = LocalDateTime.parse("2021-11-10T17:31:48.5");
    public LocalDate localDate = LocalDate.parse("2021-11-10");
    public LocalTime localTime = LocalTime.parse("17:31");
    public List<LocalDate> dates = List.of(LocalDate.parse("2000-01-01"));
  }

  // </editor-fold>
}
