package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestListObjectOnlyObjectSerializer {

  public static class Subject {

    List<TestObject> objects = Arrays.asList(new TestObject("foo"), new TestObject("bar"));
  }

  public static class TestObject {

    static class Serializer implements FieldTypeSerializer<TestObject> {

      @Override
      public TestObject deserialize(
          DataObject data,
          SerializationContext<TestObject> context,
          AnnotationAccessor annotations) {
        return new TestObject(data.getAsString());
      }

      @Override
      public DataObject serialize(
          TestObject value,
          SerializationContext<TestObject> context,
          AnnotationAccessor annotations) {
        return new DataObject(value.foo);
      }
    }

    String foo;

    TestObject(String foo) {
      this.foo = foo;
    }
  }

  private final ConfigResolver resolver = YamlConfig.getConfigResolver();

  void registerSerializer() {
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    if (!serializerRegistry.hasSerializer(TestObject.class)) {
      serializerRegistry.registerSerializer(TestObject.class, new TestObject.Serializer());
    }
  }

  void unregisterSerializer() {
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    if (serializerRegistry.hasSerializer(TestObject.class)) {
      serializerRegistry.unregisterSerializer(TestObject.class);
    }
  }

  @Test
  void testDumpWithSerializer() {
    this.registerSerializer();
    this.testDump();
  }

  @Test
  void testLoadWithSerializer() {
    this.registerSerializer();
    this.testLoad();
  }

  @Test
  void testDumpWithoutSerializer() {
    this.unregisterSerializer();
    this.testDump();
  }

  @Test
  void testLoadWithoutSerializer() {
    this.unregisterSerializer();
    this.testLoad();
  }

  void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    resolver.dump(config, writer);

    String expected = "objects:\n  - \"foo\"\n  - \"bar\"\n\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  void testLoad() {
    Subject config = new Subject();
    StringReader reader = new StringReader("objects:\n  - \"baz\"\n  - \"aabb\"\n\n");
    resolver.load(config, reader);

    Assertions.assertEquals(2, config.objects.size());
    Assertions.assertEquals("baz", config.objects.get(0).foo);
    Assertions.assertEquals("aabb", config.objects.get(1).foo);
  }
}
