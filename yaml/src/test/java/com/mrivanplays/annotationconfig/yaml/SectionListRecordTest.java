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

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.utils.TypeToken;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SectionListRecordTest {

  record TestObj(String key, String value) {}

  record TestObj2(String key, int value) {

    TestObj2(String key) {
      this(key, -1);
    }
  }

  record TestObj3(@Key("super-cool-key") String key, int num, boolean persist) {

    TestObj3(String key) {
      this(key, -1, false);
    }
  }

  static class Subj {

    public SectionObjectList<TestObj> list =
        SectionObjectList.newBuilderForType(TestObj.class)
            .defaultValue("foo", new TestObj("bar", "baz"))
            .defaultValue("bar", new TestObj("lorem", "ipsum"))
            .build();
  }

  static class Subj2 {

    public SectionObjectList<TestObj2> list =
        SectionObjectList.newBuilderForType(TestObj2.class)
            .defaultValue("ipsum", new TestObj2("dolor", 1))
            .defaultValue("sit", new TestObj2("amet", 2))
            .build();
  }

  static class Subj3 {

    public SectionObjectList<TestObj3> list =
        SectionObjectList.newBuilderForType(TestObj3.class)
            .defaultValue("pump", new TestObj3("JAVA", 25, true))
            .defaultValue("llll", new TestObj3("wh", 34, false))
            .build();
  }

  private static final List<Type> LIST_TYPES =
      List.of(
          new TypeToken<SectionObjectList<TestObj>>() {}.getType(),
          new TypeToken<SectionObjectList<TestObj2>>() {}.getType(),
          new TypeToken<SectionObjectList<TestObj3>>() {}.getType());

  @BeforeAll
  static void registerSerializers() {
    for (Type type : LIST_TYPES) {
      SerializerRegistry.INSTANCE.registerSerializer(type, new SectionObjectListSerializer<>());
    }
  }

  @AfterAll
  static void unregisterSerializers() {
    for (Type type : LIST_TYPES) {
      SerializerRegistry.INSTANCE.unregisterSerializer(type);
    }
  }

  @Test
  void testDump() {
    StringWriter w = new StringWriter();
    Subj s = new Subj();
    YamlConfig.getConfigResolver().dump(s, w);
    String expected =
        "list:\n"
            + "  foo:\n"
            + "    key: \"bar\"\n"
            + "    value: \"baz\"\n"
            + "  bar:\n"
            + "    key: \"lorem\"\n"
            + "    value: \"ipsum\"\n";

    Assertions.assertEquals(expected, w.toString());
  }

  @Test
  void testLoad() {
    String payload =
        "list:\n"
            + "  foo:\n"
            + "    key: \"foo\"\n"
            + "    value: \"bar\"\n"
            + "  bar:\n"
            + "    key: \"dolor\"\n"
            + "    value: \"sit\"\n";
    StringReader r = new StringReader(payload);
    Subj s = new Subj();
    YamlConfig.getConfigResolver().load(s, r);

    Map<String, TestObj> map = s.list.getAsMap();

    Assertions.assertEquals("foo", map.get("foo").key());
    Assertions.assertEquals("bar", map.get("foo").value());
    Assertions.assertEquals("dolor", map.get("bar").key());
    Assertions.assertEquals("sit", map.get("bar").value());
  }

  @Test
  void testLoadDifferentCtor() {
    String payload =
        "list:\n"
            + "  aaa:\n"
            + "    key: \"bbb\"\n"
            + "    value: 5\n"
            + "  ccc:\n"
            + "    key: \"ddd\"\n"
            + "    value: 6\n";

    StringReader r = new StringReader(payload);
    Subj2 s = new Subj2();
    YamlConfig.getConfigResolver().load(s, r);

    Map<String, TestObj2> map = s.list.getAsMap();

    Assertions.assertEquals("bbb", map.get("aaa").key());
    Assertions.assertEquals(5, map.get("aaa").value());
    Assertions.assertEquals("ddd", map.get("ccc").key());
    Assertions.assertEquals(6, map.get("ccc").value());
  }

  @Test
  void testEvenDifferentCtorDump() {
    StringWriter w = new StringWriter();
    Subj3 s = new Subj3();
    YamlConfig.getConfigResolver().dump(s, w);

    String expected =
        "list:\n"
            + "  pump:\n"
            + "    super-cool-key: \"JAVA\"\n"
            + "    num: 25\n"
            + "    persist: true\n"
            + "  llll:\n"
            + "    super-cool-key: \"wh\"\n"
            + "    num: 34\n"
            + "    persist: false\n";

    Assertions.assertEquals(expected, w.toString());
  }

  @Test
  void testEvenDifferentCtorLoad() {
    String payload =
        "list:\n"
            + "  foo:\n"
            + "    super-cool-key: \"Lepa\"\n"
            + "    num: -5\n"
            + "    persist: false\n"
            + "\n";

    StringReader r = new StringReader(payload);
    Subj3 s = new Subj3();
    YamlConfig.getConfigResolver().load(s, r);

    Map<String, TestObj3> map = s.list.getAsMap();

    Assertions.assertEquals("Lepa", map.get("foo").key());
    Assertions.assertEquals(-5, map.get("foo").num());
    Assertions.assertFalse(map.get("foo").persist());
  }

  @Test
  void testNullReadKey() {
    String payload = "list:\n" + "  foo:\n" + "    num: -5\n" + "    persist: false\n" + "\n";

    StringReader r = new StringReader(payload);
    Subj3 s = new Subj3();
    YamlConfig.getConfigResolver().load(s, r);

    Map<String, TestObj3> map = s.list.getAsMap();

    Assertions.assertNull(map.get("foo").key());
    Assertions.assertEquals(-5, map.get("foo").num());
    Assertions.assertFalse(map.get("foo").persist());
  }

  @Test
  void testNullReadPersistsValue() {
    String payload = "list:\n" + "  pump:\n" + "    num: -5\n" + "    persist: false\n" + "\n";

    StringReader r = new StringReader(payload);
    Subj3 s = new Subj3();
    YamlConfig.getConfigResolver()
        .load(
            s,
            r,
            ACDefaultSettings.getDefault()
                .copy()
                .put(ACDefaultSettings.NULL_READ_HANDLER, NullReadHandleOption.USE_DEFAULT_VALUE));

    Assertions.assertNotNull(s.list.get("pump").key());
    Assertions.assertEquals(-5, s.list.get("pump").num());
    Assertions.assertFalse(s.list.get("pump").persist());
  }
}
