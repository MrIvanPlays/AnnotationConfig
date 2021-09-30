package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMinMaxString {

  private static final class MinMaxTest {

    @Min(minInt = 3)
    @Max(maxInt = 7)
    private String value = "Hello";

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  private static final File file = new File("min-max-string.properties");
  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testMin() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          MinMaxTest configSubject = new MinMaxTest();
          resolver.dump(configSubject, file);

          configSubject.setValue("a");
          file.delete();
          resolver.dump(configSubject, file);
          resolver.load(configSubject, file, true);
        });
  }

  @Test
  public void testMax() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> {
          MinMaxTest configSubject = new MinMaxTest();
          resolver.dump(configSubject, file);

          configSubject.setValue("This exceeds the max limit set");
          file.delete();
          resolver.dump(configSubject, file);
          resolver.load(configSubject, file, true);
        });
  }
}
