package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestIgnoreAnno {

  @Comment("This is on the top")
  static class IgnoreAnnoTestSubject {

    @Ignore private boolean b = false;

    @Comment("Hello my name is Ivan")
    private String name = "Ivan";
  }

  private static final File file = new File("ignore-anno.properties");

  @AfterAll
  public static void terminate() {
    file.delete();
  }

  @Test
  public void testIgnoreWorks() {
    IgnoreAnnoTestSubject config = new IgnoreAnnoTestSubject();
    PropertyConfig.getConfigResolver().dump(config, file);

    // now to make sure this works we have to read the file
    Properties properties = new Properties();
    try (Reader reader = new FileReader(file)) {
      properties.load(reader);
    } catch (IOException e) {
      Assertions.fail(e);
      return;
    }
    Assertions.assertFalse(properties.containsKey("b"));
    Assertions.assertTrue(properties.containsKey("name"));
  }
}
