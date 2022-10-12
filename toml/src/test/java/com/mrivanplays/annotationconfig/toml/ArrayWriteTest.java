package com.mrivanplays.annotationconfig.toml;

import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArrayWriteTest {

  static class Subject {

    private double[] arr = new double[] {1.1, 2.2};
  }

  @Test
  public void testWrite() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    TomlConfig.getConfigResolver().dump(config, writer);

    String expected = "arr = [1.1, 2.2]\n\n";

    Assertions.assertEquals(expected, writer.toString());
  }
}
