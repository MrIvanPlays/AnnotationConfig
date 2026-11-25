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

package com.mrivanplays.annotationconfig.scenarios.objects;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Maps, {@link DataObject}s, records and other objects, in every format with sections, and keys
 * that need quoting.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SECTIONS)
class TestObjects extends Scenario {

  TestObjects(Format format) {
    super(format, "objects");
  }

  // <editor-fold desc="Maps and DataObject">

  @Test
  void mapsAreWrittenAndReadBack() {
    final String written = assertDump("maps", new Maps());
    Maps read = new Maps();
    read.map = Map.of();
    read.tablesOnly = Map.of();
    loadText(read, written);
    Assertions.assertEquals(numbersAsDecimals(Maps.map()), numbersAsDecimals(read.map));
    Assertions.assertEquals(
        numbersAsDecimals(new Maps().tablesOnly), numbersAsDecimals(read.tablesOnly));
  }

  @Test
  void mapsLoad() {
    Assertions.assertEquals(Map.of("key", "other"), load(new Maps(), "maps-read").map);
  }

  @Test
  void dataObjectsAreWrittenAndRead() {
    assertDump("data-object", new DataObjectField());
    DataObject read = load(new DataObjectField(), "data-object-read").data;
    Assertions.assertEquals(3, read.get("foo").getAsInt());
    Assertions.assertEquals("foo", read.get("bar").getAsString());
    Assertions.assertEquals(42069, read.get("baz").getAsInt());
  }

  @Test
  void keysThatNeedQuotingAreWrittenAndReadBack() {
    final String written = assertDump("unusual-keys", new UnusualKeys());
    UnusualKeys read = new UnusualKeys();
    read.bare = 0;
    read.spaced = 0;
    read.quote = 0;
    read.table = Map.of();
    loadText(read, written);
    Assertions.assertEquals(1, read.bare);
    Assertions.assertEquals(2, read.spaced);
    Assertions.assertEquals(4, read.quote);
    Assertions.assertEquals(
        numbersAsDecimals(new UnusualKeys().table), numbersAsDecimals(read.table));
  }

  // </editor-fold>

  // <editor-fold desc="Records">

  @Test
  void recordsAreWrittenAndReadBack() {
    final String written = assertDump("records", new Records());
    Records read = new Records();
    read.spawn = new Location("x");
    read.home = new Location("y");
    loadText(read, written);
    Assertions.assertEquals(new Records().spawn, read.spawn);
    Assertions.assertEquals(new Records().home, read.home);
  }

  @Test
  void recordsLoad() {
    Records config = load(new Records(), "records-read");
    Assertions.assertEquals(new Location("end", 1, 2, 3.5, false), config.spawn);
    Assertions.assertEquals(new Location("overworld", 4, 5, 6.0, true), config.home);
  }

  @Test
  void aMissingComponentIsNullOrKeepsItsValue() {
    // world-name is missing: null with SET_NULL, the default
    Records config = load(new Records(), "records-missing-component");
    Assertions.assertEquals(new Location(null, 1, 2, 3.5, false), config.spawn);
    // and its value in the record the field held with USE_DEFAULT_VALUE
    Settings useDefault =
        new Settings()
            .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.USE_DEFAULT_VALUE);
    config = load(new Records(), "records-missing-component", useDefault);
    Assertions.assertEquals(new Location("world", 1, 2, 3.5, false), config.spawn);
  }

  // </editor-fold>

  // <editor-fold desc="Other objects">

  @Test
  void objectsAreWrittenAndReadBackByTheDefaultSerializer() {
    final String written = assertDump("objects", new Objects());
    Objects read = new Objects();
    read.plain.foo = null;
    read.plain.bar = 0;
    read.plain.baz = 0;
    read.withoutEmptyConstructor = new WithoutEmptyConstructor("x", 0);
    loadText(read, written);
    Assertions.assertEquals("foo string", read.plain.foo);
    Assertions.assertEquals(1, read.plain.bar);
    Assertions.assertEquals(0.555, read.plain.baz);
    // a type without an empty constructor is filled in the instance the field holds
    Assertions.assertEquals("world", read.withoutEmptyConstructor.world);
    Assertions.assertEquals(20, read.withoutEmptyConstructor.x);
  }

  @Test
  void aConfigWithoutOptions() {
    loadText(new Empty(), assertDump("empty", new Empty()));
  }

  // </editor-fold>

  // <editor-fold desc="Maps and DataObject: subjects">

  static class Maps {

    public Map<String, Object> map = map();
    // maps that hold only maps
    public Map<String, Object> tablesOnly =
        Map.of("inner", Map.of("deeper", Map.of("value", 2)));

    // linked maps, so that the written order is the insertion order
    static Map<String, Object> map() {
      Map<String, Object> nested = new LinkedHashMap<>();
      nested.put("deep", true);
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("key", "value");
      map.put("nested", nested);
      map.put("empty", new LinkedHashMap<>());
      map.put("list", List.of(Map.of(), Map.of("x", 1)));
      return map;
    }
  }

  static class DataObjectField {

    public DataObject data = data();

    private static DataObject data() {
      DataObject data = new DataObject();
      data.put("foo", 2);
      data.put("bar", "baz");
      data.put("baz", 69420);
      return data;
    }
  }

  static class UnusualKeys {

    @Key("bare_key-1")
    public int bare = 1;

    @Key("has space")
    public int spaced = 2;

    @Key("quo'te")
    public int quote = 4;

    @Key("table")
    public Map<String, Object> table = Map.of("dotted.key", Map.of("x", 1));
  }

  // </editor-fold>

  // <editor-fold desc="Records: subjects">

  record Location(@Key("world-name") String world, int x, int y, double z, boolean safe) {

    // records with more than one constructor are supported
    Location(String world) {
      this(world, 0, 0, 0, false);
    }
  }

  static class Records {

    @ConfigObject public Location spawn = new Location("world", 20, 2, 1.5, true);

    // not a config object: serialized by the default serializer
    public Location home = new Location("nether", -1, 64, 3.0, false);
  }

  // </editor-fold>

  // <editor-fold desc="Other objects: subjects">

  static class Objects {

    public Plain plain = new Plain("foo string", 1, 0.555);

    @Key("without-empty-constructor")
    public WithoutEmptyConstructor withoutEmptyConstructor =
        new WithoutEmptyConstructor("world", 20);
  }

  public static class Plain {

    private String foo;
    private int bar;
    private double baz;

    public Plain() {}

    Plain(String foo, int bar, double baz) {
      this.foo = foo;
      this.bar = bar;
      this.baz = baz;
    }
  }

  public static class WithoutEmptyConstructor {

    private String world;
    private int x;

    WithoutEmptyConstructor(String world, int x) {
      this.world = world;
      this.x = x;
    }
  }

  static class Empty {}

  // </editor-fold>
}
