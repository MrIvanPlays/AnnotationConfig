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

package com.mrivanplays.annotationconfig.scenarios.collections;

import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Arrays, lists and lists of objects and maps, in every format with sections. */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SECTIONS)
class TestCollections extends Scenario {

  // the formats run in parallel, and register the same serializer
  private static final String SINGLE_FIELD_SERIALIZER = "TestCollections.SingleField.Serializer";

  TestCollections(Format format) {
    super(format, "collections");
  }

  /** Runs {@code test}, with the serializer of {@link SingleField} registered if requested. */
  private static void withSerializer(boolean register, Runnable test) {
    SerializerRegistry registry = SerializerRegistry.INSTANCE;
    if (register) {
      registry.registerSerializer(SingleField.class, new SingleField.Serializer());
    }
    try {
      test.run();
    } finally {
      if (register) {
        registry.unregisterSerializer(SingleField.class);
      }
    }
  }

  // <editor-fold desc="Arrays and lists">

  @Test
  void arraysAndListsAreWrittenAndReadBack() {
    final String written = assertDump("arrays-and-lists", new Arrays());
    Arrays read = new Arrays();
    read.numbers = new int[0];
    read.doubles = new double[0];
    read.decimals = new BigDecimal[0];
    read.items = new Item[0];
    read.names = List.of();
    read.itemList = List.of();
    read.nested = List.of();
    read.empty = List.of("not", "empty");
    read.units = List.of();
    loadText(read, written);
    Assertions.assertArrayEquals(new int[] {1, 2}, read.numbers);
    Assertions.assertArrayEquals(new double[] {1.1, 2.2}, read.doubles);
    Assertions.assertArrayEquals(
        new BigDecimal[] {new BigDecimal("1111.2222"), new BigDecimal("3333.4444")},
        read.decimals);
    Assertions.assertArrayEquals(new Item[] {new Item("a", 1), new Item("b", 2)}, read.items);
    Assertions.assertEquals(List.of("Hello", "bla bla"), read.names);
    Assertions.assertEquals(List.of(new Item("c", 3)), read.itemList);
    Assertions.assertEquals(List.of(List.of(1, 2), List.of(3)), read.nested);
    Assertions.assertEquals(List.of(), read.empty);
    Assertions.assertEquals(List.of(TimeUnit.SECONDS, TimeUnit.MINUTES), read.units);
  }

  @Test
  void arraysAndListsLoad() {
    Arrays config = load(new Arrays(), "arrays-and-lists-read");
    Assertions.assertArrayEquals(new int[] {3, 4, 5}, config.numbers);
    Assertions.assertArrayEquals(new double[0], config.doubles);
    Assertions.assertArrayEquals(
        new BigDecimal[] {new BigDecimal("2222.3333"), new BigDecimal("4444.5555")},
        config.decimals);
    // an array of objects has the declared component type
    Assertions.assertEquals(Item[].class, config.items.getClass());
    Assertions.assertArrayEquals(new Item[] {new Item("x", 7)}, config.items);
    Assertions.assertEquals(List.of(), config.names);
    Assertions.assertEquals(List.of(new Item("y", 8), new Item("z", 9)), config.itemList);
    Assertions.assertEquals(List.of(List.of(9)), config.nested);
    Assertions.assertEquals(List.of("now", "filled"), config.empty);
    // enum elements, read case-insensitively
    Assertions.assertEquals(List.of(TimeUnit.MINUTES, TimeUnit.HOURS), config.units);
  }

  @Test
  void aSingleValueIsReadAsAListOfIt() {
    Assertions.assertEquals(
        List.of("aabb"), load(new SingleValueList(), "single-value-list").foo);
  }

  @Test
  void elementsAreConvertedToTheDeclaredType() {
    TypedElements config = load(new TypedElements(), "typed-elements");
    // parsers read small numbers as Integer; they used to end up in the List<Long> unconverted
    Assertions.assertEquals(List.of(1L, 2L), config.longs);
    Assertions.assertEquals(Long.class, ((Object) config.longs.get(0)).getClass());
    Assertions.assertEquals(List.of(List.of("a", "b"), List.of("c")), config.nested);
    Assertions.assertEquals(2, config.wildcard.size());
    // an empty string is a value, also in a list
    Assertions.assertEquals(List.of("", "a"), config.texts);
    // the values of maps too; JSON left them BigDecimal, the type its parser reads
    Assertions.assertEquals(Map.of("a", 1L, "b", 2L), config.counts);
    Assertions.assertEquals(Long.class, ((Object) config.counts.get("a")).getClass());
    Assertions.assertEquals(Map.of("odd", List.of(1, 3), "even", List.of(2)), config.groups);
    Assertions.assertEquals(Integer.class, ((Object) config.groups.get("odd").get(0)).getClass());
    // a value that can't be converted is kept as read, as it was before 3.1.0
    Assertions.assertEquals("many", ((Map<?, ?>) config.amounts).get("x"));
  }

  // </editor-fold>

  // <editor-fold desc="Lists of objects">

  @Test
  void listsOfObjects() {
    assertDump("list-of-objects", new ObjectList());
    ObjectList config = load(new ObjectList(), "list-of-objects-read");
    Assertions.assertEquals(3, config.objects.size());
    Assertions.assertEquals("asdfp", config.objects.get(0).name);
    Assertions.assertEquals(false, config.objects.get(0).value);
    Assertions.assertEquals("electricity", config.objects.get(2).value);
  }

  // a list of single field objects is a list of plain values, with a serializer and without
  @ParameterizedTest(name = "with serializer: {0}")
  @ValueSource(booleans = {true, false})
  @ResourceLock(SINGLE_FIELD_SERIALIZER)
  void listsOfSingleFieldObjects(boolean registerSerializer) {
    withSerializer(
        registerSerializer,
        () -> {
          assertDump("single-field-objects", new SingleFieldList());
          SingleFieldList config = load(new SingleFieldList(), "single-field-objects-read");
          Assertions.assertEquals(2, config.objects.size());
          Assertions.assertEquals("baz", config.objects.get(0).foo);
          Assertions.assertEquals("aabb", config.objects.get(1).foo);
        });
  }

  @Test
  void mapsWithListsOfMaps() {
    final String written = assertDump("maps-with-lists-of-maps", new MapOfLists());
    MapOfLists read = new MapOfLists();
    read.values = Map.of();
    loadText(read, written);
    Assertions.assertEquals(
        numbersAsDecimals(new MapOfLists().values), numbersAsDecimals(read.values));
  }

  // </editor-fold>

  // <editor-fold desc="Arrays and lists: subjects">

  static class Arrays {

    public int[] numbers = {1, 2};
    public double[] doubles = {1.1, 2.2};
    public BigDecimal[] decimals = {new BigDecimal("1111.2222"), new BigDecimal("3333.4444")};
    public Item[] items = {new Item("a", 1), new Item("b", 2)};
    public List<String> names = List.of("Hello", "bla bla");
    public List<Item> itemList = List.of(new Item("c", 3));
    public List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3));
    public List<String> empty = Collections.emptyList();
    public List<TimeUnit> units = List.of(TimeUnit.SECONDS, TimeUnit.MINUTES);
  }

  public static class Item {

    public String name;
    public int amount;

    public Item() {}

    Item(String name, int amount) {
      this.name = name;
      this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof Item other && other.name.equals(name) && other.amount == amount;
    }

    @Override
    public int hashCode() {
      return name.hashCode() * 31 + amount;
    }

    @Override
    public String toString() {
      return "Item[" + name + ", " + amount + "]";
    }
  }

  static class SingleValueList {

    public List<String> foo;
  }

  static class TypedElements {

    public List<Long> longs;
    public List<List<String>> nested;
    public List<? extends Number> wildcard;
    public List<String> texts;
    public Map<String, Long> counts;
    public Map<String, List<Integer>> groups;
    public Map<String, Integer> amounts;
  }

  // </editor-fold>

  // <editor-fold desc="Lists of objects: subjects">

  static class ObjectList {

    private List<Named> objects = List.of(new Named("foo", true), new Named("bar", "baz"));
  }

  public static class Named {

    public String name;
    public Object value;

    public Named() {}

    Named(String name, Object value) {
      this.name = name;
      this.value = value;
    }
  }

  static class SingleFieldList {

    List<SingleField> objects = List.of(new SingleField("foo"), new SingleField("bar"));
  }

  static class SingleField {

    String foo;

    public SingleField() {}

    SingleField(String foo) {
      this.foo = foo;
    }

    @NullMarked
    static class Serializer implements FieldTypeSerializer<SingleField> {

      @Override
      public SingleField deserialize(
          DataObject data,
          SerializationContext<SingleField> context,
          AnnotationAccessor annotations) {
        return new SingleField(data.getAsString());
      }

      @Override
      public DataObject serialize(
          SingleField value,
          SerializationContext<SingleField> context,
          AnnotationAccessor annotations) {
        return new DataObject(value.foo);
      }
    }
  }

  /** A map holding a list of maps, whose values are a list in one entry and a map in the other. */
  static class MapOfLists {

    public Map<String, List<Map<String, Object>>> values = Map.of("entries", entries());

    // linked maps, so that the written order is the insertion order
    private static List<Map<String, Object>> entries() {
      Map<String, Object> first = new LinkedHashMap<>();
      first.put("foo", 1);
      first.put("bar", 2);
      first.put("baz", List.of("baba", "yaga"));
      Map<String, Object> bazMap = new LinkedHashMap<>();
      bazMap.put("baba", "yaga");
      bazMap.put("lorem", "ipsum");
      Map<String, Object> second = new LinkedHashMap<>();
      second.put("foo", 3);
      second.put("bar", 4);
      second.put("baz", bazMap);
      List<Map<String, Object>> entries = new ArrayList<>();
      entries.add(first);
      entries.add(second);
      return entries;
    }
  }

  // </editor-fold>
}
