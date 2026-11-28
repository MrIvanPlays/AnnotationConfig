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

import com.mrivanplays.annotationconfig.core.serialization.AdvancedEnumSerializer;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.ContextFreeFieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.serialization.helpers.DurationSerializer;
import com.mrivanplays.annotationconfig.core.serialization.helpers.UuidToStringSerializer;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestContextFreeFieldTypeSerializer {

  @Test
  void deserializeAndSerializeCallTheSimpleMethods() {
    FieldTypeSerializer<Point> serializer = new PointSerializer();
    // the context is not used, so null works
    Point point = serializer.deserialize(new DataObject("1:2"), null, AnnotationAccessor.EMPTY);
    Assertions.assertEquals(new Point(1, 2), point);
    Assertions.assertEquals(
        "1:2", serializer.serialize(point, null, AnnotationAccessor.EMPTY).getAsString());
  }

  @Test
  void contextFreeSerializersSayTheyAre() {
    Assertions.assertTrue(new PointSerializer().isContextFree());
    Assertions.assertTrue(new UuidToStringSerializer().isContextFree());
    Assertions.assertTrue(new DurationSerializer(ChronoUnit.SECONDS).isContextFree());
    Assertions.assertTrue(AdvancedEnumSerializer.forEnum(Mode.class).isContextFree());
  }

  @Test
  void otherSerializersAreNotContextFree() {
    FieldTypeSerializer<Point> functional =
        FieldTypeSerializer.functional(
            (data, _, _) -> Point.parse(data.getAsString()),
            (value, _, _) -> new DataObject(value.x() + ":" + value.y()));
    Assertions.assertFalse(functional.isContextFree());
  }

  @Test
  void registerSimpleSerializerRegistersAContextFreeSerializer() {
    SerializerRegistry.INSTANCE.registerSimpleSerializer(
        Point.class,
        data -> Point.parse(data.getAsString()),
        value -> new DataObject(value.x() + ":" + value.y()));
    try {
      FieldTypeSerializer<?> registered =
          SerializerRegistry.INSTANCE.getSerializer(Point.class).orElseThrow();
      Assertions.assertTrue(registered.isContextFree());
    } finally {
      SerializerRegistry.INSTANCE.unregisterSerializer(Point.class);
    }
  }

  // <editor-fold desc="Subjects">

  private enum Mode {
    ON,
    OFF
  }

  private record Point(int x, int y) {

    static Point parse(String text) {
      String[] parts = text.split(":", -1);
      return new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }
  }

  private static final class PointSerializer implements ContextFreeFieldTypeSerializer<Point> {

    @Override
    public @Nullable Point deserializeSimple(DataObject data) {
      return Point.parse(data.getAsString());
    }

    @Override
    public DataObject serializeSimple(Point value) {
      return new DataObject(value.x() + ":" + value.y());
    }
  }

  // </editor-fold>
}
