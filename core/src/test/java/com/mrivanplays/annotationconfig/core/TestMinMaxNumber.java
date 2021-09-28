package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

  private File file;

  @Before
  public void initialize() {
    file = new File("min-max-number.properties");
  }

  @After
  public void terminate() {
    file.delete();
  }

  @Test
  public void testMin() {
    MinMaxTest configSubject = new MinMaxTest();
    PropertyConfig.getConfigResolver().loadOrDump(configSubject, file, true);

    configSubject.setDoubleValue(-34.6);
    file.delete();
    try {
      PropertyConfig.getConfigResolver().dump(configSubject, file);
      PropertyConfig.getConfigResolver().load(configSubject, file, true);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testMax() {
    MinMaxTest configSubject = new MinMaxTest();
    PropertyConfig.getConfigResolver().loadOrDump(configSubject, file, true);

    configSubject.setDoubleValue(45.2);
    file.delete();
    try {
      PropertyConfig.getConfigResolver().dump(configSubject, file);
      PropertyConfig.getConfigResolver().load(configSubject, file, true);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(true);
    }
  }
}
