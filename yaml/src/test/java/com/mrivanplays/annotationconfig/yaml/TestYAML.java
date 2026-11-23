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

package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestYAML {

  private final ConfigResolver resolver = YamlConfig.getConfigResolver();

  @Test
  public void dumpedFileLoadsBack(@TempDir Path dir) {
    Path file = dir.resolve("config.yml");
    resolver.dump(new YAMLTestSubject(), file);

    YAMLTestSubject config = new YAMLTestSubject();
    resolver.load(config, file);
    Assertions.assertEquals("Ivan", config.getName());
    Assertions.assertEquals("No console!", config.getMessages().getNoConsole());
    Assertions.assertEquals(List.of("Hello", "bla bla"), config.getList());
    Assertions.assertEquals(20, config.getLocationTwo().getX());
  }

  @Test
  public void newOptionsAreAddedToAnOlderFile(@TempDir Path dir) throws IOException {
    // YAMLSecondTestSubject is YAMLTestSubject with options added
    Path file = dir.resolve("config.yml");
    resolver.dump(new YAMLTestSubject(), file);

    YAMLSecondTestSubject config = new YAMLSecondTestSubject();
    resolver.load(config, file);

    Assertions.assertEquals(1, config.getA());
    Assertions.assertEquals("world_the_end", config.getLocationThree().getWorld());
    String written = Files.readString(file);
    Assertions.assertTrue(written.contains("# baz\na: 1\n"), written);
    Assertions.assertTrue(written.contains("# Third location\nlocation-no-three:\n"), written);
    Assertions.assertTrue(
        written.contains("# This is some dummy section\nnew-section:\n  foo: \"foo\"\n"), written);
  }

  @Test
  void testLoad() {
    YAMLSecondTestSubject config = new YAMLSecondTestSubject();
    resolver.load(config, getClass().getClassLoader().getResourceAsStream("test.yml"));
    Assertions.assertEquals("foo", config.getLocationThree().getWorld());
    Assertions.assertEquals(1, config.getLocationThree().getX());
    Assertions.assertEquals(223, config.getLocationThree().getY());
    Assertions.assertEquals(445, config.getLocationThree().getZ());
  }
}
