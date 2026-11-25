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

import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@code @Polymorphic} values: sections, lists, arrays, records and custom data types. */
class TestPolymorphicType {

  private static final ConfigResolver RESOLVER = YamlConfig.getConfigResolver();

  private static String dump(Object config) {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(config, writer);
    return writer.toString();
  }

  private static <T> T load(T config, String yaml) {
    RESOLVER.load(config, new StringReader(yaml));
    return config;
  }

  private static String rejected(Object config, String yaml) {
    return Assertions.assertThrows(IllegalArgumentException.class, () -> load(config, yaml))
        .getMessage();
  }

  // <editor-fold desc="A @Polymorphic field">

  @Test
  void theDiscriminatorIsWrittenFirst() {
    String expected =
        """
        execution:
          type: "sequential"
          # milliseconds between tasks
          delay: 100
        """;
    Assertions.assertEquals(expected, dump(new Subject()));
  }

  @Test
  void eachImplementationIsWrittenAndReadBack() {
    for (ExecutionStrategy strategy :
        List.of(
            new SequentialStrategy(7),
            new ParallelStrategy(8),
            new FixedStrategy(5, "s"),
            new NoStrategy())) {
      Subject subject = new Subject();
      subject.execution = strategy;
      Subject read = load(new Subject(), dump(subject));
      Assertions.assertEquals(strategy, read.execution, dump(subject));
    }
  }

  @Test
  void aClassWithoutFieldsIsWrittenAsItsDiscriminator() {
    Subject subject = new Subject();
    subject.execution = new NoStrategy();
    Assertions.assertEquals(
        """
        execution:
          type: "none"
        """,
        dump(subject));
  }

  @Test
  void theDiscriminatorChoosesTheImplementation() {
    Subject subject = load(new Subject(), "execution: {type: parallel, threads: 8}");
    Assertions.assertEquals(new ParallelStrategy(8), subject.execution);
    subject = load(new Subject(), "execution: {type: fixed, every: 3, unit: m}");
    Assertions.assertEquals(new FixedStrategy(3, "m"), subject.execution);
  }

  @Test
  void aValueOfTheSameImplementationIsFilledInPlace() {
    Subject subject = new Subject();
    SequentialStrategy held = new SequentialStrategy(500);
    subject.execution = held;
    load(subject, "execution: {type: sequential}");
    // the option missing from the file keeps the value the field held
    Assertions.assertSame(held, subject.execution);
    Assertions.assertEquals(500, held.delay);
  }

  @Test
  void anotherImplementationIsCreatedWithItsConstructor() {
    Subject subject = new Subject();
    subject.execution = new SequentialStrategy(500);
    load(subject, "execution: {type: parallel}");
    Assertions.assertEquals(new ParallelStrategy(4), subject.execution);
  }

  @Test
  void aRecordReadsMissingComponentsAsRecordsDo() {
    // SET_NULL, the default: a missing component is null
    Subject subject = new Subject();
    subject.execution = new FixedStrategy(5, "s");
    load(subject, "execution: {type: fixed, every: 9}");
    Assertions.assertEquals(new FixedStrategy(9, null), subject.execution);
    // USE_DEFAULT_VALUE: it keeps its value from the record the field held, of the same type
    Settings useDefault =
        new Settings()
            .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.USE_DEFAULT_VALUE);
    subject.execution = new FixedStrategy(5, "s");
    RESOLVER.load(
        subject, new StringReader("execution: {type: fixed, every: 9}"), useDefault);
    Assertions.assertEquals(new FixedStrategy(9, "s"), subject.execution);
  }

  @Test
  void aMissingDiscriminatorKeepsTheImplementationAndIsWritten(@TempDir Path dir)
      throws IOException {
    Path file = dir.resolve("config.yml");
    Files.writeString(file, "execution:\n  delay: 3\n");
    Subject subject = new Subject();
    RESOLVER.load(subject, file);
    Assertions.assertEquals(new SequentialStrategy(3), subject.execution);
    // it is a missing option, so GENERATE_NEW_OPTIONS writes it
    Assertions.assertEquals(
        """
        execution:
          type: "sequential"
          # milliseconds between tasks
          delay: 3
        """,
        Files.readString(file));
  }

  @Test
  void aMissingSectionKeepsTheValue() {
    Subject subject = new Subject();
    ExecutionStrategy held = subject.execution;
    load(subject, "other: 1");
    Assertions.assertSame(held, subject.execution);
  }

  @Test
  void aDiscriminatorThatIsNotAStringIsMatchedByItsText() {
    Numbered numbered = load(new Numbered(), "value: {type: 2, threads: 6}");
    Assertions.assertEquals(new ParallelStrategy(6), numbered.value);
  }

  @Test
  void anUnknownDiscriminatorIsRejected() {
    String message = rejected(new Subject(), "execution: {type: random}");
    Assertions.assertTrue(message.contains("'execution'"), message);
    Assertions.assertTrue(message.contains("'random'"), message);
    Assertions.assertTrue(message.contains("[sequential, parallel, fixed, none]"), message);
    // YAML 1.1 reads on as a boolean
    message = rejected(new Subject(), "execution: {type: on}");
    Assertions.assertTrue(message.contains("'true'"), message);
    Assertions.assertTrue(message.contains("quote it"), message);
  }

  @Test
  void aDiscriminatorThatIsASectionIsRejected() {
    String message = rejected(new Subject(), "execution: {type: {a: 1}}");
    Assertions.assertTrue(message.contains("must name its type"), message);
  }

  @Test
  void anUnlistedClassIsNotWritten() {
    Subject subject = new Subject();
    // a subclass of a listed class, as an anonymous class is, is not listed itself
    subject.execution = new SequentialStrategy(1) {};
    String message =
        Assertions.assertThrows(IllegalArgumentException.class, () -> dump(subject))
            .getMessage();
    Assertions.assertTrue(message.contains("'execution'"), message);
    Assertions.assertTrue(message.contains("is not listed"), message);
  }

  // </editor-fold>

  // <editor-fold desc="A @Polymorphic type">

  @Test
  void aPolymorphicTypeIsWrittenEverywhereItIsUsed() {
    String expected =
        """
        shapes:
          - kind: "Circle"
            radius: 1.5
          - kind: "box"
            side: 2
          - kind: "Triangle"
            a: 3
            b: 4
            c: 5

        array:
          - kind: "box"
            side: 3

        single:
          kind: "Circle"
          radius: 2.0

        drawing:
          main:
            kind: "box"
            side: 4
          extra:
            - kind: "Circle"
              radius: 1.0

        canvas:
          background:
            kind: "Circle"
            radius: 9.0
          name: "c"
        """;
    Assertions.assertEquals(expected, dump(new Shapes()));
  }

  @Test
  void aPolymorphicTypeIsReadEverywhereItIsUsed() {
    Shapes read =
        load(
            new Shapes(),
            """
            shapes:
            - {kind: box, side: 7}
            - {kind: Triangle, a: 1, b: 1, c: 1}
            array:
            - {kind: Circle, radius: 0.5}
            single: {kind: box, side: 8}
            drawing:
              main: {kind: Circle, radius: 3.0}
              extra: [{kind: box, side: 9}]
            canvas:
              background: {kind: box, side: 10}
            """);
    Assertions.assertEquals(List.of(new Square(7), new Triangle(1, 1, 1)), read.shapes);
    Assertions.assertArrayEquals(new Shape[] {new Circle(0.5)}, read.array);
    Assertions.assertEquals(new Square(8), read.single);
    Assertions.assertEquals(new Drawing(new Circle(3.0), List.of(new Square(9))), read.drawing);
    Assertions.assertEquals(new Square(10), read.canvas.background);
    Assertions.assertEquals("c", read.canvas.name);
  }

  @Test
  void aListElementNeedsADiscriminator() {
    String message = rejected(new Shapes(), "shapes: [{radius: 1.0}]");
    Assertions.assertTrue(message.contains("no 'kind' key"), message);
    Assertions.assertTrue(message.contains("[Circle, box, Triangle]"), message);
  }

  @Test
  void aPolymorphicValueNeedsASection() {
    String message = rejected(new Shapes(), "drawing: {main: 5}");
    Assertions.assertTrue(message.contains("needs a section"), message);
  }

  @Test
  void anAnnotationOnAListFieldAppliesToTheElements() {
    String expected =
        """
        strategies:
          - type: "sequential"
            delay: 100
          - type: "parallel"
            threads: 4
        """;
    Assertions.assertEquals(expected, dump(new Tasks()));
    Tasks read = load(new Tasks(), "strategies: [{type: parallel, threads: 2}]");
    Assertions.assertEquals(List.of(new ParallelStrategy(2)), read.strategies);
  }

  // </editor-fold>

  // <editor-fold desc="A @Polymorphic field: subjects">

  interface ExecutionStrategy {}

  static class Subject {

    @Polymorphic({
      @Polymorphic.Subtype(name = "sequential", type = SequentialStrategy.class),
      @Polymorphic.Subtype(name = "parallel", type = ParallelStrategy.class),
      @Polymorphic.Subtype(name = "fixed", type = FixedStrategy.class),
      @Polymorphic.Subtype(name = "none", type = NoStrategy.class)
    })
    ExecutionStrategy execution = new SequentialStrategy();
  }

  static class Numbered {

    @Polymorphic({
      @Polymorphic.Subtype(name = "1", type = SequentialStrategy.class),
      @Polymorphic.Subtype(name = "2", type = ParallelStrategy.class)
    })
    ExecutionStrategy value = new SequentialStrategy();
  }

  static class SequentialStrategy implements ExecutionStrategy {

    @Comment("milliseconds between tasks")
    int delay = 100;

    SequentialStrategy() {}

    SequentialStrategy(int delay) {
      this.delay = delay;
    }

    @Override
    public boolean equals(Object o) {
      return o != null && o.getClass() == getClass() && ((SequentialStrategy) o).delay == delay;
    }

    @Override
    public int hashCode() {
      return delay;
    }

    @Override
    public String toString() {
      return "SequentialStrategy[" + delay + "]";
    }
  }

  static final class ParallelStrategy implements ExecutionStrategy {

    int threads = 4;

    ParallelStrategy() {}

    ParallelStrategy(int threads) {
      this.threads = threads;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof ParallelStrategy other && other.threads == threads;
    }

    @Override
    public int hashCode() {
      return threads;
    }

    @Override
    public String toString() {
      return "ParallelStrategy[" + threads + "]";
    }
  }

  record FixedStrategy(int every, String unit) implements ExecutionStrategy {}

  static final class NoStrategy implements ExecutionStrategy {

    @Override
    public boolean equals(Object o) {
      return o instanceof NoStrategy;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  // </editor-fold>

  // <editor-fold desc="A @Polymorphic type: subjects">

  @Polymorphic(discriminator = "kind")
  sealed interface Shape permits Circle, Square, Polygon {}

  sealed interface Polygon extends Shape permits Triangle {}

  static class Shapes {

    List<Shape> shapes = List.of(new Circle(1.5), new Square(2), new Triangle(3, 4, 5));
    Shape[] array = {new Square(3)};
    Shape single = new Circle(2.0);
    Drawing drawing = new Drawing(new Square(4), List.of(new Circle(1.0)));
    Canvas canvas = new Canvas();
  }

  static class Tasks {

    @Polymorphic({
      @Polymorphic.Subtype(name = "sequential", type = SequentialStrategy.class),
      @Polymorphic.Subtype(name = "parallel", type = ParallelStrategy.class)
    })
    List<ExecutionStrategy> strategies = List.of(new SequentialStrategy(), new ParallelStrategy());
  }

  record Circle(double radius) implements Shape {}

  @Polymorphic.Name("box")
  static final class Square implements Shape {

    int side = 1;

    Square() {}

    Square(int side) {
      this.side = side;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Square other && other.side == side;
    }

    @Override
    public int hashCode() {
      return side;
    }

    @Override
    public String toString() {
      return "Square[" + side + "]";
    }
  }

  record Triangle(int a, int b, int c) implements Polygon {}

  record Drawing(Shape main, List<Shape> extra) {}

  // a custom data type, not a section
  static final class Canvas {

    Shape background = new Circle(9.0);
    String name = "c";

    Canvas() {}

    @Override
    public boolean equals(Object o) {
      return o instanceof Canvas other
          && other.background.equals(background)
          && other.name.equals(name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(background, name);
    }
  }

  // </editor-fold>
}
