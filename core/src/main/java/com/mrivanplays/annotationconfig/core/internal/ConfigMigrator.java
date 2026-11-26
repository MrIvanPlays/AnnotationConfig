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

package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.annotations.ConfigVersion;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategies;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategy;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationVersionRange;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Brings the values read for a {@code @ConfigVersion} class up to its version with the registered
 * {@link MigrationStrategy migration strategies}, and copies a migrated file before it is
 * rewritten.
 *
 * @hidden
 */
@NullMarked
final class ConfigMigrator {

  // System.Logger: no logging library is needed, and applications can route it to theirs
  private static final System.Logger LOGGER =
      System.getLogger("com.mrivanplays.annotationconfig.migration");

  private ConfigMigrator() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * The checked {@code @ConfigVersion} of {@code type}, {@code null} if it has none.
   *
   * @throws IllegalArgumentException if the version is below 1 or the key is empty or dotted
   */
  static @Nullable ConfigVersion versionOf(Class<?> type) {
    ConfigVersion version = ClassInfo.of(type).version();
    if (version == null) {
      return null;
    }
    if (version.value() < 1) {
      throw new IllegalArgumentException(
          "@ConfigVersion of " + type.getName() + " must be at least 1, got " + version.value());
    }
    String key = version.key();
    if (key.isEmpty() || key.indexOf('.') >= 0) {
      throw new IllegalArgumentException(
          "The key of @ConfigVersion of "
              + type.getName()
              + " must be neither empty nor dotted, got '"
              + key
              + "'");
    }
    return version;
  }

  /**
   * Migrates {@code values}, read for {@code config}, to the version of its class.
   *
   * @param settings the settings the values are read with; their key resolver is passed to the
   *     strategies
   * @param source what the values were read from, for error messages
   * @return the values to set the fields from: {@code values} itself if nothing was migrated
   * @throws IllegalArgumentException if the version in the values is invalid or newer than the
   *     class's
   * @throws IllegalStateException if a strategy is missing on the way or fails
   */
  static Result migrate(
      Object config, Map<String, Object> values, Settings settings, String source) {
    Class<?> type = config.getClass();
    ConfigVersion version = versionOf(type);
    // an empty file has no options to migrate: it misses all of them, the version included
    if (version == null || values.isEmpty()) {
      return new Result(values, 0, false);
    }
    String key = version.key();
    int current = version.value();
    Object stored = values.get(key);
    // a file written before the class had a version is version 1
    int fileVersion = stored == null ? 1 : parseVersion(stored, key, source);
    if (fileVersion > current) {
      throw new IllegalArgumentException(
          source
              + " is of version "
              + fileVersion
              + ", newer than version "
              + current
              + " of "
              + type.getName());
    }
    if (fileVersion == current) {
      return new Result(values, 0, stored == null);
    }

    // the whole chain is found first, so a missing step fails before any strategy runs
    Class<?> versionClass = ClassInfo.of(type).versionClass();
    //noinspection DataFlowIssue: a class with a version has the class that declares it
    Map<MigrationVersionRange, MigrationStrategy> registered =
        MigrationStrategies.INSTANCE.getStrategiesFor(versionClass);
    if (registered.isEmpty()) {
      // the class migrates nothing: the values are read as they are, and the file is neither
      // copied nor rewritten. A chain with a step missing still fails below.
      LOGGER.log(
          System.Logger.Level.WARNING,
          "{0} is of version {1}, older than version {2} of {3}, but no migration of {4} is"
              + " registered: it is read without migrating it",
          source,
          fileVersion,
          current,
          type.getName(),
          versionClass.getName());
      return new Result(values, 0, false);
    }
    Map<Integer, Map.Entry<MigrationVersionRange, MigrationStrategy>> byFrom = new HashMap<>();
    for (Map.Entry<MigrationVersionRange, MigrationStrategy> entry : registered.entrySet()) {
      byFrom.put(entry.getKey().from(), entry);
    }
    List<Map.Entry<MigrationVersionRange, MigrationStrategy>> steps = new ArrayList<>();
    int at = fileVersion;
    while (at < current) {
      Map.Entry<MigrationVersionRange, MigrationStrategy> step = byFrom.get(at);
      if (step == null) {
        throw new IllegalStateException(
            "Cannot migrate "
                + source
                + " from version "
                + fileVersion
                + " to version "
                + current
                + ": no migration of "
                + versionClass.getName()
                + " from version "
                + at
                + " is registered");
      }
      if (step.getKey().to() > current) {
        throw new IllegalStateException(
            "The migration "
                + step.getKey()
                + " of "
                + versionClass.getName()
                + " goes beyond its version "
                + current);
      }
      steps.add(step);
      at = step.getKey().to();
    }

    // a mutable copy: the strategies can't change the values that were read
    DataObject tree = new DataObject(values, false);
    tree.remove(key);
    KeyResolver keyResolver = settings.get(ACDefaultSettings.KEY_RESOLVER).orElseThrow();
    for (Map.Entry<MigrationVersionRange, MigrationStrategy> step : steps) {
      MigrationVersionRange range = step.getKey();
      DataObject migrated;
      try {
        migrated = step.getValue().apply(tree, keyResolver);
      } catch (RuntimeException e) {
        throw new IllegalStateException(
            "The migration " + range + " of " + source + " failed: " + e.getMessage(), e);
      }
      if (migrated == null || migrated.isSingleValue()) {
        throw new IllegalStateException(
            "The migration " + range + " of " + source + " returned no values");
      }
      tree = migrated;
    }
    return new Result(tree.getAsMap(), fileVersion, false);
  }

  /** The version {@code stored} under {@code key}: a whole number, also as text. */
  private static int parseVersion(Object stored, String key, String source) {
    // .properties reads every value as text, the other parsers as numbers of their own types
    String text = stored instanceof Number || stored instanceof String ? stored.toString() : null;
    if (text != null) {
      try {
        int version = new BigDecimal(text.trim()).intValueExact();
        if (version >= 1) {
          return version;
        }
      } catch (NumberFormatException | ArithmeticException e) {
        // invalid, as below
      }
    }
    throw new IllegalArgumentException(
        "Invalid version '" + stored + "' under the key '" + key + "' of " + source);
  }

  /**
   * Copies {@code path} to {@code <name>.v<version>.bak} next to it, or to {@code
   * <name>.v<version>.bak.<n>} if that exists, keeping its attributes. The name doesn't end with
   * the config's extension, so {@code resolveMultiple} doesn't read the copy.
   *
   * @return the copy
   */
  static Path backup(Path path, int version) {
    String name = path.getFileName().toString() + ".v" + version + ".bak";
    try {
      for (int n = 1; ; n++) {
        Path backup = path.resolveSibling(n == 1 ? name : name + "." + n);
        try {
          // follows a symbolic link: the copy holds the content of the file it points to
          return Files.copy(path, backup, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (FileAlreadyExistsException e) {
          // an older copy is kept, the next name is tried
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not back up " + path + " before migrating it", e);
    }
  }

  /**
   * The values to set the fields from.
   *
   * @param fromVersion the version the values were migrated from; 0 if they were not migrated
   * @param versionMissing whether the values are current but lack the version, so it is a missing
   *     option
   */
  record Result(Map<String, Object> values, int fromVersion, boolean versionMissing) {

    boolean migrated() {
      return this.fromVersion != 0;
    }
  }
}
