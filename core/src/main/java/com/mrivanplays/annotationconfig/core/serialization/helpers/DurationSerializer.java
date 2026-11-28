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

package com.mrivanplays.annotationconfig.core.serialization.helpers;

import com.mrivanplays.annotationconfig.core.serialization.ContextFreeFieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Duration} serializer that writes durations as a whole number followed by a unit, such as
 * {@code 30s}, {@code 5m}, {@code 2h} or {@code 1d}.
 *
 * <p>It is not registered by default. Register it for the {@link Duration} fields of your configs
 * before loading or dumping them:
 *
 * <pre>{@code
 * SerializerRegistry.INSTANCE.registerSerializer(
 *     Duration.class, new DurationSerializer(ChronoUnit.MINUTES));
 * }</pre>
 *
 * <p>A duration is written in the unit passed to the constructor when it is a whole number of it,
 * and otherwise in the largest smaller unit it is a whole number of: with {@link
 * ChronoUnit#MINUTES}, five minutes are written as {@code 5m} and 90 seconds as {@code 90s}. A
 * duration with a fraction of a second is written in ISO-8601 ({@link Duration#toString()}, such as
 * {@code PT1.5S}), so nothing is lost.
 *
 * <p>Reading accepts every unit, whichever one is passed to the constructor: a whole number, which
 * may be negative, followed by {@code s}, {@code m}, {@code h} or {@code d}, ignoring case. It also
 * accepts ISO-8601 durations, as {@link Duration#parse(CharSequence)} reads them ({@code PT1H30M}).
 * An empty value is no value.
 *
 * @see SerializerRegistry#registerSerializer(Class, FieldTypeSerializer)
 * @author Ivan Pekov
 * @since 3.1.1
 */
@NullMarked
public final class DurationSerializer implements ContextFreeFieldTypeSerializer<Duration> {

  // largest first: a duration is written in the first one, from the configured unit on, that it
  // is a whole number of
  private static final List<ChronoUnit> UNITS =
      List.of(ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES, ChronoUnit.SECONDS);

  private final int firstUnit;

  /**
   * Creates a serializer that writes durations in {@code serializeTo} where they are a whole number
   * of it.
   *
   * @param serializeTo the unit to write durations in: {@link ChronoUnit#SECONDS}, {@link
   *     ChronoUnit#MINUTES}, {@link ChronoUnit#HOURS} or {@link ChronoUnit#DAYS}
   * @throws IllegalArgumentException if {@code serializeTo} is another unit
   */
  public DurationSerializer(ChronoUnit serializeTo) {
    Objects.requireNonNull(serializeTo, "serializeTo");
    this.firstUnit = UNITS.indexOf(serializeTo);
    if (this.firstUnit == -1) {
      throw new IllegalArgumentException(
          "Cannot write durations in " + serializeTo + ": expected one of " + UNITS);
    }
  }

  // P..., or -P... / +P..., ignoring case, as Duration#parse reads them
  private static boolean isIso(String text) {
    int start = text.charAt(0) == '-' || text.charAt(0) == '+' ? 1 : 0;
    return text.length() > start && Character.toUpperCase(text.charAt(start)) == 'P';
  }

  private static @Nullable ChronoUnit unitOf(char suffix) {
    return switch (Character.toLowerCase(suffix)) {
      case 's' -> ChronoUnit.SECONDS;
      case 'm' -> ChronoUnit.MINUTES;
      case 'h' -> ChronoUnit.HOURS;
      case 'd' -> ChronoUnit.DAYS;
      default -> null;
    };
  }

  private static String suffixOf(ChronoUnit unit) {
    return String.valueOf(unit.name().toLowerCase(Locale.ROOT).charAt(0));
  }

  @Override
  public @Nullable Duration deserializeSimple(DataObject data) {
    if (data.isEmpty() || !data.isSingleValue()) {
      throw new IllegalArgumentException("Illegal duration to deserialize " + data);
    }
    String text = data.getAsString().trim();
    if (text.isEmpty()) {
      return null;
    }
    try {
      // ISO-8601 first: PT1H also ends in a unit letter
      if (isIso(text)) {
        return Duration.parse(text);
      }
      ChronoUnit unit = unitOf(text.charAt(text.length() - 1));
      if (unit == null) {
        throw new NumberFormatException("no unit");
      }
      return Duration.of(Long.parseLong(text.substring(0, text.length() - 1)), unit);
    } catch (NumberFormatException | DateTimeParseException | ArithmeticException e) {
      throw new IllegalArgumentException(
          "Invalid duration '"
              + text
              + "': expected a whole number followed by s, m, h or d (such as 30s),"
              + " or an ISO-8601 duration (such as PT1H30M)",
          e);
    }
  }

  @Override
  public DataObject serializeSimple(Duration value) {
    if (value.getNano() != 0) {
      // no unit holds it exactly
      return new DataObject(value.toString());
    }
    long seconds = value.getSeconds();
    for (int i = this.firstUnit; i < UNITS.size(); i++) {
      ChronoUnit unit = UNITS.get(i);
      long unitSeconds = unit.getDuration().getSeconds();
      if (seconds % unitSeconds == 0) {
        return new DataObject(seconds / unitSeconds + suffixOf(unit));
      }
    }
    // SECONDS, the last unit, holds every whole number of seconds
    throw new AssertionError(value);
  }
}
