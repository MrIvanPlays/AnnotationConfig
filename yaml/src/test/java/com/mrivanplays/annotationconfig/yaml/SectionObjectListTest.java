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

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.utils.TypeToken;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SectionObjectListTest {

  static class SerializedObject {

    private int foo;
    private String baz;

    public SerializedObject() {}

    public SerializedObject(int foo, String baz) {
      this.foo = foo;
      this.baz = baz;
    }

    public int getFoo() {
      return foo;
    }

    public String getBaz() {
      return baz;
    }
  }

  static class Subject {

    public SectionObjectList<SerializedObject> list =
        SectionObjectList.newBuilderForType(SerializedObject.class)
            .defaultValue("foo", new SerializedObject(1, "lorem"))
            .defaultValue("bar", new SerializedObject(2, "ipsum"))
            .build();
  }

  private static final Type LIST_TYPE =
      new TypeToken<SectionObjectList<SerializedObject>>() {}.getType();
  private static final ConfigResolver resolver = YamlConfig.getConfigResolver();

  @BeforeAll
  static void registerSerializer() {
    SerializerRegistry.INSTANCE.registerSerializer(
        LIST_TYPE, new SectionObjectListSerializer<SerializedObject>());
  }

  @AfterAll
  static void unregisterSerializer() {
    SerializerRegistry.INSTANCE.unregisterSerializer(LIST_TYPE);
  }

  @Test
  void testDump() {
    StringWriter writer = new StringWriter();
    resolver.dump(new Subject(), writer);
    String expected =
        "list:\n"
            + "  foo:\n"
            + "    foo: 1\n"
            + "    baz: \"lorem\"\n"
            + "  bar:\n"
            + "    foo: 2\n"
            + "    baz: \"ipsum\"\n";
    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  void testLoad() {
    Subject subject = new Subject();
    String input =
        "list:\n"
            + "  baz:\n"
            + "    foo: 3\n"
            + "    baz: \"dolor\"\n"
            + "  foo:\n"
            + "    foo: 69\n"
            + "    baz: \"sit\"\n";
    resolver.load(subject, new StringReader(input));

    Map<String, SerializedObject> serialized = subject.list.getAsMap();
    Assertions.assertEquals(3, serialized.get("baz").getFoo());
    Assertions.assertEquals("dolor", serialized.get("baz").getBaz());
    Assertions.assertEquals(69, serialized.get("foo").getFoo());
    Assertions.assertEquals("sit", serialized.get("foo").getBaz());
  }
}
