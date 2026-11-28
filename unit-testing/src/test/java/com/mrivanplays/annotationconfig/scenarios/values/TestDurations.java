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

import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.serialization.helpers.DurationSerializer;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link DurationSerializer}, registered by this class for {@link Duration}, which no other test
 * class uses, in every format.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestDurations extends Scenario {

  TestDurations(Format format) {
    super(format, "values");
  }

  @BeforeAll
  static void registerSerializer() {
    SerializerRegistry.INSTANCE.registerSerializer(
        Duration.class, new DurationSerializer(ChronoUnit.MINUTES));
  }

  @AfterAll
  static void unregisterSerializer() {
    SerializerRegistry.INSTANCE.unregisterSerializer(Duration.class);
  }

  // <editor-fold desc="Durations">

  @Test
  void durationsAreWrittenExactlyAndReadBack() {
    final String written = assertDump("durations", new Durations());
    Durations read = loadText(Durations.zero(), written);
    Durations expected = new Durations();
    Assertions.assertEquals(expected.timeout, read.timeout);
    Assertions.assertEquals(expected.retry, read.retry);
    Assertions.assertEquals(expected.precise, read.precise);
  }

  @Test
  void everyUnitAndIso8601AreRead() {
    Durations read = load(Durations.zero(), "durations-read");
    Assertions.assertEquals(Duration.ofHours(2), read.timeout);
    Assertions.assertEquals(Duration.ofDays(1), read.retry);
    Assertions.assertEquals(Duration.ofMinutes(90), read.precise);
  }

  // </editor-fold>

  // <editor-fold desc="Durations: subjects">

  static class Durations {

    public Duration timeout = Duration.ofMinutes(5);
    // not a whole number of minutes, so written in seconds
    public Duration retry = Duration.ofSeconds(90);
    public Duration precise = Duration.ofMillis(1500);

    static Durations zero() {
      Durations durations = new Durations();
      durations.timeout = Duration.ZERO;
      durations.retry = Duration.ZERO;
      durations.precise = Duration.ZERO;
      return durations;
    }
  }

  // </editor-fold>
}
