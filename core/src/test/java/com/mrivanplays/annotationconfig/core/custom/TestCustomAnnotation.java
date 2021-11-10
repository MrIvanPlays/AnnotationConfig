package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.PropertyConfig;
import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestCustomAnnotation {

  @Documented
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface HelloMessage {}

  static class HelloMessageValidator implements AnnotationValidator<HelloMessage> {

    @Override
    public boolean validate(
        HelloMessage annotation, Object value, CustomOptions options, Field field) {
      if (!(value instanceof String)) {
        return false;
      }
      String stringValue = (String) value;
      return stringValue.contains("Hello");
    }

    @Override
    public Throwable error() {
      return new IllegalArgumentException("Message does not contain \"Hello\" :(");
    }
  }

  static class CustomConfig {

    @HelloMessage private String foo = "Hello mama";

    public String getFoo() {
      return foo;
    }

    public void setFoo(String newFoo) {
      this.foo = newFoo;
    }
  }

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
    CustomAnnotationRegistry.INSTANCE.register(HelloMessage.class, new HelloMessageValidator());
  }

  @BeforeEach
  public void prepareFile() {
    if (file.exists()) {
      file.delete();
    }
  }

  @Test
  public void testSuccessful() {
    CustomConfig config = new CustomConfig();
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
    CustomConfig config = new CustomConfig();
    config.setFoo("Foo bar baz");
    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          resolver.dump(config, file);
          resolver.load(config, file);
        });
  }
}
