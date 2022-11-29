package com.mrivanplays.annotationconfig.core;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestBigIntegerBigDecimal {

  public static class Subject {

    private BigDecimal decimal = new BigDecimal("1111111111.22222222");
    private BigInteger integer = new BigInteger("1111111111");
  }

  @Test
  public void testSerialize() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    PropertyConfig.getConfigResolver().dump(config, writer);

    String expected = "integer=1111111111\n" + "\n" + "decimal=1111111111.22222222\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  public void testDeserialize() {
    Subject config = new Subject();
    PropertyConfig.getConfigResolver()
        .load(
            config,
            getClass().getClassLoader().getResourceAsStream("big-int-big-dec-test.properties"));

    Assertions.assertEquals(3.3333333332222223E9, config.decimal.doubleValue());
    Assertions.assertEquals(3333333333L, config.integer.longValueExact());
  }
}
