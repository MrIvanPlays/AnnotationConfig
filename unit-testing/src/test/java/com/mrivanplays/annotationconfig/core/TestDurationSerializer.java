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

import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.helpers.DurationSerializer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TestDurationSerializer {

  private static String write(ChronoUnit unit, Duration duration) {
    // the serializer uses neither the context nor the annotations
    return new DurationSerializer(unit)
        .serialize(duration, null, AnnotationAccessor.EMPTY)
        .getAsString();
  }

  private static Duration read(String text) {
    return new DurationSerializer(ChronoUnit.SECONDS)
        .deserialize(new DataObject(text), null, AnnotationAccessor.EMPTY);
  }

  // <editor-fold desc="Writing">

  @Test
  void aDurationIsWrittenInTheUnitItIsAWholeNumberOf() {
    Assertions.assertEquals("5m", write(ChronoUnit.MINUTES, Duration.ofMinutes(5)));
    Assertions.assertEquals("2h", write(ChronoUnit.HOURS, Duration.ofHours(2)));
    Assertions.assertEquals("3d", write(ChronoUnit.DAYS, Duration.ofDays(3)));
    Assertions.assertEquals("30s", write(ChronoUnit.SECONDS, Duration.ofSeconds(30)));
    // never in a larger unit than the one asked for
    Assertions.assertEquals("120s", write(ChronoUnit.SECONDS, Duration.ofMinutes(2)));
    Assertions.assertEquals("0m", write(ChronoUnit.MINUTES, Duration.ZERO));
    Assertions.assertEquals("-5m", write(ChronoUnit.MINUTES, Duration.ofMinutes(-5)));
  }

  @Test
  void aDurationIsWrittenInASmallerUnitWhenItIsNotAWholeNumberOfTheUnit() {
    // these were cut to 1m, 0m and 1d
    Assertions.assertEquals("90s", write(ChronoUnit.MINUTES, Duration.ofSeconds(90)));
    Assertions.assertEquals("30s", write(ChronoUnit.MINUTES, Duration.ofSeconds(30)));
    Assertions.assertEquals("36h", write(ChronoUnit.DAYS, Duration.ofHours(36)));
    Assertions.assertEquals("-90s", write(ChronoUnit.MINUTES, Duration.ofSeconds(-90)));
  }

  @Test
  void aFractionOfASecondIsWrittenInIso8601() {
    Duration duration = Duration.ofMillis(1500);
    String written = write(ChronoUnit.SECONDS, duration);
    Assertions.assertEquals("PT1.5S", written);
    Assertions.assertEquals(duration, read(written));
  }

  @ParameterizedTest
  @EnumSource(
      value = ChronoUnit.class,
      names = {"SECONDS", "MINUTES", "HOURS", "DAYS"},
      mode = EnumSource.Mode.EXCLUDE)
  void aUnitItCannotWriteIsRejected(ChronoUnit unit) {
    // it was accepted, and the first dump failed
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class, () -> new DurationSerializer(unit));
  }

  // </editor-fold>

  // <editor-fold desc="Reading">

  @Test
  void everyUnitIsRead() {
    Assertions.assertEquals(Duration.ofSeconds(30), read("30s"));
    Assertions.assertEquals(Duration.ofMinutes(5), read("5m"));
    Assertions.assertEquals(Duration.ofHours(2), read("2h"));
    Assertions.assertEquals(Duration.ofDays(1), read("1d"));
    Assertions.assertEquals(Duration.ofMinutes(-5), read("-5m"));
    Assertions.assertEquals(Duration.ofHours(2), read(" 2H "));
  }

  @Test
  void iso8601IsRead() {
    Assertions.assertEquals(Duration.ofMinutes(90), read("PT1H30M"));
    Assertions.assertEquals(Duration.ofHours(-1), read("-pt1h"));
    Assertions.assertEquals(Duration.ofDays(2), read("P2D"));
  }

  @Test
  void anEmptyValueIsNoValue() {
    // it failed with a StringIndexOutOfBoundsException
    Assertions.assertNull(read(""));
  }

  @Test
  void anInvalidDurationSaysWhatIsExpected() {
    for (String text : new String[] {"5", "5x", "m", "1.5m", "500ms", "PT", "99999999999999999d"}) {
      IllegalArgumentException error =
          Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> read(text), text);
      Assertions.assertTrue(error.getMessage().contains("'" + text + "'"), error.getMessage());
      Assertions.assertTrue(error.getMessage().contains("PT1H30M"), error.getMessage());
    }
  }

  // </editor-fold>
}
