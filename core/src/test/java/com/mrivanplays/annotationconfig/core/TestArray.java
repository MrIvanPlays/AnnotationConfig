package com.mrivanplays.annotationconfig.core;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestArray {

  static class Subject {

    char[] charArray = new char[] {'a', 'b', 'c'};
    boolean[] boolArray = new boolean[] {true, false, true};
    int[] intArray = new int[] {7, 8, 9, 10, 11, 12};

    BigInteger[] bigInteger = new BigInteger[] {BigInteger.valueOf(1), BigInteger.valueOf(2)};
    BigDecimal[] bigDecimal = new BigDecimal[] {BigDecimal.valueOf(1.2), BigDecimal.valueOf(3.4)};
    String[] stringArray = new String[] {"make", "america", "great", "again"};
  }

  @Test
  public void testRead() {
    Subject config = new Subject();
    PropertyConfig.getConfigResolver()
        .load(config, getClass().getClassLoader().getResourceAsStream("array-test.properties"));

    Assertions.assertEquals(1, config.intArray[0]);
    Assertions.assertEquals('d', config.charArray[0]);
    Assertions.assertFalse(config.boolArray[0]);
    Assertions.assertEquals(3, config.bigInteger[0].longValue());
    Assertions.assertEquals(5.6, config.bigDecimal[0].doubleValue());
    Assertions.assertEquals("europe", config.stringArray[0]);
  }

  @Test
  public void testWrite() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();

    PropertyConfig.getConfigResolver().dump(config, writer);

    String expected =
        "stringArray=make, america, great, again\n"
            + "\n"
            + "bigDecimal=1.2, 3.4\n"
            + "\n"
            + "bigInteger=1, 2\n"
            + "\n"
            + "intArray=7, 8, 9, 10, 11, 12\n"
            + "\n"
            + "boolArray=true, false, true\n"
            + "\n"
            + "charArray=a, b, c\n";

    Assertions.assertEquals(expected, writer.toString());
  }
}
