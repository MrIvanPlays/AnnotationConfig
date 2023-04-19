package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ExtendedConfigurationTest {

  @Comment("Test comment foo")
  static class Subject {

    int foo = 1;

  }

  static class ExtendedSubject extends Subject {
    String bar = "baz";
  }

  @BeforeAll
  static void updateSettings() {
    PropertyConfig.getConfigResolver().settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true);
  }

  @Test
  void testDump() {
    ExtendedSubject subject = new ExtendedSubject();
    StringWriter writer = new StringWriter();
    PropertyConfig.getConfigResolver().dump(subject, writer);

    String expected = "# Test comment foo\n\nfoo=1\n\nbar=baz\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  void testParse() {
    ExtendedSubject subject = new ExtendedSubject();
    StringReader reader = new StringReader("foo=2\n\nbar=aaa\n");
    PropertyConfig.getConfigResolver().load(subject, reader);

    Assertions.assertEquals(2, subject.foo);
    Assertions.assertEquals("aaa", subject.bar);
  }
}
