package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MapUtilsTest {

  private Map<String, Object> map1;
  private Map<String, Object> map2;

  @BeforeEach
  public void initialize() {
    map1 = new HashMap<>();
    map2 = new HashMap<>();

    Map<String, Object> baz = new HashMap<>();
    baz.put("bar", "aa");
    Map<String, Object> foo = new HashMap<>();
    foo.put("baz", baz);
    map1.put("foo", foo);

    Map<String, Object> baz1 = new HashMap<>();
    baz1.put("eee", "rrr");
    Map<String, Object> foo1 = new HashMap<>();
    foo1.put("baz", baz1);
    map2.put("foo", foo1);
  }

  @AfterEach
  public void terminate() {
    map1 = null;
    map2 = null;
  }

  @Test
  public void testMapUtils() {
    MapUtils.populateFirst(map1, map2);

    System.out.println(map1);
    Assertions.assertNotEquals(map1, map2);
  }
}
