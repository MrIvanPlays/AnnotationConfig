/*
 * Copyright (c) Ivan Pekov 2026
 * Copyright (c) Contributors 2026
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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

  static class Subject {

    List<TestObject> objects = Arrays.asList(new TestObject("foo"), new TestObject("bar"));
  }

  static class TestObject {

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

    public TestObject() {}

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

    Assertions.assertEquals(expected, writer.toString().replaceAll("\\R", "\n"));
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
