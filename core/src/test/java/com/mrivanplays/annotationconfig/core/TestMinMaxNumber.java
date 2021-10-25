package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMinMaxNumber {

  private static final class MinMaxTest {

    @Min(minDouble = -33.9)
    @Max(maxDouble = 44.4)
    private double doubleValue = 33.5;

    public double getDoubleValue() {
      return doubleValue;
    }

    public void setDoubleValue(double newVal) {
      this.doubleValue = newVal;
    }
  }

  private static final File file = new File("min-max-number.properties");
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

          configSubject.setDoubleValue(-34.6);
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

          configSubject.setDoubleValue(45.2);
          file.delete();
          resolver.dump(configSubject, file);
          resolver.load(configSubject, file, true);
        });
  }
}
