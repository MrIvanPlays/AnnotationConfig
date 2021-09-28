package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.serialization.registry.SerializerRegistry;
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
    if (!SerializerRegistry.INSTANCE.hasSerializer(LocationNoConfigObject.class)) {
      SerializerRegistry.INSTANCE.registerSerializer(
          LocationNoConfigObject.class, new LocationNoConfigObjectSerializer());
    }
  }

  @After
  public void terminate() {
    file.delete();
  }

  @Test
  public void testCreatingFile() {
    YAMLTestSubject config = new YAMLTestSubject();
    YamlConfig.getConfigResolver().loadOrDump(config, file, true);

    Assert.assertEquals("Ivan", config.getName());
    Assert.assertEquals("bar", config.getFoo());
    Assert.assertEquals("No console!", config.getMessages().getNoConsole());
    Assert.assertEquals("This command is console only!", config.getMessages().getConsoleOnly());
    Assert.assertEquals("baz", config.getBar());
    Assert.assertTrue(config.getList().contains("Hello"));
    Assert.assertEquals(20, config.getLocationTwo().getX());
  }

  @Test
  public void testNonExistingField() {
    YAMLSecondTestSubject config = new YAMLSecondTestSubject();
    YamlConfig.getConfigResolver().loadOrDump(config, file, true);

    Assert.assertEquals(1, config.getA());
  }
}
