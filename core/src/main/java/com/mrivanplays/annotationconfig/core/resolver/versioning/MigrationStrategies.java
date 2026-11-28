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

package com.mrivanplays.annotationconfig.core.resolver.versioning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NullMarked;

/**
 * The registry of the {@link MigrationStrategy migration strategies} of config classes that have a
 * {@link com.mrivanplays.annotationconfig.core.annotations.ConfigVersion}.
 *
 * <p>Strategies are registered for the class that carries the annotation, one per version they
 * migrate from. Loading a file of version 1 into a class of version 3 uses the strategy from
 * version 1, then the one from the version that strategy migrates to, until version 3 is reached.
 *
 * <p>Register the strategies of a class before the first config of it is loaded. A class without
 * any strategy reads an older file without migrating it, and the file can then be replaced without
 * the migration: a dump, or a load with {@code GENERATE_NEW_OPTIONS} that adds a missing option,
 * writes the current version with only the values that were read, and makes no backup. Later loads
 * count the file as current, so the strategies are never applied to it.
 *
 * <p>The registry is safe to use from several threads: every method is atomic, and a load uses the
 * strategies that were registered when it started.
 *
 * @author Ivan Pekov
 * @since 3.1.0
 */
@NullMarked
public enum MigrationStrategies {
  INSTANCE;

  // per class, the strategies by the version they migrate from. The inner maps are never modified,
  // but replaced, so a load reads one consistent chain.
  private final Map<Class<?>, NavigableMap<Integer, Registered>> strategies =
      new ConcurrentHashMap<>();

  /**
   * Registers {@code strategy} to migrate the files of {@code configClass} over {@code range}.
   *
   * <p>Call it before a config of {@code configClass} is loaded: a file loaded before is read
   * without migrating it, and may then be replaced in the current version (see {@link
   * MigrationStrategies}).
   *
   * @param configClass the class that carries {@code @ConfigVersion}
   * @param range the versions the strategy migrates between
   * @param strategy the strategy
   * @throws IllegalArgumentException if a strategy from {@code range.from()} is registered already
   */
  public void registerStrategyFor(
      Class<?> configClass, MigrationVersionRange range, MigrationStrategy strategy) {
    Objects.requireNonNull(configClass, "configClass");
    Objects.requireNonNull(range, "range");
    Objects.requireNonNull(strategy, "strategy");
    this.strategies.compute(
        configClass,
        (type, registered) -> {
          if (registered != null && registered.containsKey(range.from())) {
            throw new IllegalArgumentException(
                "A migration of "
                    + type.getName()
                    + " from version "
                    + range.from()
                    + " is already registered: "
                    + registered.get(range.from()).range());
          }
          NavigableMap<Integer, Registered> copy =
              registered == null ? new TreeMap<>() : new TreeMap<>(registered);
          copy.put(range.from(), new Registered(range, strategy));
          return Collections.unmodifiableNavigableMap(copy);
        });
  }

  /**
   * Removes the strategy of {@code configClass} registered for {@code range}.
   *
   * @param configClass the class the strategy is registered for
   * @param range the range it is registered for
   * @throws IllegalArgumentException if no strategy is registered for that range
   */
  public void unregisterStrategyFor(Class<?> configClass, MigrationVersionRange range) {
    Objects.requireNonNull(configClass, "configClass");
    Objects.requireNonNull(range, "range");
    this.strategies.compute(
        configClass,
        (type, registered) -> {
          Registered found = registered == null ? null : registered.get(range.from());
          if (found == null || !found.range().equals(range)) {
            throw new IllegalArgumentException(
                "No migration of " + type.getName() + " is registered for " + range);
          }
          if (registered.size() == 1) {
            return null;
          }
          NavigableMap<Integer, Registered> copy = new TreeMap<>(registered);
          copy.remove(range.from());
          return Collections.unmodifiableNavigableMap(copy);
        });
  }

  /**
   * Removes every strategy registered for {@code configClass}, if there are any.
   *
   * @param configClass the class the strategies are registered for
   */
  public void unregisterStrategiesFor(Class<?> configClass) {
    Objects.requireNonNull(configClass, "configClass");
    this.strategies.remove(configClass);
  }

  /**
   * Returns the strategies registered for {@code configClass}, by the version they migrate from.
   *
   * @param configClass the class the strategies are registered for
   * @return the strategies, ordered by the version they migrate from; one snapshot of the registry,
   *     which can't be modified
   */
  public Map<MigrationVersionRange, MigrationStrategy> getStrategiesFor(Class<?> configClass) {
    Objects.requireNonNull(configClass, "configClass");
    NavigableMap<Integer, Registered> registered = this.strategies.get(configClass);
    if (registered == null) {
      return Collections.emptyMap();
    }
    Map<MigrationVersionRange, MigrationStrategy> ret = new LinkedHashMap<>();
    for (Registered entry : registered.values()) {
      ret.put(entry.range(), entry.strategy());
    }
    return Collections.unmodifiableMap(ret);
  }

  /** A registered strategy and its range. */
  private record Registered(MigrationVersionRange range, MigrationStrategy strategy) {}
}
