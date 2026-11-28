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

import static com.mrivanplays.annotationconfig.toml.TomlTesting.RESOLVER;

import com.mrivanplays.annotationconfig.core.annotations.ConfigVersion;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategies;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationVersionRange;
import java.io.StringReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A migration of a TOML file of 3.0.x, which wrote dotted keys as one quoted key. The migrations of
 * every format are the {@code migration} scenarios.
 */
class TestTomlMigration {

  // the subject is used only here, so no other class sees this strategy
  @BeforeAll
  static void registerStrategy() {
    // version 2 moved server.port to network.port
    MigrationStrategies.INSTANCE.registerStrategyFor(
        MovedSection.class,
        MigrationVersionRange.of(1, 2),
        (values, keyResolver) -> {
          values.renameDottedKey("server.port", "network.port", keyResolver);
          return values;
        });
  }

  @AfterAll
  static void unregisterStrategy() {
    MigrationStrategies.INSTANCE.unregisterStrategiesFor(MovedSection.class);
  }

  @Test
  void aQuotedKeyOf30IsMoved() {
    // TOML's key resolver reads a dotted key as one quoted key first
    MovedSection config = new MovedSection();
    RESOLVER.load(
        config, new StringReader("'server.host' = \"example.org\"\n'server.port' = 25565\n"));
    Assertions.assertEquals("example.org", config.host);
    Assertions.assertEquals(25565, config.port);
  }

  @ConfigVersion(2)
  static class MovedSection {

    @Key("server.host")
    String host = "localhost";

    @Key("network.port")
    int port = 8080;
  }
}
