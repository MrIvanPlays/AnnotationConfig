package com.mrivanplays.annotationconfig.toml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTOML {

  @Test
  public void testCreatingFile() {
    TOMLTestSubject config = new TOMLTestSubject();
    TomlConfig.getConfigResolver()
        .load(config, getClass().getClassLoader().getResourceAsStream("test-toml.toml"));

    Assertions.assertEquals(25577, config.getServer().getPort());
  }
}
