package com.mrivanplays.annotationconfig.toml;

import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestTOML {

  private File file;

  @Before
  public void initialize() {
    file = new File("non-existing.toml");
  }

  @After
  public void terminate() {
  }

  @Test
  public void testCreatingFile() {
    TOMLTestSubject config = new TOMLTestSubject();
    TomlConfig.load(config, file);

    Assert.assertEquals("Ivan", config.getName());
    Assert.assertFalse(config.isBar());
    Assert.assertEquals("localhost", config.getServer().getHipi());
    Assert.assertEquals(25565, config.getServer().getPort());
  }
}
