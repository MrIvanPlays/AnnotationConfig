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

import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.utils.TypeToken;
import java.io.StringWriter;
import java.lang.reflect.Type;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestEmptySectionObjectListSerialize {

  private static final Type LIST_TYPE =
      new TypeToken<SectionObjectList<SubjectValue>>() {}.getType();

  @BeforeAll
  static void registerSerializer() {
    SerializerRegistry.INSTANCE.registerSerializer(LIST_TYPE, new SectionObjectListSerializer<>());
  }

  @AfterAll
  static void unregisterSerializer() {
    SerializerRegistry.INSTANCE.unregisterSerializer(LIST_TYPE);
  }

  private static String dump(SubjectConfig config) {
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, writer);
    return writer.toString();
  }

  @Test
  void emptiedListIsLeftOut() {
    SubjectConfig config = new SubjectConfig();
    String expected =
        "list:\n"
            + "  baz:\n"
            + "    foo: \"lorem ipsum\"\n"
            + "    bar: \"dolor sit amet\"\n"
            + "\n"
            + "controlInteger: 1\n";
    Assertions.assertEquals(expected, dump(config));

    config.list.update("baz", null);
    Assertions.assertEquals("controlInteger: 1\n", dump(config));
  }

  static class SubjectValue {

    public String foo;
    public String bar;

    public SubjectValue() {}

    public SubjectValue(String foo, String bar) {
      this.foo = foo;
      this.bar = bar;
    }
  }

  static class SubjectConfig {

    public SectionObjectList<SubjectValue> list =
        SectionObjectList.newBuilderForType(SubjectValue.class)
            .defaultValue("baz", new SubjectValue("lorem ipsum", "dolor sit amet"))
            .build();

    private int controlInteger = 1;
  }
}
