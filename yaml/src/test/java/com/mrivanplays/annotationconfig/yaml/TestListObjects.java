package com.mrivanplays.annotationconfig.yaml;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestListObjects {

  static class Subject {

    private List<Serialized> objects =
        Arrays.asList(new Serialized("foo", true), new Serialized("bar", "baz"));
  }

  static class Serialized {

    public String name;
    public Object value;

    public Serialized(String name, Object value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public String toString() {
      return "Serialized{" +
          "name='" + name + '\'' +
          ", value=" + value +
          '}';
    }
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, writer);

    String expected =
        "objects:\n"
            + "  - name: \"foo\"\n"
            + "    value: true\n"
            + "  - name: \"bar\"\n"
            + "    value: \"baz\"\n"
            + "\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  public void testLoad() {
    Subject config = new Subject();
    YamlConfig.getConfigResolver().load(config, getClass().getClassLoader().getResourceAsStream("list-objects.yml"));

    Assertions.assertEquals(3, config.objects.size());
    Assertions.assertEquals("asdfp", config.objects.get(0).name);
    Assertions.assertEquals("electricity", config.objects.get(2).value);
  }
}
