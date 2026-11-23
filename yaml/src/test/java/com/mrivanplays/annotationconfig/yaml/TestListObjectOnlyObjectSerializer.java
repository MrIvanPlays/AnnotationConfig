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
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A list of single field objects is written as a list of plain values, both with a registered
 * serializer and with the default serializer.
 */
class TestListObjectOnlyObjectSerializer {

  private final ConfigResolver resolver = YamlConfig.getConfigResolver();

  /** Runs {@code test}, with the serializer registered only for its duration if requested. */
  private static void run(boolean withSerializer, Runnable test) {
    SerializerRegistry registry = SerializerRegistry.INSTANCE;
    if (withSerializer) {
      registry.registerSerializer(TestObject.class, new TestObject.Serializer());
    }
    try {
      test.run();
    } finally {
      if (withSerializer) {
        registry.unregisterSerializer(TestObject.class);
      }
    }
  }

  @ParameterizedTest(name = "with serializer: {0}")
  @ValueSource(booleans = {true, false})
  void testDump(boolean withSerializer) {
    run(
        withSerializer,
        () -> {
          StringWriter writer = new StringWriter();
          resolver.dump(new Subject(), writer);
          Assertions.assertEquals("objects:\n  - \"foo\"\n  - \"bar\"\n", writer.toString());
        });
  }

  @ParameterizedTest(name = "with serializer: {0}")
  @ValueSource(booleans = {true, false})
  void testLoad(boolean withSerializer) {
    run(
        withSerializer,
        () -> {
          Subject config = new Subject();
          resolver.load(config, new StringReader("objects:\n  - \"baz\"\n  - \"aabb\"\n"));
          Assertions.assertEquals(2, config.objects.size());
          Assertions.assertEquals("baz", config.objects.get(0).foo);
          Assertions.assertEquals("aabb", config.objects.get(1).foo);
        });
  }

  static class Subject {

    List<TestObject> objects = Arrays.asList(new TestObject("foo"), new TestObject("bar"));
  }

  static class TestObject {

    String foo;

    public TestObject() {}

    TestObject(String foo) {
      this.foo = foo;
    }

    @NullMarked
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
  }
}
