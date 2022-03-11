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

  public static Map<String, Object> getLastCommonMap(
      Map<String, Object> map1, Map<String, Object> map2) {
    Map<String, Object> ret = map2;
    for (Map.Entry<String, Object> entry : map1.entrySet()) {
      if (map2.containsKey(entry.getKey())) {
        Object val = map2.get(entry.getKey());
        if (val instanceof Map) {
          ret = (Map<String, Object>) val;
        }
      }
    }
    return ret;
  }

  public static void populateFirst(Map<String, Object> map1, Map<String, Object> map2) {
    Map<String, Object> lastCommonMap1 = getLastCommonMap(map1, map2);
    Map<String, Object> lastCommonMap2 = getLastCommonMap(map2, map1);
    String lastKeyMap1;
    if (lastCommonMap1.size() == 1) {
      lastKeyMap1 = lastCommonMap1.keySet().stream().findFirst().get();
    } else {
      lastKeyMap1 = getLastKey(lastCommonMap1);
    }
    if (lastCommonMap2.containsKey(lastKeyMap1)) {
      Object contained = lastCommonMap2.get(lastKeyMap1);
      if (!(contained instanceof Map)) {
        throw new IllegalArgumentException("Something's wrong here. Check your annotated config.");
      }
      Map<String, Object> mContained = (Map<String, Object>) contained;
      Map<String, Object> m1 = (Map<String, Object>) lastCommonMap1.get(lastKeyMap1);
      mContained.putAll(m1);
    } else {
      lastCommonMap2.putAll(lastCommonMap1);
    }
  }

  private MapUtils() {
    throw new IllegalArgumentException("Instantiation of utility-type class.");
  }
}
