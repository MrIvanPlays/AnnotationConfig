package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Min;
import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMinMaxPlacement {

  private static final class MinMaxTest {

    @Min(minInt = 1)
    private boolean value = false;

  }

  private File file;

  @Before
  public void initialize() {
    file = new File("min-max-placement.properties");
  }

  @After
  public void terminate() {
    file.delete();
  }

  @Test
  public void testMinMaxInvalidPlacement() {
    MinMaxTest configSubject = new MinMaxTest();
    PropertyConfig.getConfigResolver().dump(configSubject, file);

    try {
      PropertyConfig.getConfigResolver().load(configSubject, file, true);
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(true);
    }
  }
}
