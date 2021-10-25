package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestYAML {

  private static final File file = new File("non-existing.yml");
  private final ConfigResolver resolver = YamlConfig.getConfigResolver();

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testCreatingFile() {
    YAMLTestSubject config = new YAMLTestSubject();
    try {
      resolver.dump(config, file);
      resolver.load(config, file, true);
      Assertions.assertTrue(true);
    } catch (Throwable e) {
      e.printStackTrace();
      Assertions.fail();
    }
  }

  @Test
  public void testNonExistingField() {
    YAMLSecondTestSubject config = new YAMLSecondTestSubject();
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
