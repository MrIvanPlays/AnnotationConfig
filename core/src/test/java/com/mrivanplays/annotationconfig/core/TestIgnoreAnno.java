package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestIgnoreAnno {

  @Comment("This is on the top")
  static class RetrieveAnnoTestSubject {

    @Ignore private boolean b = false;

    @Comment("Hello my name is Ivan")
    private String name = "Ivan";
  }

  private File file;

  @Before
  public void initialize() {
    file = new File("retrieve-anno.properties");
  }

  @After
  public void terminate() {
    file.delete();
  }

  @Test
  public void testRetrieveWorks() {
    RetrieveAnnoTestSubject config = new RetrieveAnnoTestSubject();
    PropertyConfig.load(config, file);

    // now to make sure this works we have to read the file
    Properties properties = new Properties();
    try (Reader reader = new FileReader(file)) {
      properties.load(reader);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
      return;
    }
    Assert.assertFalse(properties.containsKey("b"));
    Assert.assertTrue(properties.containsKey("name"));
  }
}
