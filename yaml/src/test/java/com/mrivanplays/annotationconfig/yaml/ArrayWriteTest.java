package com.mrivanplays.annotationconfig.yaml;

import java.io.StringWriter;
import org.junit.jupiter.api.Test;

public class ArrayWriteTest {

  static class Subject {

    private int[] foo = new int[] {1, 2};
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

    // todo: write conditions
  }
}
