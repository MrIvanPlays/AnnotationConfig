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

package com.mrivanplays.annotationconfig.scenarios.fields;

import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.RawConfig;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Which fields are options, in every format: the fields of parent classes, static fields, ignored
 * fields, the raw config, and data types with one field and a constant.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestFields extends Scenario {

  // the formats run in parallel and share these static fields
  private static final String STATICS = "TestFields.statics";

  TestFields(Format format) {
    super(format, "fields");
  }

  // passed per call: changing the shared resolver's settings would leak into other tests
  private static Settings findParentFields() {
    return new Settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true);
  }

  private static Settings findStaticFields() {
    return new Settings().put(ACDefaultSettings.FIND_STATIC_FIELDS, true);
  }

  // <editor-fold desc="Parent classes">

  @Test
  void parentFieldsAreOptionsOnlyWithFindParentFields() {
    assertOutput("parent-fields-ignored", dump(new Extended()));
    assertOutput("parent-fields", dump(new Extended(), findParentFields()));
    Extended read = load(new Extended(), "parent-fields-read", findParentFields());
    Assertions.assertEquals(2, read.foo);
    Assertions.assertEquals("aaa", read.bar);
  }

  @Test
  void aHiddenParentFieldIsNotAnOption() {
    // the hidden foo of Parent was written too, which failed with "Duplicate key 'foo'"
    assertOutput("hidden-parent-field", dump(new Hiding(), findParentFields()));
    Hiding read = load(new Hiding(), "hidden-parent-field-read", findParentFields());
    Assertions.assertEquals("changed", read.foo);
    Assertions.assertEquals(1, ((Parent) read).foo);
  }

  @Test
  void theCommentsOfAncestorsComeFirst() {
    String written = dump(new Child(), findParentFields());
    assertOutput("hierarchy", written);
    Child read = new Child();
    read.g1 = 0;
    read.p1 = 0;
    read.c2 = 0;
    loadText(read, written, findParentFields());
    Assertions.assertEquals(
        List.of(1, 2, 3, 4, 5, 6), List.of(read.g1, read.g2, read.p1, read.p2, read.c1, read.c2));
  }

  // </editor-fold>

  // <editor-fold desc="Static fields">

  @Test
  @ResourceLock(STATICS)
  void staticFieldsAreOptionsOnlyWithFindStaticFields() {
    assertOutput("static-fields-ignored", dump(new WithStatic()));
    assertOutput("static-fields", dump(new WithStatic(), findStaticFields()));
    try {
      load(new WithStatic(), "static-fields-read");
      Assertions.assertEquals(5, WithStatic.counter, "loaded without FIND_STATIC_FIELDS");
      load(new WithStatic(), "static-fields-read", findStaticFields());
      Assertions.assertEquals(9, WithStatic.counter);
    } finally {
      WithStatic.counter = 5;
    }
  }

  @Test
  @ResourceLock(STATICS)
  void aDataTypeWithOnlyAStaticFieldIsNotRead() {
    try {
      Assertions.assertThrows(
          IllegalArgumentException.class, () -> load(new WithOnlyStatic(), "only-static"));
      // the static field was set to 9 before
      Assertions.assertEquals(5, OnlyStatic.counter);
    } finally {
      OnlyStatic.counter = 5;
    }
  }

  @Test
  void aConstantDoesNotStopTheSingleFieldShortcut() {
    // a section, which .properties can't write, before
    assertDump("single-field-with-constant", new WithPort());
    // failed with "does not have exactly 1 field" before
    WithPort read = load(new WithPort(), "single-field-with-constant-read");
    Assertions.assertEquals(8080, read.port.value);
  }

  // </editor-fold>

  // <editor-fold desc="Ignored fields and the raw config">

  @Test
  void ignoredFieldsAreNotOptions() {
    String written = assertDump("ignored", new WithIgnored());
    WithIgnored read = new WithIgnored();
    read.ignored = true;
    read.name = null;
    loadText(read, written);
    Assertions.assertEquals("Ivan", read.name);
    Assertions.assertTrue(read.ignored);
  }

  @Test
  void theRawConfigHoldsTheValuesRead() {
    WithRawConfig config = load(new WithRawConfig(), "raw-config-read");
    Assertions.assertTrue(config.raw.has("foo"));
    Assertions.assertTrue(config.raw.has("baz"));
    Assertions.assertEquals("bar", config.raw.get("foo").getAsString());
    Assertions.assertEquals("bar", config.foo);
    Assertions.assertEquals(2.5, config.baz);
  }

  @Test
  void theRawConfigHoldsTheValuesWritten() {
    WithRawConfig config = new WithRawConfig();
    dump(config);
    Assertions.assertNotNull(config.raw);
    Assertions.assertEquals("asd", config.raw.get("foo").getAsString());
    Assertions.assertEquals(2.3, config.raw.get("baz").getAsDouble());
  }

  // </editor-fold>

  // <editor-fold desc="Parent classes: subjects">

  @Comment("Test comment foo")
  static class Parent {

    int foo = 1;
  }

  static class Extended extends Parent {

    String bar = "baz";
  }

  static class Hiding extends Parent {

    String foo = "child";
  }

  @Comment("grandparent comment")
  static class Grandparent {

    int g1 = 1;
    int g2 = 2;
  }

  @Comment("parent comment")
  static class Middle extends Grandparent {

    int p1 = 3;
    int p2 = 4;
  }

  @Comment("child comment")
  static class Child extends Middle {

    int c1 = 5;
    int c2 = 6;
  }

  // </editor-fold>

  // <editor-fold desc="Static fields: subjects">

  static class WithStatic {

    public static int counter = 5;
    public String own = "own";
  }

  /** Only a static field, so nothing to read or write without FIND_STATIC_FIELDS. */
  static class OnlyStatic {

    static int counter = 5;

    public OnlyStatic() {}
  }

  static class WithOnlyStatic {

    OnlyStatic holder = new OnlyStatic();
  }

  /** One field and a constant: written and read as the field's value alone. */
  static class Port {

    static final int DEFAULT = 25565;

    int value = DEFAULT;

    public Port() {}
  }

  static class WithPort {

    Port port = new Port();
  }

  // </editor-fold>

  // <editor-fold desc="Ignored fields and the raw config: subjects">

  @Comment("This is on the top")
  static class WithIgnored {

    @Ignore private boolean ignored = false;

    @Comment("Hello my name is Ivan")
    private String name = "Ivan";
  }

  static class WithRawConfig {

    private String foo = "asd";
    private double baz = 2.3;

    @RawConfig private DataObject raw;
  }

  // </editor-fold>
}
