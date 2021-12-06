package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDataObjectSerialization {

  static class Subject {

    private DataObject data = getData();

    private DataObject getData() {
      DataObject ret = new DataObject();
      ret.put("foo", 2);
      ret.put("bar", "baz");
      ret.put("baz", 69420);
      return ret;
    }

    public DataObject data() {
      return data;
    }
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, writer);

    String expected = "data:\n" + "  foo: 2\n" + "  bar: \"baz\"\n" + "  baz: 69420\n" + "\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  public void testLoad() {
    Subject config = new Subject();
    YamlConfig.getConfigResolver()
        .load(
            config,
            getClass().getClassLoader().getResourceAsStream("data-object-serialization.yml"));

    DataObject received = config.data();

    Assertions.assertEquals(3, received.get("foo").getAsInt());
    Assertions.assertEquals("foo", received.get("bar").getAsString());
    Assertions.assertEquals(42069, received.get("baz").getAsInt());
  }
}
