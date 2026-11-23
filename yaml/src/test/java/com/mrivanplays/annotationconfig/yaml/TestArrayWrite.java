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

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestArrayWrite {

  @Test
  void testWriteArray() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, writer);

    String expected =
        "foo: [1, 2]\n"
            + "\n"
            + "bigDec: [1111.2222, 3333.4444]\n"
            + "\n"
            + "myObjectArr:\n"
            + "  - foo: 1\n"
            + "    baz: true\n"
            + "  - foo: 2\n"
            + "    baz: false\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  void testReadArray() {
    Subject config = new Subject();
    YamlConfig.getConfigResolver()
        .load(config, getClass().getClassLoader().getResourceAsStream("array-write.yml"));

    Assertions.assertEquals(3, config.foo.length);
    Assertions.assertEquals(5, config.foo[2]);
    Assertions.assertEquals(2, config.myObjectArr.length);
    Assertions.assertEquals(3, config.myObjectArr[0].foo);
    Assertions.assertEquals(2, config.bigDec.length);
    Assertions.assertEquals(new BigDecimal("4444.5555"), config.bigDec[1]);
  }

  @Test
  void testReadEmptyArray() {
    Simple s = new Simple();
    StringReader r = new StringReader("foo: []");
    YamlConfig.getConfigResolver().load(s, r);

    Assertions.assertEquals(0, s.foo.length);
  }

  @Test
  void testReadSimple() {
    Simple s = new Simple();
    StringReader r = new StringReader("foo: [5, 6, 7]");
    YamlConfig.getConfigResolver().load(s, r);

    Assertions.assertEquals(3, s.foo.length);
    Assertions.assertEquals(5, s.foo[0]);
  }

  static class Subject {

    private int[] foo = new int[] {1, 2};
    private BigDecimal[] bigDec =
        new BigDecimal[] {new BigDecimal("1111.2222"), new BigDecimal("3333.4444")};
    private MyObject[] myObjectArr = new MyObject[] {new MyObject(1, true), new MyObject(2, false)};

    static class MyObject {

      private int foo;
      private boolean baz;

      public MyObject() {}

      public MyObject(int foo, boolean baz) {
        this.foo = foo;
        this.baz = baz;
      }

      public int getFoo() {
        return foo;
      }

      public boolean isBaz() {
        return baz;
      }
    }
  }

  static class Simple {
    private int[] foo = new int[] {1, 2, 3};
  }
}
