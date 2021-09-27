package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

  private File file;

  @Before
  public void initialize() {
    file = new File("min-max-string.properties");
  }

  @After
  public void terminate() {
    file.delete();
  }

  @Test
  public void testMin() {
    MinMaxTest configSubject = new MinMaxTest();
    PropertyConfig.load(configSubject, file);

    configSubject.setValue("a");
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

    configSubject.setValue("This exceeds the max limit set");
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
