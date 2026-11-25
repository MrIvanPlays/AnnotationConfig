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

package com.mrivanplays.annotationconfig.toml;

import static com.mrivanplays.annotationconfig.toml.TomlTesting.RESOLVER;

import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** {@code @Polymorphic} values in TOML. */
class TestTomlPolymorphic {

  private static Subject changed() {
    Subject subject = new Subject();
    subject.task = new Repeat(3);
    subject.shapes = List.of(new Square(4), new Circle(0.5));
    return subject;
  }

  @Test
  void tomlIsWrittenAndReadBack() {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(changed(), writer);
    String written = writer.toString();
    Assertions.assertEquals(
        """
        shapes = [{kind = 'Square', side = 4}, {kind = 'Circle', radius = 0.5}]

        [task]
        type = 'repeat'
        times = 3
        """,
        written);
    Subject read = new Subject();
    RESOLVER.load(read, new StringReader(written));
    Assertions.assertEquals(changed(), read);
  }

  @Test
  void anUnknownTypeIsRejected() {
    String message =
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RESOLVER.load(new Subject(), new StringReader("[task]\ntype = 'never'\n")))
            .getMessage();
    Assertions.assertTrue(message.contains("unknown type 'never'"), message);
  }

  // <editor-fold desc="subjects">

  interface Task {}

  @Polymorphic(discriminator = "kind")
  sealed interface Shape permits Circle, Square {}

  static class Subject {

    @Polymorphic({
      @Polymorphic.Subtype(name = "once", type = Once.class),
      @Polymorphic.Subtype(name = "repeat", type = Repeat.class)
    })
    Task task = new Once();

    List<Shape> shapes = List.of(new Circle(1.0));

    @Override
    public boolean equals(Object o) {
      return o instanceof Subject other && other.task.equals(task) && other.shapes.equals(shapes);
    }

    @Override
    public int hashCode() {
      return task.hashCode() * 31 + shapes.hashCode();
    }

    @Override
    public String toString() {
      return task + " " + shapes;
    }
  }

  record Once() implements Task {}

  record Repeat(int times) implements Task {}

  record Circle(double radius) implements Shape {}

  record Square(int side) implements Shape {}

  // </editor-fold>
}
