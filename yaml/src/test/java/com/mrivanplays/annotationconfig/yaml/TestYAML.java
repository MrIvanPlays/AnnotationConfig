package com.mrivanplays.annotationconfig.yaml;

import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestYAML {

  private File file;

  @Before
  public void initialize() {
    file = new File("non-existing.yml");
  }

  @After
  public void terminate() {
    file.delete();
  }

  @Test
  public void testCreatingFile() {
    YAMLTestSubject config = new YAMLTestSubject();
    YamlConfig.load(config, file);

    Assert.assertEquals("Ivan", config.getName());
    Assert.assertEquals("bar", config.getFoo());
    Assert.assertEquals("No console!", config.getMessages().getNoConsole());
    Assert.assertEquals("This command is console only!", config.getMessages().getConsoleOnly());
    Assert.assertEquals("baz", config.getBar());
  }

  @Test
  public void testNonExistingField() {
    YAMLSecondTestSubject config = new YAMLSecondTestSubject();
    YamlConfig.load(config, file);

    Assert.assertEquals(1, config.getA());
  }
}
