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

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Which fields of a custom data type the default serializer reads and writes: static fields only
 * with {@code FIND_STATIC_FIELDS}, as for the fields of a config, and fields that a config class
 * inherits are found when an instance to reuse is looked up.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class TestDefaultSerializerFields {

  private static final ConfigResolver RESOLVER = PropertyConfig.getConfigResolver();

  private static FieldTypeSerializer serializer() {
    return SerializerRegistry.INSTANCE.getDefaultSerializer();
  }

  private static AnnotationAccessor noAnnotations() {
    return AnnotationAccessor.EMPTY;
  }

  @AfterEach
  void resetStatic() {
    OnlyStatic.counter = 5;
  }

  @Test
  void aConstantDoesNotStopTheSingleFieldShortcutWhenDumping() {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(new WithPort(), writer);
    // a section, which .properties can't write, before
    Assertions.assertEquals("port=25565\n", writer.toString());
  }

  @Test
  void aConstantDoesNotStopTheSingleFieldShortcutWhenLoading() {
    WithPort config = new WithPort();
    RESOLVER.load(config, new StringReader("port=8080"));
    // failed with "does not have exactly 1 field" before
    Assertions.assertEquals(8080, config.port.value);
  }

  @Test
  void aStaticOnlyFieldIsNotLoadedWithoutFindStaticFields() {
    WithOnlyStatic config = new WithOnlyStatic();
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> RESOLVER.load(config, new StringReader("holder=9")));
    // the static field was set to 9 before
    Assertions.assertEquals(5, OnlyStatic.counter);
  }

  @Test
  void constantsOfADataTypeAreNotWritten() {
    DataObject serialized =
        serializer()
            .serialize(
                new Data(), SerializationContext.of(Data.class, Data.class, null), noAnnotations());
    // CONSTANT was written as well before
    Assertions.assertEquals(Map.of("name", "name", "amount", 3), serialized.getAsMap());
  }

  @Test
  void constantsOfADataTypeAreWrittenWithFindStaticFields() {
    Settings settings = new Settings().put(ACDefaultSettings.FIND_STATIC_FIELDS, true);
    DataObject serialized =
        serializer()
            .serialize(
                new Data(),
                SerializationContext.of(Data.class, Data.class, (Object) null, settings),
                noAnnotations());
    Assertions.assertEquals(
        Map.of("CONSTANT", "constant", "name", "name", "amount", 3), serialized.getAsMap());
  }

  @Test
  void aDataTypeWithAPrivateEmptyConstructorIsCreated() {
    Object read =
        serializer()
            .deserialize(
                new DataObject(Map.of("name", "read", "amount", 2), true),
                SerializationContext.of(PrivateConstructor.class, PrivateConstructor.class, null),
                noAnnotations());
    // failed with "does not have an empty constructor" before: only public ones were found
    PrivateConstructor value = Assertions.assertInstanceOf(PrivateConstructor.class, read);
    Assertions.assertEquals("read", value.name);
    Assertions.assertEquals(2, value.amount);
  }

  // <editor-fold desc="Fields of a parent class">

  @Test
  void anInstanceInAFieldOfTheParentClassIsReused() {
    Child config = new Child();
    Holder existing = config.holder;
    // no default value in the context, so the instance is looked up by the field's name
    Object read =
        serializer()
            .deserialize(
                new DataObject(Map.of("name", "read"), true),
                SerializationContext.of("holder", null, Holder.class, Holder.class, config),
                noAnnotations());
    // a new Holder without the existing amount before
    Assertions.assertSame(existing, read);
    Assertions.assertEquals("read", existing.name);
    Assertions.assertEquals(7, existing.amount);
  }

  @Test
  void aMissingRecordComponentKeepsTheValueOfAFieldOfTheParentClass() {
    Settings settings =
        new Settings()
            .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.USE_DEFAULT_VALUE);
    Object read =
        serializer()
            .deserialize(
                new DataObject(Map.of("name", "read"), true),
                SerializationContext.of(
                    "pair", null, Pair.class, Pair.class, new Child(), settings),
                noAnnotations());
    // the amount had no value, which failed to create the record before
    Assertions.assertEquals(new Pair("read", 7), read);
  }

  // </editor-fold>

  static class PrivateConstructor {

    String name = "name";
    int amount = 1;

    private PrivateConstructor() {}
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

  /** Only a static field, so nothing to read or write without FIND_STATIC_FIELDS. */
  static class OnlyStatic {

    static int counter = 5;

    public OnlyStatic() {}
  }

  static class WithOnlyStatic {

    OnlyStatic holder = new OnlyStatic();
  }

  static class Data {

    static final String CONSTANT = "constant";

    String name = "name";
    int amount = 3;
  }

  // <editor-fold desc="Fields of a parent class: subjects">

  public static class Holder {

    public String name = "default";
    public int amount;

    public Holder() {}

    Holder(String name, int amount) {
      this.name = name;
      this.amount = amount;
    }
  }

  public record Pair(String name, int amount) {}

  static class Parent {

    Holder holder = new Holder("kept", 7);
    Pair pair = new Pair("kept", 7);
  }

  static class Child extends Parent {}

  // </editor-fold>
}
