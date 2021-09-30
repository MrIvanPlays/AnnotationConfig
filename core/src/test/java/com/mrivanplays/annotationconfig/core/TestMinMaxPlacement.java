package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Min;
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMinMaxPlacement {

  private static final class MinMaxTest {

    @Min(minInt = 1)
    private boolean value = false;
  }

  private static final File file = new File("min-max-placement.properties");
  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testMinMaxInvalidPlacement() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          MinMaxTest configSubject = new MinMaxTest();
          resolver.dump(configSubject, file);
          resolver.load(configSubject, file, true);
        });
  }
}
