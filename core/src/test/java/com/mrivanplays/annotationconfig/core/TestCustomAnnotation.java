package com.mrivanplays.annotationconfig.core;

import java.io.File;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.function.Supplier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestCustomAnnotation {

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface CustomWrite {}

  public static class CustomWriteResolver
      implements CustomAnnotationRegistry.AnnotationResolver<CustomWrite> {

    @Override
    public void write(
        CustomAnnotationRegistry.AnnotationWriter writer,
        CustomWrite annotation,
        CustomAnnotationRegistry.AnnotationResolverContext context) {
      if (context.getConfigType().isAssignableFrom(PropertyConfig.class)) {
        writer.write(context.getKeyName() + "=" + context.getDefaultsToValue());
      }
    }

    @Override
    public Supplier<FieldTypeResolver> typeResolver() {
      return IntegerFieldResolver::new;
    }

    public static class IntegerFieldResolver implements FieldTypeResolver {

      @Override
      public Object toType(Object value, Field field) throws Exception {
        return Integer.parseInt(String.valueOf(value));
      }

      @Override
      public boolean shouldResolve(Class<?> fieldType) {
        return Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType);
      }
    }
  }

  @Comment("Say hello")
  public static class TestSubjectWithCustomAnno {

    @Comment("Table number")
    @Key("table.number")
    @CustomWrite
    private int tableNumber = 1;

    public int getTableNumber() {
      return tableNumber;
    }
  }

  private File file;

  @Before
  public void initialize() {
    file = new File("custom-anno.properties");
    PropertyConfig.getAnnotationRegistry().register(CustomWrite.class, new CustomWriteResolver());
  }

  @After
  public void terminate() {
    file.delete();
  }

  @Test
  public void perform() {
    TestSubjectWithCustomAnno config = new TestSubjectWithCustomAnno();
    PropertyConfig.load(config, file);

    Assert.assertEquals(1, config.getTableNumber());
  }
}
