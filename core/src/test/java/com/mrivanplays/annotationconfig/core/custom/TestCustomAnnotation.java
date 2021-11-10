package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.PropertyConfig;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCustomAnnotation {

  private static final File file = new File("custom-annotations.properties");
  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @AfterAll
  public static void terminate() {
    if (file.exists()) {
      file.delete();
    }
  }

  @BeforeAll
  public static void registerValidator() {
    CustomAnnotationRegistry.INSTANCE.register(DummyAnnotation.class, new DummyAnnotationHandler());
  }

  @BeforeEach
  public void prepareFile() {
    if (file.exists()) {
      file.delete();
    }
  }

  @Test
  public void testSuccessful() {
    DummyConfig config = new DummyConfig();
    try {
      resolver.dump(config, file);
      resolver.load(config, file);
      Assertions.assertTrue(true);
    } catch (Throwable e) {
      Assertions.fail();
    }
  }

  @Test
  public void testFail() {
    DummyConfig config = new DummyConfig();
    config.setFoo("Foo bar baz");
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          resolver.dump(config, file);
          resolver.load(config, file);
        });
  }

  @Test
  public void testAlterOption() {
    resolver.options().put(OptsConstant.DUMMY_OPTION, OptsConstant.DUMMY_DEFAULT);
    DummyConfig config = new DummyConfig();
    resolver.dump(config, file);
    resolver.load(config, file);
    Assertions.assertTrue(
        resolver.options().getAsOr(OptsConstant.DUMMY_OPTION, Boolean.class, false));
  }
}
