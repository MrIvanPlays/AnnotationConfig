package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.PropertyConfig;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestCustomAnnotation {

  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @BeforeAll
  public static void registerValidator() {
    CustomAnnotationRegistry.INSTANCE.register(DummyAnnotation.class, new DummyAnnotationHandler());
    CustomAnnotationRegistry.INSTANCE.register(
        DummyAnnotation2.class, new DummyAnnotation2Handler());
  }

  @Test
  public void testSuccessful() {
    DummyConfig config = new DummyConfig();
    resolver.load(
        config,
        getClass().getClassLoader().getResourceAsStream("custom/custom-anno-def.properties"));
  }

  @Test
  public void testFail() {
    DummyConfig config = new DummyConfig();
    Assertions.assertThrows(
        RuntimeException.class,
        () ->
            resolver.load(
                config,
                getClass()
                    .getClassLoader()
                    .getResourceAsStream("custom/custom-anno-fail.properties")));
  }

  @Test
  public void testAlterOption() {
    resolver.options().put(OptsConstant.DUMMY_OPTION, OptsConstant.DUMMY_DEFAULT);
    DummyConfig config = new DummyConfig();
    resolver.load(
        config,
        getClass().getClassLoader().getResourceAsStream("custom/custom-anno-def.properties"));
    Assertions.assertTrue(
        resolver.options().getAsOr(OptsConstant.DUMMY_OPTION, Boolean.class, false));
  }

  @Test
  public void testDump() {
    DummyConfig config = new DummyConfig();
    StringWriter writer = new StringWriter();
    resolver.dump(config, writer);

    String written = writer.toString();
    // now we have to load because we need to verify that bar=Lorem ipsum dolor sit amet.
    resolver.load(config, new StringReader(written));

    Assertions.assertEquals("Lorem ipsum dolor sit amet.", config.getBar());
  }
}
