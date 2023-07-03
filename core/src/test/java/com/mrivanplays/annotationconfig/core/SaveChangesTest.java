package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SaveChangesTest {

  static class Subject {

    String foo = "barbaba";
  }

  static File file;
  static ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @BeforeAll
  static void initialize() throws IOException {
    file = File.createTempFile("saveChangesTest", ".properties");
  }

  @AfterAll
  static void terminate() {
    file.delete();
    file = null;
  }

  @Test
  void perform() {
    Subject subject = new Subject();
    System.out.println("Before first dump: " + subject.foo);
    resolver.dump(subject, file);

    subject.foo = "www";
    System.out.println("Before second dump: " + subject.foo);
    resolver.dump(subject, file);

    // change value and force load
    subject.foo = "This should work";
    System.out.println("Before forced load: " + subject.foo);
    resolver.load(subject, file);
    System.out.println("After forced load: " + subject.foo);

    Assertions.assertEquals("www", subject.foo);
    System.out.println("Test completed successfully!");
  }
}
