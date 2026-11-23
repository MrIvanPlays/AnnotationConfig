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

package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestSerializerRegistry {

  private static final SerializerRegistry REGISTRY = SerializerRegistry.INSTANCE;
  private static final FieldTypeSerializer<Point> SERIALIZER =
      FieldTypeSerializer.functional(
          (data, _, _) -> Point.parse(data.getAsString()),
          (value, _, _) -> new DataObject(value.x + ":" + value.y));

  private static String dump() {
    StringWriter writer = new StringWriter();
    PropertyConfig.getConfigResolver().dump(new Subject(), writer);
    return writer.toString();
  }

  private static Point load(String properties) {
    Subject subject = new Subject();
    PropertyConfig.getConfigResolver().load(subject, new StringReader(properties));
    return subject.point;
  }

  // the registry is global, so no test may leave a serializer for Point behind
  @AfterEach
  void unregister() {
    if (REGISTRY.hasSerializer(Point.class)) {
      REGISTRY.unregisterSerializer(Point.class);
    }
  }

  @Test
  void classAndTypeOverloadsShareTheirEntries() {
    REGISTRY.registerSerializer(Point.class, SERIALIZER);

    Assertions.assertTrue(REGISTRY.hasSerializer((Type) Point.class));
    Assertions.assertSame(SERIALIZER, REGISTRY.getSerializer((Type) Point.class).orElseThrow());
    Assertions.assertSame(SERIALIZER, REGISTRY.getSerializer(Point.class).orElseThrow());

    REGISTRY.unregisterSerializer((Type) Point.class);
    Assertions.assertFalse(REGISTRY.hasSerializer(Point.class));
    Assertions.assertTrue(REGISTRY.getSerializer(Point.class).isEmpty());
  }

  @Test
  void registeredSerializerIsUsed() {
    REGISTRY.registerSerializer(Point.class, SERIALIZER);
    Assertions.assertEquals("point=1:2\n", dump());
    Point point = load("point=3:4");
    Assertions.assertEquals(3, point.x);
    Assertions.assertEquals(4, point.y);
  }

  @Test
  void registeringTwiceIsRejected() {
    REGISTRY.registerSerializer(Point.class, SERIALIZER);

    IllegalArgumentException e =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> REGISTRY.registerSerializer((Type) Point.class, SERIALIZER));
    Assertions.assertTrue(e.getMessage().contains("already registered"), e.getMessage());
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () ->
            REGISTRY.registerSimpleSerializer(
                Point.class, data -> Point.parse(data.getAsString()), _ -> new DataObject("")));
    // the first registration is kept
    Assertions.assertSame(SERIALIZER, REGISTRY.getSerializer(Point.class).orElseThrow());
  }

  @Test
  void unregisteringWithoutSerializerIsRejected() {
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> REGISTRY.unregisterSerializer(Point.class));
  }

  @Test
  void simpleValueSerializerAndDeserializerAreCombined() {
    REGISTRY.registerSimpleValueSerializer(
        Point.class, value -> new DataObject(value.x + ":" + value.y));
    REGISTRY.registerSimpleValueDeserializer(Point.class, data -> Point.parse(data.getAsString()));

    Assertions.assertEquals("point=1:2\n", dump());
    Assertions.assertEquals(5, load("point=5:6").x);
  }

  @Test
  void simpleValueDeserializerAndSerializerAreCombinedInEitherOrder() {
    REGISTRY.registerSimpleValueDeserializer(Point.class, data -> Point.parse(data.getAsString()));
    REGISTRY.registerSimpleValueSerializer(
        Point.class, value -> new DataObject(value.x + ":" + value.y));

    Assertions.assertEquals("point=1:2\n", dump());
    Assertions.assertEquals(6, load("point=5:6").y);
  }

  @Test
  void concurrentRegistrationsOfOneTypeLetExactlyOneWin() throws Exception {
    int threads = 16;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger rejected = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (int i = 0; i < threads; i++) {
        futures.add(
            executor.submit(() -> {
              start.await();
              try {
                REGISTRY.registerSerializer(Point.class, SERIALIZER);
              } catch (IllegalArgumentException e) {
                rejected.incrementAndGet();
              }
              return null;
            }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get();
      }
    } finally {
      executor.shutdownNow();
    }
    Assertions.assertEquals(threads - 1, rejected.get());
    Assertions.assertTrue(REGISTRY.hasSerializer(Point.class));
  }

  /** A type only this test registers serializers for. */
  static final class Point {

    final int x;
    final int y;

    Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    static Point parse(String text) {
      String[] parts = text.split(":");
      return new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
  }

  static class Subject {

    Point point = new Point(1, 2);
  }
}
