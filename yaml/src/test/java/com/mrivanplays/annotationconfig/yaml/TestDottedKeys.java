package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDottedKeys {

  private static final class Subject {

    @Comment("Mashallah")
    @Key("dotted.key.test.mashallah")
    private String mashallah = "foo";

  }

  private static final File file = new File("dotted-key-generate.yml");
  private final ConfigResolver resolver = YamlConfig.getConfigResolver();

  @Test
  public void testValueGet() {
    Subject config = new Subject();
    resolver.load(config, getClass().getClassLoader().getResourceAsStream("dotted-keys.yml"));

    Assertions.assertEquals("bar", config.mashallah);
  }

  @Test
  public void testGeneration() {
    Subject config = new Subject();
    resolver.dump(config, file);

    Assertions.assertEquals("foo", config.mashallah);
    file.delete();
  }
}
