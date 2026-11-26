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

import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategies;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategy;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationVersionRange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The registry of migration strategies and the ranges they are registered for. */
class TestMigrationStrategies {

  private static final MigrationStrategy IDENTITY = (values, keyResolver) -> values;

  // the registry is global, other test classes must not see these strategies
  @AfterEach
  void unregister() {
    MigrationStrategies.INSTANCE.unregisterStrategiesFor(Subject.class);
  }

  @Test
  void strategiesAreListedByTheVersionTheyMigrateFrom() {
    MigrationStrategies.INSTANCE.registerStrategyFor(
        Subject.class, MigrationVersionRange.of(3, 4), IDENTITY);
    MigrationStrategies.INSTANCE.registerStrategyFor(
        Subject.class, MigrationVersionRange.of(1, 3), IDENTITY);

    Map<MigrationVersionRange, MigrationStrategy> registered =
        MigrationStrategies.INSTANCE.getStrategiesFor(Subject.class);
    Assertions.assertEquals(
        List.of(MigrationVersionRange.of(1, 3), MigrationVersionRange.of(3, 4)),
        List.copyOf(registered.keySet()));
    Assertions.assertThrows(UnsupportedOperationException.class, registered::clear);
  }

  @Test
  void aSecondStrategyFromTheSameVersionIsRejected() {
    MigrationStrategies.INSTANCE.registerStrategyFor(
        Subject.class, MigrationVersionRange.of(1, 2), IDENTITY);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MigrationStrategies.INSTANCE.registerStrategyFor(
                Subject.class, MigrationVersionRange.of(1, 3), IDENTITY));
  }

  @Test
  void unregisteringNeedsTheRegisteredRange() {
    MigrationStrategies.INSTANCE.registerStrategyFor(
        Subject.class, MigrationVersionRange.of(1, 2), IDENTITY);
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            MigrationStrategies.INSTANCE.unregisterStrategyFor(
                Subject.class, MigrationVersionRange.of(1, 3)));

    MigrationStrategies.INSTANCE.unregisterStrategyFor(
        Subject.class, MigrationVersionRange.of(1, 2));
    Assertions.assertTrue(MigrationStrategies.INSTANCE.getStrategiesFor(Subject.class).isEmpty());
  }

  @Test
  void aRangeGoesUpFromVersion1() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> MigrationVersionRange.of(0, 1));
    Assertions.assertThrows(IllegalArgumentException.class, () -> MigrationVersionRange.of(2, 2));
    Assertions.assertThrows(IllegalArgumentException.class, () -> MigrationVersionRange.of(3, 2));
  }

  private static final class Subject {}
}
