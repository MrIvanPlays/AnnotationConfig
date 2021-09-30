package com.mrivanplays.annotationconfig.toml;

import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTOML {

  private static final File file = new File("non-existing.toml");

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testCreatingFile() {
    TOMLTestSubject config = new TOMLTestSubject();
    try {
      // once to generate, 2nd time to load
      TomlConfig.load(config, file);
      TomlConfig.load(config, file);
      Assertions.assertTrue(true);
    } catch (Throwable e) {
      e.printStackTrace();
      Assertions.fail();
    }
  }
}
