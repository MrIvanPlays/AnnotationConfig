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

package com.mrivanplays.annotationconfig.examples;

import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategies;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationVersionRange;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.yaml.YamlConfig;
import java.io.File;

/**
 * Registers the migrations of {@link ExampleVersionedConfig}, then loads it, or writes the
 * defaults if there is no file yet.
 *
 * <p>A file of an older version is migrated when it is loaded: it is copied to {@code
 * versioned-config.yaml.v<version>.bak} next to it, then rewritten in the current version.
 */
public class ExampleMigrateConfig {

  static void main(String[] args) {
    // register the migrations before the first load: a file loaded without them is read as it
    // is, and a later dump would replace it in the current version without migrating it
    registerMigrations();

    ExampleVersionedConfig config = new ExampleVersionedConfig();
    YamlConfig.getConfigResolver().loadOrDump(config, new File("examples/versioned-config.yaml"));

    // do stuff with config ...
  }

  private static void registerMigrations() {
    MigrationStrategies migrations = MigrationStrategies.INSTANCE;

    // version 2 moved host and port into the section "network". The key resolver is the one of
    // the format the file is read in, so these lines work for YAML, JSON, TOML and .properties
    migrations.registerStrategyFor(
        ExampleVersionedConfig.class,
        MigrationVersionRange.of(1, 2),
        (values, keyResolver) -> {
          values.renameDottedKey("host", "network.host", keyResolver);
          values.renameDottedKey("port", "network.port", keyResolver);
          return values;
        });

    // version 3 made the timeout a duration; it was a number of seconds
    migrations.registerStrategyFor(
        ExampleVersionedConfig.class,
        MigrationVersionRange.of(2, 3),
        (values, keyResolver) -> {
          DataObject timeout = values.get("timeout");
          if (timeout != null) {
            values.put("timeout", timeout.getAsLong() + "s");
          }
          return values;
        });
  }
}
