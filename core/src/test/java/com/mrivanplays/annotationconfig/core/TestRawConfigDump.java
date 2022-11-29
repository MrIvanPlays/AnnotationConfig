package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.RawConfig;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRawConfigDump {

  public static class Subject {

    private String foo = "aabb";
    private int bar = 1;

    @RawConfig public DataObject raw;
  }

  @Test
  public void testRawConfigDump() {
    Subject config = new Subject();
    PropertyConfig.getConfigResolver().dump(config, new StringWriter());

    Assertions.assertNotNull(config.raw);
    Assertions.assertEquals("aabb", config.raw.get("foo").getAsString());
    Assertions.assertEquals(1, config.raw.get("bar").getAsInt());
  }
}
