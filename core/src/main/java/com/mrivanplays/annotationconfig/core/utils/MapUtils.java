package com.mrivanplays.annotationconfig.core.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A utility class for interacting with {@link Map Maps} and combining them, putting values at the
 * right place. They're self-explanatory and they won't be documented. If you are going to use them,
 * do not expect support.
 *
 * @author MrIvanPlays
 * @since 2.1.0
 */
public final class MapUtils {

  public static String getLastKey(Map<String, Object> map) {
    List<String> keys = new ArrayList<>(map.keySet());
    return keys.get(keys.size() - 1);
  }

  public static Map<String, Object> getLastMap(Map<String, Object> map) {
    Map<String, Object> ret = map;
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      Object val = entry.getValue();
      if (!(val instanceof Map)) {
        break;
      }
      ret = getLastMap((Map<String, Object>) val);
    }
    return ret;
  }

  public static Map<String, Object> getLastCommonMap(
      Map<String, Object> map1, Map<String, Object> map2) {
    Map<String, Object> ret = map2;
    for (Map.Entry<String, Object> entry : map1.entrySet()) {
      Object val = entry.getValue();
      if (!(val instanceof Map)) {
        break;
      }
      Map<String, Object> valMap = (Map<String, Object>) val;
      ret = getLastCommonMap(map2, valMap);
    }
    return ret;
  }

  public static void populateFirst(Map<String, Object> map1, Map<String, Object> map2) {
    Map<String, Object> lastCommonMap1 = getLastCommonMap(map1, map2);
    Map<String, Object> lastCommonMap2 = getLastCommonMap(map2, map1);
    lastCommonMap2.putAll(lastCommonMap1);
  }

  private MapUtils() {
    throw new IllegalArgumentException("Instantiation of utility-type class.");
  }
}
