package com.mrivanplays.annotationconfig.yaml;

import java.io.StringWriter;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArrayWriteTest {

  static class Subject {

    private int[] foo = new int[] {1, 2};
    private BigDecimal[] bigDec =
        new BigDecimal[] {new BigDecimal("1111.2222"), new BigDecimal("3333.4444")};
    private MyObject[] myObjectArr = new MyObject[] {new MyObject(1, true), new MyObject(2, false)};

    static class MyObject {

      private int foo;
      private boolean baz;

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

  @Test
  public void testWriteArray() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, writer);

    String expected = "bigDec: [1111.2222, 3333.4444]\n"
        + "\n"
        + "foo: [1, 2]\n"
        + "\n"
        + "myObjectArr:\n"
        + "  - foo: 1\n"
        + "    baz: true\n"
        + "  - foo: 2\n"
        + "    baz: false\n\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  public void testReadArray() {
    Subject config = new Subject();
    YamlConfig.getConfigResolver().load(config,  getClass().getClassLoader().getResourceAsStream("array-write.yml"));

    Assertions.assertEquals(3, config.foo.length);
    Assertions.assertEquals(5, config.foo[2]);
    Assertions.assertEquals(2, config.myObjectArr.length);
    Assertions.assertEquals(3, config.myObjectArr[0].foo);
    Assertions.assertEquals(2, config.bigDec.length);
    Assertions.assertEquals(4444.5555, config.bigDec[1].doubleValue());
  }
}
