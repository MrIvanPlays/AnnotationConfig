package com.mrivanplays.annotationconfig.yaml;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListEntryMapWithMapValueMapTest {

  static class Subject {

    private List<Map<String, List<Map<String, Object>>>> values =
        Collections.singletonList(
            Collections.singletonMap(
                "foo", Collections.singletonList(Collections.singletonMap("bar", 222))));
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, writer);

    String expected = "values:\n" + " - foo:\n" + "    - bar: 222\n\n";

    Assertions.assertEquals(expected, writer.toString());
  }
}
