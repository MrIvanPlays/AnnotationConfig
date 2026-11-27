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

package com.mrivanplays.annotationconfig.scenarios.validation;

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.OneOf;
import com.mrivanplays.annotationconfig.core.resolver.ConfigLoadException;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code REPORT_ALL_ERRORS} reads every option before it reports the ones that failed. Options in
 * sections are in {@link TestReportAllErrorsInSections}.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestReportAllErrors extends Scenario {

  TestReportAllErrors(Format format) {
    super(format, "validation");
  }

  static Settings reportAll() {
    return new Settings().put(ACDefaultSettings.REPORT_ALL_ERRORS, true);
  }

  static List<String> keys(ConfigLoadException e) {
    return e.problems().stream().map(ConfigLoadException.Problem::key).toList();
  }

  // <editor-fold desc="Reporting">

  @Test
  void everyOptionThatFailsIsReportedInTheOrderTheyAreRead() {
    ConfigLoadException e =
        Assertions.assertThrows(
            ConfigLoadException.class, () -> load(new Server(), "errors-three", reportAll()));

    // .properties reads the fields in reverse declaration order, as it writes them
    List<String> expected =
        this.format == Format.PROPERTIES
            ? List.of("level", "mode", "port")
            : List.of("port", "mode", "level");
    Assertions.assertEquals(expected, keys(e));
    Assertions.assertEquals(3, e.getSuppressed().length);
    String message = e.getMessage();
    Assertions.assertTrue(
        message.startsWith("3 options of the configuration could not be read:\n- '"), message);
    for (String part : List.of("above the maximum", "'hardcore'", "'impossible'")) {
      Assertions.assertTrue(message.contains(part), message);
    }
  }

  @Test
  void theOptionsThatCanBeReadAreRead() {
    Server config = new Server();
    Assertions.assertThrows(
        ConfigLoadException.class, () -> load(config, "errors-three", reportAll()));

    Assertions.assertEquals(20, config.players);
    Assertions.assertEquals("welcome", config.motd);
    // the options that failed keep their values
    Assertions.assertEquals(25565, config.port);
    Assertions.assertEquals("survival", config.mode);
    Assertions.assertEquals(Level.EASY, config.level);
  }

  @Test
  void oneFailureIsReportedTheSameWay() {
    ConfigLoadException e =
        Assertions.assertThrows(
            ConfigLoadException.class, () -> load(new Server(), "errors-port", reportAll()));

    Assertions.assertEquals(List.of("port"), keys(e));
    Assertions.assertTrue(
        e.getMessage().startsWith("1 option of the configuration could not be read:"),
        e.getMessage());
  }

  @Test
  void withoutTheSettingLoadingStopsAtTheFirstFailure() {
    IllegalArgumentException e =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> load(new Server(), "errors-three"));

    Assertions.assertFalse(e instanceof ConfigLoadException, e.toString());
  }

  @Test
  void aConfigWithoutFailuresLoads() {
    Server config = load(new Server(), "errors-none", reportAll());

    Assertions.assertEquals(8080, config.port);
    Assertions.assertEquals("creative", config.mode);
    Assertions.assertEquals(Level.HARD, config.level);
  }

  @Test
  void aFileWithFailuresIsNotWrittenAgain(@TempDir Path dir) throws IOException {
    // the file misses options, which GENERATE_NEW_OPTIONS would add
    Path file = dir.resolve("config." + this.format.extension());
    Files.writeString(file, text("errors-port"));

    Settings settings = reportAll().put(ACDefaultSettings.GENERATE_NEW_OPTIONS, true);
    ConfigLoadException e =
        Assertions.assertThrows(
            ConfigLoadException.class, () -> this.resolver.load(new Server(), file, settings));

    Assertions.assertEquals(text("errors-port"), Files.readString(file));
    Assertions.assertTrue(
        e.getMessage().startsWith("1 option of the configuration " + file + " could not"),
        e.getMessage());
  }

  // </editor-fold>

  // <editor-fold desc="Reporting: subjects">

  enum Level {
    EASY,
    HARD
  }

  static class Server {

    @Min(minInt = 1)
    @Max(maxInt = 65535)
    int port = 25565;

    @OneOf({"survival", "creative"})
    String mode = "survival";

    Level level = Level.EASY;

    int players = 10;

    String motd = "hi";
  }

  // </editor-fold>
}
