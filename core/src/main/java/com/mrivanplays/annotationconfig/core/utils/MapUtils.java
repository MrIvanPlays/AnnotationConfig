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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Helpers for the maps AnnotationConfig reads and writes, where a value can itself be a map (a
 * section of the config).
 *
 * <p>They cover:
 *
 * <ul>
 *   <li>merging the maps of dotted keys that share their first parts ({@link #populateFirst(Map,
 *       Map)});
 *   <li>copying maps with their keys as text, as values read from a file can have keys that are not
 *       strings, such as numbers in YAML ({@link #copyWithStringKeys(Map, Function)} and the
 *       methods built on it);
 *   <li>copying maps and the maps and lists in them into ones that can't be modified ({@link
 *       #unmodifiableDeepCopy(Map, Function)});
 *   <li>finding the sections that hold a key of a map by dotted path ({@link #parentPaths(Map)}).
 * </ul>
 *
 * <p>They are made for AnnotationConfig's own maps. Each method describes what it does, including
 * its limits, so read it before using it elsewhere.
 *
 * @author Ivan Pekov
 * @since 2.1.0
 */
@SuppressWarnings("unchecked")
@NullMarked
public final class MapUtils {

  private MapUtils() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * @deprecated not used by AnnotationConfig anymore
   */
  @Deprecated(since = "3.1.0", forRemoval = true)
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

  /**
   * Merges {@code map2} into {@code map1}, at any depth, changing {@code map1} and the maps in it.
   * Used to merge the values of dotted keys that share their first parts: {@code server.ip} and
   * {@code server.port} give {@code {ip=...}} and {@code {port=...}}, merged into one {@code
   * server} section.
   *
   * <p>For every entry of {@code map2}:
   *
   * <ul>
   *   <li>if both maps hold a map under its key, the two maps are merged the same way;
   *   <li>if {@code map1} doesn't have the key, the entry is put into {@code map1};
   *   <li>otherwise {@code map1} keeps its value.
   * </ul>
   *
   * <p>So {@code {a={b={c={x=1}}}}} and {@code {a={b={c={y=2}}}}} give {@code {a={b={c={x=1,
   * y=2}}}}}. {@code map2} is not changed, but the maps it holds are put into {@code map1} as they
   * are, not copied, so a later merge into {@code map1} can change them.
   *
   * @param map1 the map that is merged into
   * @param map2 the map whose entries are merged into {@code map1}
   */
  public static void populateFirst(Map<String, Object> map1, Map<String, Object> map2) {
    for (Map.Entry<String, Object> entry : map2.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      Object existing = map1.get(key);
      if (existing instanceof Map && value instanceof Map) {
        populateFirst((Map<String, Object>) existing, (Map<String, Object>) value);
      } else if (existing == null && !map1.containsKey(key)) {
        map1.put(key, value);
      }
    }
  }

  /**
   * Copies {@code map} into a new map in its iteration order, with each key turned into its text
   * ({@code String.valueOf}) and each value replaced by what {@code values} returns for it.
   *
   * @param map the map to copy
   * @param values what each value is replaced with
   * @param <V> value type of the copy
   * @return a new, mutable map
   * @since 3.1.0
   */
  public static <V> Map<String, V> copyWithStringKeys(
      Map<?, ?> map, Function<@Nullable Object, V> values) {
    Map<String, V> copy = LinkedHashMap.newLinkedHashMap(map.size());
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      copy.put(String.valueOf(entry.getKey()), values.apply(entry.getValue()));
    }
    return copy;
  }

  /**
   * Copies {@code map} and every map in it, at any depth, with each key turned into its text. Other
   * values, lists included, are not copied.
   *
   * @param map the map to copy
   * @return a new, mutable map
   * @since 3.1.0
   */
  public static Map<String, Object> deepCopyWithStringKeys(Map<?, ?> map) {
    return copyWithStringKeys(
        map, value -> value instanceof Map<?, ?> inner ? deepCopyWithStringKeys(inner) : value);
  }

  /**
   * Copies {@code map} and every map and list in it, at any depth, into maps and lists that can't
   * be modified, in their iteration order. Every other value, {@code null} included, is replaced by
   * what {@code values} returns for it, which can throw to reject it.
   *
   * <p>Unlike {@link #deepCopyWithStringKeys(Map)}, keys are not turned into text: a map with a key
   * that is not a {@code String} is rejected.
   *
   * @param map the map to copy
   * @param values what each value that is neither a map nor a list is replaced with
   * @return a new map that can't be modified, nor can the maps and lists in it
   * @throws IllegalArgumentException if a map has a key that is not a {@code String}
   * @since 3.1.1
   */
  public static Map<String, Object> unmodifiableDeepCopy(
      Map<?, ?> map, Function<@Nullable Object, @Nullable Object> values) {
    Map<String, @Nullable Object> copy = LinkedHashMap.newLinkedHashMap(map.size());
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        throw new IllegalArgumentException("Not a String key: " + entry.getKey());
      }
      copy.put(key, unmodifiableDeepCopyOf(entry.getValue(), values));
    }
    return Collections.unmodifiableMap(copy);
  }

  private static @Nullable Object unmodifiableDeepCopyOf(
      @Nullable Object value, Function<@Nullable Object, @Nullable Object> values) {
    if (value instanceof Map<?, ?> map) {
      return unmodifiableDeepCopy(map, values);
    }
    if (value instanceof List<?> list) {
      List<@Nullable Object> copy = new ArrayList<>(list.size());
      for (Object element : list) {
        copy.add(unmodifiableDeepCopyOf(element, values));
      }
      return Collections.unmodifiableList(copy);
    }
    return values.apply(value);
  }

  /**
   * Returns {@code map} itself when all of its keys are strings, and otherwise a copy with each key
   * turned into its text. The values are not copied.
   *
   * @param map the map
   * @return {@code map} or a new map
   * @since 3.1.0
   */
  public static Map<String, Object> withStringKeys(Map<?, ?> map) {
    for (Object key : map.keySet()) {
      if (!(key instanceof String)) {
        return copyWithStringKeys(map, value -> value);
      }
    }
    return (Map<String, Object>) map;
  }

  /**
   * Returns the dotted paths of the parents of the keys whose value is not empty: {@code a} and
   * {@code a.b} for the key {@code a.b.c}. Used to tell which maps hold anything of {@code map},
   * e.g. which sections hold a commented option.
   *
   * @param map values by dotted path
   * @return the parent paths, in no particular order
   * @since 3.1.0
   */
  public static Set<String> parentPaths(Map<String, ? extends Collection<?>> map) {
    Set<String> parents = new HashSet<>();
    for (Map.Entry<String, ? extends Collection<?>> entry : map.entrySet()) {
      if (entry.getValue().isEmpty()) {
        continue;
      }
      String key = entry.getKey();
      for (int dot = key.indexOf('.'); dot >= 0; dot = key.indexOf('.', dot + 1)) {
        parents.add(key.substring(0, dot));
      }
    }
    return parents;
  }
}
