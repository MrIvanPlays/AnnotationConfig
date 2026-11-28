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

package com.mrivanplays.annotationconfig.scenarios.migration;

import com.mrivanplays.annotationconfig.core.annotations.ConfigVersion;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.CommentPlaceholder;
import com.mrivanplays.annotationconfig.core.resolver.CommentPlaceholderRegistry;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategies;
import com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationVersionRange;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code @ConfigVersion} in every format: the version is written first, older files are migrated
 * step by step with the registered strategies, a migrated file is rewritten after a copy of it is
 * made, and files that can't be migrated fail without being changed.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestMigration extends Scenario {

  TestMigration(Format format) {
    super(format, "migration");
  }

  // the subjects are used only here, so no other class sees these strategies
  @BeforeAll
  static void registerStrategies() {
    MigrationStrategies strategies = MigrationStrategies.INSTANCE;
    // version 2 renamed port to server-port
    strategies.registerStrategyFor(
        Server.class,
        MigrationVersionRange.of(1, 2),
        (values, keyResolver) -> {
          values.renameKey("port", "server-port");
          return values;
        });
    // version 3 made the timeout a duration, which was a number of seconds
    strategies.registerStrategyFor(
        Server.class,
        MigrationVersionRange.of(2, 3),
        (values, keyResolver) -> {
          DataObject timeout = values.get("timeout");
          if (timeout != null) {
            values.put("timeout", timeout.getAsLong() + "s");
          }
          return values;
        });
    // version 2 moved server.port to network.port; the key resolver is the format's
    strategies.registerStrategyFor(
        MovedSection.class,
        MigrationVersionRange.of(1, 2),
        (values, keyResolver) -> {
          values.renameDottedKey("server.port", "network.port", keyResolver);
          return values;
        });
    // a chain with a gap: nothing migrates from version 1
    strategies.registerStrategyFor(
        MissingStep.class, MigrationVersionRange.of(2, 3), (values, keyResolver) -> values);
    // records which key resolver it got
    strategies.registerStrategyFor(
        SeesKeyResolver.class,
        MigrationVersionRange.of(1, 2),
        (values, keyResolver) -> {
          values.put("seen", keyResolver instanceof MarkedKeyResolver ? "passed" : "default");
          return values;
        });
    strategies.registerStrategyFor(
        FailingMigration.class,
        MigrationVersionRange.of(1, 2),
        (values, keyResolver) -> {
          throw new IllegalArgumentException("the strategy failed");
        });
  }

  @AfterAll
  static void unregisterStrategies() {
    MigrationStrategies.INSTANCE.unregisterStrategiesFor(Server.class);
    MigrationStrategies.INSTANCE.unregisterStrategiesFor(FailingMigration.class);
    MigrationStrategies.INSTANCE.unregisterStrategiesFor(MovedSection.class);
    MigrationStrategies.INSTANCE.unregisterStrategiesFor(SeesKeyResolver.class);
    MigrationStrategies.INSTANCE.unregisterStrategiesFor(MissingStep.class);
  }

  /** The names of the files in {@code dir}, sorted. */
  private static List<String> filesIn(Path dir) throws IOException {
    try (Stream<Path> files = Files.list(dir)) {
      return files.map(path -> path.getFileName().toString()).sorted().toList();
    }
  }

  /**
   * The comment lines right above the line that writes {@code key}, in a format with comments: the
   * lines before it that start with the format's comment marker.
   */
  private static List<String> commentsAbove(String written, String key) {
    List<String> lines = written.lines().toList();
    int at = -1;
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).strip();
      if (line.startsWith(key) || line.startsWith("\"" + key + "\"")) {
        at = i;
        break;
      }
    }
    Assertions.assertNotEquals(-1, at, () -> "no line for " + key + " in:\n" + written);
    List<String> comments = new ArrayList<>();
    for (int i = at - 1; i >= 0; i--) {
      String line = lines.get(i).strip();
      if (!line.startsWith("#") && !line.startsWith("//")) {
        break;
      }
      comments.addFirst(lines.get(i));
    }
    return comments;
  }

  /** A file name with the format's extension. */
  private String file(String name) {
    return name + "." + this.format.extension();
  }

  // <editor-fold desc="Reading">

  /** Writes the input {@code name} to the file {@code fileName} in {@code dir}. */
  private Path write(Path dir, String fileName, String name) throws IOException {
    return Files.writeString(dir.resolve(file(fileName)), text(name));
  }

  @Test
  void aFileWithoutAVersionIsMigratedFromVersion1() {
    Server config = load(new Server(), "v1");
    Assertions.assertEquals("Hello", config.motd);
    Assertions.assertEquals(25565, config.port);
    Assertions.assertEquals("10s", config.timeout);
  }

  @Test
  void aFileIsMigratedFromItsVersion() {
    Server config = load(new Server(), "v2");
    Assertions.assertEquals(25566, config.port);
    Assertions.assertEquals("20s", config.timeout);
  }

  @Test
  void aFileOfTheCurrentVersionIsReadAsItIs() {
    Server config = load(new Server(), "v3");
    Assertions.assertEquals(25567, config.port);
    Assertions.assertEquals("40s", config.timeout);
  }

  @Test
  void aSubclassIsMigratedWithTheStrategiesOfTheVersionedClass() {
    // the fields are Server's, which a subclass reads only with FIND_PARENT_FIELDS
    Server config =
        load(new Server() {}, "v1", new Settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true));
    Assertions.assertEquals(25565, config.port);
    Assertions.assertEquals("10s", config.timeout);
  }

  @Test
  void valuesLoadedFromAMapAreMigrated() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("port", 1234);
    Server config = new Server();
    this.resolver.load(config, values);
    Assertions.assertEquals(1234, config.port);
    // the map passed in is not changed
    Assertions.assertEquals(Map.of("port", 1234), values);
  }

  @Test
  void aFileOfANewerVersionIsRejected() {
    String message = rejected(new Server(), "v4");
    Assertions.assertTrue(
        message.contains("is of version 4, newer than version 3 of " + Server.class.getName()),
        message);
  }

  @Test
  void anInvalidVersionIsRejected() {
    String message = rejected(new Server(), "invalid-version");
    Assertions.assertTrue(
        message.contains("Invalid version 'two' under the key 'config-version'"), message);
  }

  @Test
  void aMissingStepFailsBeforeAnyFieldIsSet() {
    // only 2 -> 3 is registered, so a file of version 1 can't be migrated
    MissingStep config = new MissingStep();
    IllegalStateException error =
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.resolver.load(config, new StringReader(text("v1"))));
    Assertions.assertTrue(
        error
            .getMessage()
            .contains(
                "no migration of " + MissingStep.class.getName() + " from version 1 is registered"),
        error.getMessage());
    Assertions.assertEquals("A server", config.motd);
  }

  @Test
  void withoutStrategiesAnOlderFileIsReadAsItIs(@TempDir Path dir) throws IOException {
    Path file = write(dir, "config", "v1");
    List<LogRecord> logged = new CopyOnWriteArrayList<>();
    Handler handler =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            logged.add(record);
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        };
    Logger logger = Logger.getLogger("com.mrivanplays.annotationconfig.migration");
    logger.addHandler(handler);
    try {
      WithoutMigrations config = new WithoutMigrations();
      this.resolver.load(config, file);
      Assertions.assertEquals("Hello", config.motd);
    } finally {
      logger.removeHandler(handler);
    }

    // neither copied nor rewritten
    Assertions.assertEquals(text("v1"), Files.readString(file));
    Assertions.assertEquals(List.of(file("config")), filesIn(dir));
    // the formats run in parallel and log too: this one's record names its file
    List<String> warnings =
        logged.stream()
            .filter(record -> record.getLevel() == Level.WARNING)
            .map(record -> new SimpleFormatter().formatMessage(record))
            .filter(message -> message.contains(file.toString()))
            .toList();
    Assertions.assertEquals(1, warnings.size(), () -> "logged: " + warnings);
    Assertions.assertTrue(
        warnings.getFirst().contains("is of version 1, older than version 2 of "),
        warnings.getFirst());
    Assertions.assertTrue(
        warnings.getFirst().contains("no migration of " + WithoutMigrations.class.getName()),
        warnings.getFirst());
  }

  // </editor-fold>

  // <editor-fold desc="Writing">

  @Test
  void aDottedVersionKeyIsRejected() {
    IllegalArgumentException error =
        Assertions.assertThrows(IllegalArgumentException.class, () -> dump(new DottedVersionKey()));
    Assertions.assertTrue(error.getMessage().contains("'meta.version'"), error.getMessage());
  }

  @Test
  void theVersionIsWrittenFirst() {
    String written = assertDump("dump", new Server());
    // read back without a migration, as it is of the current version
    Server read = loadText(new Server(), written);
    Assertions.assertEquals(8080, read.port);
    Assertions.assertEquals("30s", read.timeout);
  }

  @Test
  void aMigratedFileIsRewrittenAfterItIsCopied(@TempDir Path dir) throws IOException {
    Path file = write(dir, "config", "v1");

    Server config = new Server();
    this.resolver.load(config, file);

    Assertions.assertEquals(25565, config.port);
    assertOutput("migrated", Files.readString(file));
    Assertions.assertEquals(text("v1"), Files.readString(dir.resolve(file("config") + ".v1.bak")));

    // the file is current now: loading it again neither changes it nor copies it
    this.resolver.load(new Server(), file);
    assertOutput("migrated", Files.readString(file));
    Assertions.assertEquals(List.of(file("config"), file("config") + ".v1.bak"), filesIn(dir));
  }

  @Test
  void anOlderCopyIsKept(@TempDir Path dir) throws IOException {
    Path file = write(dir, "config", "v2");
    Path older = Files.writeString(dir.resolve(file("config") + ".v2.bak"), "older copy");

    this.resolver.load(new Server(), file);

    Assertions.assertEquals("older copy", Files.readString(older));
    Assertions.assertEquals(
        text("v2"), Files.readString(dir.resolve(file("config") + ".v2.bak.2")));
  }

  @Test
  void loadOrDumpMigratesAnExistingFile(@TempDir Path dir) throws IOException {
    Path file = write(dir, "config", "v1");
    this.resolver.loadOrDump(new Server(), file);
    assertOutput("migrated", Files.readString(file));
  }

  @Test
  void aFailingStepLeavesTheFileAsItWas(@TempDir Path dir) throws IOException {
    Path file = write(dir, "config", "v1");

    IllegalStateException error =
        Assertions.assertThrows(
            IllegalStateException.class, () -> this.resolver.load(new FailingMigration(), file));

    Assertions.assertTrue(error.getMessage().contains("The migration 1 -> 2"), error.getMessage());
    Assertions.assertInstanceOf(IllegalArgumentException.class, error.getCause());
    Assertions.assertEquals(text("v1"), Files.readString(file));
    Assertions.assertEquals(List.of(file("config")), filesIn(dir));
  }

  // </editor-fold>

  // <editor-fold desc="Moving an option between sections">

  @Test
  void aCurrentFileWithoutTheVersionGetsItAsANewOption(@TempDir Path dir) throws IOException {
    Path file = write(dir, "config", "v1");

    FirstVersion config = new FirstVersion();
    this.resolver.load(config, file, generateNewOptions());

    Assertions.assertEquals("Hello", config.motd);
    assertOutput("version-added", Files.readString(file));
    // nothing was migrated, so nothing was copied
    Assertions.assertEquals(List.of(file("config")), filesIn(dir));
  }

  @Test
  void anOptionIsMovedToAnotherSection() {
    // .properties keeps server.port as one key, the others as the key port of the section server
    MovedSection config = load(new MovedSection(), "nested-v1");
    Assertions.assertEquals("example.org", config.host);
    Assertions.assertEquals(25565, config.port);
  }

  @Test
  void aStrategyGetsTheKeyResolverOfTheSettingsPassed() {
    KeyResolver formats =
        this.resolver.settings().get(ACDefaultSettings.KEY_RESOLVER).orElseThrow();
    Assertions.assertEquals("default", load(new SeesKeyResolver(), "v1").seen);
    Settings settings =
        new Settings().put(ACDefaultSettings.KEY_RESOLVER, new MarkedKeyResolver(formats));
    Assertions.assertEquals("passed", load(new SeesKeyResolver(), "v1", settings).seen);
  }

  // </editor-fold>

  // <editor-fold desc="Comments of the version">

  @Test
  void aFileWithAMovedOptionIsRewritten(@TempDir Path dir) throws IOException {
    Path file = write(dir, "config", "nested-v1");

    MovedSection config = new MovedSection();
    this.resolver.load(config, file);

    Assertions.assertEquals(25565, config.port);
    assertOutput("nested-migrated", Files.readString(file));
    Assertions.assertEquals(
        text("nested-v1"), Files.readString(dir.resolve(file("config") + ".v1.bak")));
    // read back as it was written, without a migration
    MovedSection read = new MovedSection();
    this.resolver.load(read, file);
    Assertions.assertEquals("example.org", read.host);
    Assertions.assertEquals(25565, read.port);
  }

  @Test
  void theCommentsOfTheVersionAreWritten() {
    assertDump("commented-version", new CommentedVersion());
  }

  @Test
  void theVersionIsCommentedAsAFieldWithTheSameComments() {
    Assumptions.assumeTrue(this.format.has(Format.Feature.COMMENTS));
    String written = dump(new CommentedVersion());
    List<String> versionComments = commentsAbove(written, "config-version");
    Assertions.assertFalse(versionComments.isEmpty(), written);
    Assertions.assertEquals(commentsAbove(written, "motd"), versionComments, written);
  }

  @Test
  void aCommentedVersionIsReadBack() {
    // a version that wasn't found would be version 1, which has no migration to version 2
    CommentedVersion read = roundTrip(new CommentedVersion(), new CommentedVersion());
    Assertions.assertEquals("A server", read.motd);
  }

  @Test
  void theCommentsOfTheVersionGetThePlaceholders() {
    Assumptions.assumeTrue(this.format.has(Format.Feature.COMMENTS));
    // a placeholder no other test class uses, as the registry is global
    CommentPlaceholder placeholder =
        CommentPlaceholder.simple("%migration-test-version%", "the version");
    CommentPlaceholderRegistry.INSTANCE.registerPlaceholder(placeholder);
    try {
      String written = dump(new PlaceholderVersion());
      Assertions.assertEquals(
          commentsAbove(written, "motd"), commentsAbove(written, "config-version"), written);
      Assertions.assertFalse(written.contains("%migration-test-version%"), written);
    } finally {
      CommentPlaceholderRegistry.INSTANCE.unregisterPlaceholder(placeholder);
    }
  }

  // </editor-fold>

  // <editor-fold desc="subjects">

  @ConfigVersion(3)
  static class Server {

    String motd = "A server";

    @Key("server-port")
    int port = 8080;

    String timeout = "30s";
  }

  @ConfigVersion(2)
  static class SeesKeyResolver {

    String seen = "none";
  }

  /** The key resolver it is made from, told apart by its class. */
  record MarkedKeyResolver(KeyResolver delegate) implements KeyResolver {

    @Override
    public @Nullable Object unbox(String key, Map<String, Object> values) {
      return this.delegate.unbox(key, values);
    }

    @Override
    public void boxTo(String key, Object value, Map<String, Object> values) {
      this.delegate.boxTo(key, value, values);
    }
  }

  @ConfigVersion(2)
  static class MovedSection {

    @Key("server.host")
    String host = "localhost";

    @Key("network.port")
    int port = 8080;
  }

  @ConfigVersion(2)
  static class WithoutMigrations {

    String motd = "A server";
  }

  @ConfigVersion(3)
  static class MissingStep {

    String motd = "A server";
  }

  @ConfigVersion(2)
  static class FailingMigration {

    String motd = "A server";
  }

  @ConfigVersion(1)
  static class FirstVersion {

    String motd = "A server";
  }

  // the class comment is written above the version, the first option
  @Comment("The server's config")
  @ConfigVersion(
      value = 2,
      comments = {"Don't change this", "It is how old the file is,\nused to migrate it"})
  static class CommentedVersion {

    @Comment("Don't change this")
    @Comment("It is how old the file is,\nused to migrate it")
    String motd = "A server";
  }

  @ConfigVersion(value = 1, comments = "Written by %migration-test-version%")
  static class PlaceholderVersion {

    @Comment("Written by %migration-test-version%")
    String motd = "A server";
  }

  @ConfigVersion(value = 1, key = "meta.version")
  static class DottedVersionKey {

    String motd = "A server";
  }

  // </editor-fold>
}
