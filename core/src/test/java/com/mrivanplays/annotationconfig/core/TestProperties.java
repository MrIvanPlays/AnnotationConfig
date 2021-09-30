package com.mrivanplays.annotationconfig.core;

import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestProperties {

  private static final File file = new File("non-existing.properties");
  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testCreatingFile() {
    PropertiesTestSubject config = new PropertiesTestSubject();
    try {
      resolver.dump(config, file);
      resolver.load(config, file, true);
      Assertions.assertTrue(true);
    } catch (Throwable e) {
      e.printStackTrace();
      Assertions.fail();
    }
  }
}
