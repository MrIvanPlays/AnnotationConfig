package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.resolver.WritableObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ResolveMultipleTest {

  public static class Subject {

    int foo = 2000;

    Subject() {}

    Subject(int foo) {
      this.foo = foo;
    }
  }

  private static Path dir;

  @BeforeAll
  static void loadDifferentFiles() {
    try {
      dir = Files.createTempDirectory("resolveMultipleTest");
      for (int i = 0; i < 3; i++) {
        Path file = Files.createTempFile(dir, "test" + i, ".properties");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file))) {
          writer.println("foo=" + (1000 + i));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testResolveMultiple() {
    Map<String, Subject> loadedConfigs =
        PropertyConfig.getConfigResolver()
            .resolveMultiple(
                dir,
                Subject::new,
                WritableObject.createFromPath(dir.resolve("default.properties")));

    int firstFoo =
        loadedConfigs.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("test0"))
            .findFirst()
            .orElse(new AbstractMap.SimpleEntry<>("foo", new Subject(69420)))
            .getValue()
            .foo;
    Assertions.assertEquals(1000, firstFoo);
  }

  @AfterAll
  static void terminate() {
    try (Stream<Path> stream = Files.list(dir)) {
      Iterator<Path> iterator = stream.iterator();
      while (iterator.hasNext()) {
        Files.delete(iterator.next());
      }
      Files.delete(dir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
