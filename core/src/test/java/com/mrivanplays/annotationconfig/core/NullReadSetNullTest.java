package com.mrivanplays.annotationconfig.core;

import java.io.StringReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NullReadSetNullTest {

  static class Subject {

    Integer foo = 1;

    String bar = "aa";

  }

  static StringReader reader;

  @BeforeAll
  static void initialize() {
    reader = new StringReader("foo=\n\nbar=");
  }

  @AfterAll
  static void terminate() {
    reader = null;
  }

  @Test
  void testNullRead() {
    Subject subject = new Subject();
    PropertyConfig.getConfigResolver().load(subject, reader);

    Assertions.assertNull(subject.foo);
    Assertions.assertNull(subject.bar);
  }

}
