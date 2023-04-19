package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestMultilineString {

  static class Subject {

    @Key("control-field")
    public int controlField = -1;

    @Multiline public String multiline = " Look at ma! \n I am a multiline string!";
  }

  static class FailSubject {

    @Multiline private int foo = 1;
  }

  @Test
  public void testFailSubject() {
    Assertions.assertThrowsExactly(
        IllegalArgumentException.class,
        () -> {
          FailSubject subject = new FailSubject();
          YamlConfig.getConfigResolver().dump(subject, new StringWriter());
        });
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, writer);

    String expected =
        "control-field: -1\n"
            + "\n"
            + "multiline: \"\n"
            + " Look at ma! \\n\n"
            + " I am a multiline string!\"\n"
            + "\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  public void testLoad() {
    Subject config = new Subject();
    YamlConfig.getConfigResolver()
        .load(config, getClass().getClassLoader().getResourceAsStream("multiline-string.yml"));

    String expected = " Hello! \n" + " My name is Ivan! \n" + " I like to code! \n ";

    Assertions.assertEquals(expected, config.multiline);
    Assertions.assertEquals(-2, config.controlField);
  }
}
