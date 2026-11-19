/*
 * Copyright (c) Ivan Pekov 2026
 * Copyright (c) Contributors 2026
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.mrivanplays.annotationconfig.core.utils;

import java.util.Iterator;
import java.util.Map;

/**
 * A utility class for interacting with {@link Map Maps} and combining them, putting values at the
 * right place. They're self-explanatory and they won't be documented. If you are going to use them,
 * do not expect support.
 *
 * @author Ivan Pekov
 * @since 2.1.0
 */
@SuppressWarnings("unchecked")
public final class MapUtils {

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
    String lastKeyMap1 = null;
    Iterator<String> iterator = lastCommonMap1.keySet().iterator();
    int i = 0;
    while (iterator.hasNext()) {
      if (lastCommonMap1.size() == 1 || (i + 1) == lastCommonMap1.size()) {
        lastKeyMap1 = iterator.next();
        break;
      }
      iterator.next();
      i++;
    }
    if (lastCommonMap2.containsKey(lastKeyMap1)) {
      Object contained = lastCommonMap2.get(lastKeyMap1);
      if (!(contained instanceof Map)) {
        lastCommonMap2.put(lastKeyMap1, contained);
        return;
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
