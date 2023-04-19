package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.RawConfig;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRawConfig {

  static class Subject {

    private String foo = "asd";
    private double baz = 2.3;

    @RawConfig private DataObject rawConfig;

    public DataObject getRawConfig() {
      return rawConfig;
    }
  }

  @Test
  public void testRawConfig() {
    Subject config = new Subject();
    PropertyConfig.getConfigResolver()
        .load(config, getClass().getClassLoader().getResourceAsStream("raw-config.properties"));

    Assertions.assertTrue(config.getRawConfig().has("foo"));
    Assertions.assertTrue(config.getRawConfig().has("baz"));
    Assertions.assertEquals("bar", config.foo);
    Assertions.assertEquals(2.5, config.baz);
  }
}
