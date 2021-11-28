package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the default yaml value writer. It uses homebrew writing to support comments.
 *
 * @author MrIvanPlays
 * @since 2.1.0
 */
public final class YamlValueWriter implements ValueWriter {

  private Map<String, Object> toWrite = new HashMap<>();
  private Map<String, List<String>> toWriteComments = new HashMap<>();
  private Map<String, Map<String, List<String>>> mapPartComments = new HashMap<>();

  /** {@inheritDoc} */
  @Override
  public void handleMapPart(
      String key,
      Map<String, Object> value,
      CustomOptions options,
      Map<String, List<String>> comments) {
    if (!toWrite.containsKey(key)) {
      toWrite.put(key, value);
    } else {
      throw new IllegalArgumentException("Duplicate key!");
    }
    if (!comments.isEmpty()) {
      mapPartComments.put(key, comments);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void handlePart(String key, Object value, CustomOptions options, List<String> comments) {
    if (!toWrite.containsKey(key)) {
      toWrite.put(key, value);
    } else {
      if (value instanceof Map) {
        Map<String, Object> mapValue = (Map<String, Object>) value;
        Object stored = toWrite.get(key);
        if (!(stored instanceof Map)) {
          throw new IllegalArgumentException(
              "Invalid key found (perhaps duplicate key). Check your annotated config.");
        }
        Map<String, Object> storedMap = (Map<String, Object>) stored;
        MapUtils.populateFirst(storedMap, mapValue);
        toWrite.replace(key, storedMap);
      } else {
        throw new IllegalArgumentException("Duplicate key!");
      }
    }
    if (!comments.isEmpty()) {
      if (!(value instanceof Map)) {
        toWriteComments.put(key, comments);
      } else {
        Map<String, Object> mapValue = MapUtils.getLastMap((Map<String, Object>) value);
        if (mapValue.size() != 1) {
          toWriteComments.put(key, comments);
          return;
        }
        if (!mapPartComments.containsKey(key)) {
          Map<String, List<String>> commentPart = new HashMap<>();
          commentPart.put(MapUtils.getLastKey(mapValue), comments);
          mapPartComments.put(key, commentPart);
        } else {
          Map<String, List<String>> commentPart = mapPartComments.get(key);
          commentPart.put(MapUtils.getLastKey(mapValue), comments);
          mapPartComments.replace(key, commentPart);
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void writeAndFlush(CustomOptions options, PrintWriter writer) {
    try {
      for (Map.Entry<String, Object> entry : toWrite.entrySet()) {
        write(entry.getKey(), entry.getValue(), writer, 2, false, null);
      }
    } finally {
      toWrite.clear();
      toWriteComments.clear();
      mapPartComments.clear();
    }
  }

  private void write(
      String key,
      Object value,
      PrintWriter writer,
      int childIndents,
      boolean child,
      Map<String, List<String>> childComments) {
    StringBuilder intentPrefixBuilder = new StringBuilder();
    for (int i = 0; i < childIndents; i++) {
      intentPrefixBuilder.append(" ");
    }
    String intentPrefix = intentPrefixBuilder.toString();
    if (value instanceof Map<?, ?>) {
      List<String> baseComments = toWriteComments.getOrDefault(key, Collections.emptyList());
      if (childComments == null) {
        childComments = mapPartComments.getOrDefault(key, Collections.emptyMap());
      }
      String childPrefix = child ? intentPrefix.substring(0, childIndents - 2) : "";
      if (!baseComments.isEmpty()) {
        for (String comment : baseComments) {
          writer.println(childPrefix + "# " + comment);
        }
      }
      writer.println(childPrefix + key + ":");
      for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
        String mapKey = entry.getKey();
        Object v = entry.getValue();
        List<String> comments = childComments.getOrDefault(mapKey, Collections.emptyList());
        if (!comments.isEmpty()) {
          for (String comment : comments) {
            writer.println(intentPrefix + "# " + comment);
          }
        }
        if (v instanceof List<?>) {
          List<?> vList = (List<?>) v;
          if (vList.isEmpty()) {
            writer.println(intentPrefix + mapKey + ": []");
          } else {
            writer.println(intentPrefix + mapKey + ":");
            for (Object b : vList) {
              if (!(b instanceof String)) {
                writer.println(intentPrefix + "    - " + b);
              } else {
                writer.println(intentPrefix + "    - \"" + b + "\"");
              }
            }
          }
        } else if (v instanceof Map<?, ?>) {
          write(mapKey, v, writer, childIndents + 2, true, childComments);
        } else {
          if (!(v instanceof String)) {
            writer.println(intentPrefix + mapKey + ": " + v);
          } else {
            writer.println(intentPrefix + mapKey + ": \"" + v + "\"");
          }
        }
      }
    } else if (value instanceof List<?>) {
      List<String> comments = toWriteComments.getOrDefault(key, Collections.emptyList());
      if (!comments.isEmpty()) {
        for (String comment : comments) {
          writer.println("# " + comment);
        }
      }
      List<?> valueList = (List<?>) value;
      if (valueList.isEmpty()) {
        writer.println(key + ": []");
      } else {
        writer.println(key + ":");
        for (Object b : valueList) {
          if (!(b instanceof String)) {
            writer.println(intentPrefix + "- " + b);
          } else {
            writer.println(intentPrefix + "- \"" + b + "\"");
          }
        }
      }
    } else {
      List<String> comments = toWriteComments.getOrDefault(key, Collections.emptyList());
      if (!comments.isEmpty()) {
        for (String comment : comments) {
          writer.println("# " + comment);
        }
      }
      if (!(value instanceof String)) {
        writer.println(key + ": " + value);
      } else {
        writer.println(key + ": \"" + value + "\"");
      }
    }
    if (!child) {
      writer.append('\n');
    }
  }
}
