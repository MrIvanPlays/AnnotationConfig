package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.PropertiesTestSubject.MessageType;
import com.mrivanplays.annotationconfig.core.PropertiesTestSubject.MessageTypeResolver;
import com.mrivanplays.annotationconfig.core.serialization.registry.SerializerRegistry;
import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestProperties {

  private File file;

  @Before
  public void initialize() {
    file = new File("non-existing.properties");
    SerializerRegistry.INSTANCE.registerSerializer(MessageType.class, new MessageTypeResolver());
  }

  @After
  public void terminate() {
    file.delete();
  }

  @Test
  public void testCreatingFile() {
    PropertiesTestSubject config = new PropertiesTestSubject();
    PropertyConfig.load(config, file);

    Assert.assertEquals("this is a message", config.getMessage());
    Assert.assertSame(config.getMessageType(), MessageType.STRING);
  }
}
