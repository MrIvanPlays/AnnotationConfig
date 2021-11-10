package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.PropertiesTestSubject.MessageType;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestProperties {

  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @Test
  public void testCreatingFile() {
    PropertiesTestSubject config = new PropertiesTestSubject();
    resolver.load(config, getClass().getClassLoader().getResourceAsStream("test-prop.properties"));
    Assertions.assertEquals(MessageType.COMPONENT, config.getMessageType());
    Assertions.assertEquals("foo bar baz", config.getMessage());
  }
}
