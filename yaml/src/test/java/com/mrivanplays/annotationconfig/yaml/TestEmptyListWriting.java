package com.mrivanplays.annotationconfig.yaml;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestEmptyListWriting {

  static class Subject {

    private List<String> foo = Collections.emptyList();
  }

  @Test
  public void testEmptyListDump() {
    Subject subject = new Subject();
    StringWriter writer = new StringWriter();

    YamlConfig.getConfigResolver().dump(subject, writer);

    Assertions.assertEquals("foo: []\n\n", writer.toString());
  }
}
