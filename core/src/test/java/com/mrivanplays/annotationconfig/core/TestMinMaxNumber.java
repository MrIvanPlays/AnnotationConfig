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
    PropertyConfig.load(configSubject, file);

    configSubject.setDoubleValue(-34.6);
    file.delete();
    try {
      // yes we have to call it twice - one time to generate the file and one time to invoke field
      // sets
      PropertyConfig.load(configSubject, file);
      PropertyConfig.load(configSubject, file);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(true);
    }
  }

  @Test
  public void testMax() {
    MinMaxTest configSubject = new MinMaxTest();
    PropertyConfig.load(configSubject, file);

    configSubject.setDoubleValue(45.2);
    file.delete();
    try {
      // yes we have to call it twice - one time to generate the file and one time to invoke field
      // sets
      PropertyConfig.load(configSubject, file);
      PropertyConfig.load(configSubject, file);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(true);
    }
  }
}
