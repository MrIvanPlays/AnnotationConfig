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

package com.mrivanplays.annotationconfig.scenarios.polymorphic;

import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code @Polymorphic} values in every format with sections: config fields, lists, arrays, records
 * and custom data types.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SECTIONS)
class TestPolymorphic extends Scenario {

  TestPolymorphic(Format format) {
    super(format, "polymorphic");
  }

  // <editor-fold desc="A @Polymorphic field">

  @Test
  void theDiscriminatorIsWrittenFirst() {
    assertOutput("subject", dump(new Subject()));
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
      String written = dump(subject);
      Subject read = new Subject();
      loadText(read, written);
      Assertions.assertEquals(strategy, read.execution, written);
    }
  }

  @Test
  void aClassWithoutFieldsIsWrittenAsItsDiscriminator() {
    Subject subject = new Subject();
    subject.execution = new NoStrategy();
    assertOutput("no-fields", dump(subject));
  }

  @Test
  void theDiscriminatorChoosesTheImplementation() {
    Assertions.assertEquals(
        new ParallelStrategy(8), load(new Subject(), "choose-parallel").execution);
    Assertions.assertEquals(
        new FixedStrategy(3, "m"), load(new Subject(), "choose-fixed").execution);
  }

  @Test
  void aValueOfTheSameImplementationIsFilledInPlace() {
    Subject subject = new Subject();
    SequentialStrategy held = new SequentialStrategy(500);
    subject.execution = held;
    load(subject, "same-implementation");
    // the option missing from the file keeps the value the field held
    Assertions.assertSame(held, subject.execution);
    Assertions.assertEquals(500, held.delay);
  }

  @Test
  void anotherImplementationIsCreatedWithItsConstructor() {
    Subject subject = new Subject();
    subject.execution = new SequentialStrategy(500);
    load(subject, "other-implementation");
    Assertions.assertEquals(new ParallelStrategy(4), subject.execution);
  }

  @Test
  void aRecordReadsMissingComponentsAsRecordsDo() {
    // SET_NULL, the default: a missing component is null
    Subject subject = new Subject();
    subject.execution = new FixedStrategy(5, "s");
    load(subject, "record-missing-component");
    Assertions.assertEquals(new FixedStrategy(9, null), subject.execution);
    // USE_DEFAULT_VALUE: it keeps its value from the record the field held, of the same type
    Settings useDefault =
        new Settings()
            .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.USE_DEFAULT_VALUE);
    subject.execution = new FixedStrategy(5, "s");
    load(subject, "record-missing-component", useDefault);
    Assertions.assertEquals(new FixedStrategy(9, "s"), subject.execution);
  }

  @Test
  void aMissingDiscriminatorKeepsTheImplementationAndIsWritten(@TempDir Path dir)
      throws IOException {
    Path file = dir.resolve("config." + this.format.extension());
    Files.writeString(file, text("missing-discriminator"));
    Subject subject = new Subject();
    // set, as TOML keeps it off by default
    this.resolver.load(subject, file, generateNewOptions());
    Assertions.assertEquals(new SequentialStrategy(3), subject.execution);
    // it is a missing option, so GENERATE_NEW_OPTIONS writes it
    assertOutput("missing-discriminator-written", Files.readString(file));
  }

  @Test
  void aMissingSectionKeepsTheValue() {
    Subject subject = new Subject();
    ExecutionStrategy held = subject.execution;
    load(subject, "missing-section");
    Assertions.assertSame(held, subject.execution);
  }

  @Test
  void aDiscriminatorThatIsNotAStringIsMatchedByItsText() {
    Assertions.assertEquals(new ParallelStrategy(6), load(new Numbered(), "numbered").value);
  }

  @Test
  void anUnknownDiscriminatorIsRejected() {
    String message = rejected(new Subject(), "unknown");
    Assertions.assertTrue(message.contains("'execution'"), message);
    Assertions.assertTrue(message.contains("'random'"), message);
    Assertions.assertTrue(message.contains("[sequential, parallel, fixed, none]"), message);
  }

  @Test
  void aDiscriminatorThatIsASectionIsRejected() {
    String message = rejected(new Subject(), "discriminator-section");
    Assertions.assertTrue(message.contains("must name its type"), message);
  }

  @Test
  void anUnlistedClassIsNotWritten() {
    Subject subject = new Subject();
    // a subclass of a listed class, as an anonymous class is, is not listed itself
    subject.execution = new SequentialStrategy(1) {};
    String message =
        Assertions.assertThrows(IllegalArgumentException.class, () -> dump(subject)).getMessage();
    Assertions.assertTrue(message.contains("'execution'"), message);
    Assertions.assertTrue(message.contains("is not listed"), message);
  }

  // </editor-fold>

  // <editor-fold desc="A @Polymorphic type">

  @Test
  void aPolymorphicTypeIsWrittenEverywhereItIsUsed() {
    String written = dump(new Shapes());
    assertOutput("shapes", written);
    Shapes read = new Shapes();
    read.shapes = List.of();
    loadText(read, written);
    Assertions.assertEquals(new Shapes().shapes, read.shapes);
  }

  @Test
  void aPolymorphicTypeIsReadEverywhereItIsUsed() {
    Shapes read = load(new Shapes(), "shapes-read");
    Assertions.assertEquals(List.of(new Square(7), new Triangle(1, 1, 1)), read.shapes);
    Assertions.assertArrayEquals(new Shape[] {new Circle(0.5)}, read.array);
    Assertions.assertEquals(new Square(8), read.single);
    Assertions.assertEquals(new Drawing(new Circle(3.0), List.of(new Square(9))), read.drawing);
    Assertions.assertEquals(new Square(10), read.canvas.background);
    Assertions.assertEquals("c", read.canvas.name);
  }

  @Test
  void aListElementNeedsADiscriminator() {
    String message = rejected(new Shapes(), "element-without-discriminator");
    Assertions.assertTrue(message.contains("no 'kind' key"), message);
    Assertions.assertTrue(message.contains("[Circle, box, Triangle]"), message);
  }

  @Test
  void aPolymorphicValueNeedsASection() {
    String message = rejected(new Shapes(), "value-not-a-section");
    Assertions.assertTrue(message.contains("needs a section"), message);
  }

  @Test
  void anAnnotationOnAListFieldAppliesToTheElements() {
    assertOutput("tasks", dump(new Tasks()));
    Assertions.assertEquals(
        List.of(new ParallelStrategy(2)), load(new Tasks(), "tasks-read").strategies);
  }

  @Test
  void anAnnotationOnASetFieldAppliesToTheElements() {
    assertOutput("tasks", dump(new TaskSet()));
    Assertions.assertEquals(
        Set.of(new ParallelStrategy(2)), load(new TaskSet(), "tasks-read").strategies);
  }

  // </editor-fold>

  // <editor-fold desc="A @Polymorphic field: subjects">

  interface ExecutionStrategy {}

  @Polymorphic(discriminator = "kind")
  sealed interface Shape permits Circle, Square, Polygon {}

  sealed interface Polygon extends Shape permits Triangle {}

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

  // </editor-fold>

  // <editor-fold desc="A @Polymorphic type: subjects">

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

  static class TaskSet {

    @Polymorphic({
      @Polymorphic.Subtype(name = "sequential", type = SequentialStrategy.class),
      @Polymorphic.Subtype(name = "parallel", type = ParallelStrategy.class)
    })
    Set<ExecutionStrategy> strategies =
        new LinkedHashSet<>(List.of(new SequentialStrategy(), new ParallelStrategy()));
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
