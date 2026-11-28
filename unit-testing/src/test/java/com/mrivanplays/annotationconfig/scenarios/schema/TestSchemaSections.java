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

package com.mrivanplays.annotationconfig.scenarios.schema;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.OneOf;
import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.ConfigSchema;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** The schema of sections, collections and the values of the default serializer. */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SECTIONS)
class TestSchemaSections extends Scenario {

  TestSchemaSections(Format format) {
    super(format, "schema");
  }

  private void assertSchema(String name, Object config) {
    ConfigSchema schema = this.resolver.schema(config);

    assertOutput(name, schema.toJson());
    TestSchema.assertReadsBack(schema);
  }

  // <editor-fold desc="Sections">

  @Test
  void sectionsAndDottedKeysAreNested() {
    assertSchema("sections", new Sections());
  }

  @Test
  void collectionsAreDescribedDownToTheirElements() {
    assertSchema("collections", new Containers());
  }

  @Test
  void recordsAndCustomDataTypesAreDescribedAsTheyAreWritten() {
    assertSchema("objects", new DataTypes());
  }

  @Test
  void polymorphicValuesAreAnyOfTheirImplementations() {
    assertSchema("polymorphic", new Shapes());
  }

  // </editor-fold>

  // <editor-fold desc="Sections: subjects">

  interface Shape {}

  static class Sections {

    @Comment("Where to connect")
    @ConfigObject
    Network network = new Network();

    @ConfigObject Limits limits = new Limits();

    @Key("limits.players")
    @Max(maxInt = 100)
    int players = 20;

    @Key("a.b.c")
    String deep = "deep";
  }

  @Comment("The network's settings")
  static class Network {

    String host = "localhost";

    @Min(minInt = 1)
    int port = 80;
  }

  static class Limits {

    int rate = 5;
  }

  static class Containers {

    List<@Max(maxInt = 100) Integer> scores = new ArrayList<>(List.of(1, 2));

    Map<@OneOf({"red", "blue"}) String, Integer> teams = new LinkedHashMap<>(Map.of("red", 1));

    Map<String, List<String>> groups = new LinkedHashMap<>(Map.of("admins", List.of("ivan")));

    List<? extends @Min(minLong = 0) Long> wildcard = new ArrayList<>(List.of(3L));

    String[][] grid = {{"a", "b"}};

    Object anything = "text";
  }

  record Point(int x, @Max(maxInt = 255) int y) {}

  record Single(String value) {}

  static class Wrapper {

    String value = "wrapped";
  }

  static class Item {

    @Comment("The item's id")
    String id = "stone";

    int amount = 1;

    Item next;
  }

  static class DataTypes {

    Point spawn = new Point(0, 64);

    Single single = new Single("one");

    Wrapper wrapper = new Wrapper();

    Item item = new Item();

    List<Point> points = new ArrayList<>(List.of(new Point(1, 2)));
  }

  @Comment("A circle")
  static class Circle implements Shape {

    @Min(minInt = 1)
    int radius = 1;
  }

  static class Square implements Shape {

    int side = 2;
  }

  static class Shapes {

    @Polymorphic({
      @Polymorphic.Subtype(name = "circle", type = Circle.class),
      @Polymorphic.Subtype(name = "square", type = Square.class)
    })
    Shape shape = new Circle();

    @Polymorphic(
        discriminator = "kind",
        value = {
          @Polymorphic.Subtype(name = "circle", type = Circle.class),
          @Polymorphic.Subtype(name = "square", type = Square.class)
        })
    List<Shape> shapes = new ArrayList<>(List.of(new Square()));
  }

  // </editor-fold>
}
