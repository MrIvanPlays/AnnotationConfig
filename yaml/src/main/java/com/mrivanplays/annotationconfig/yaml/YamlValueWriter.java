package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the default yaml value writer. It uses homebrew writing to support comments.
 *
 * @author MrIvanPlays
 * @since 2.1.0
 */
public final class YamlValueWriter implements ValueWriter {

  @Override
  public void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      CustomOptions options) {
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      write(entry.getKey(), entry.getValue(), writer, fieldComments, 2, false);
    }
  }

  private void write(
      String key,
      Object value,
      PrintWriter writer,
      Map<String, List<String>> commentsMap,
      int childIndents,
      boolean child) {
    StringBuilder intentPrefixBuilder = new StringBuilder();
    for (int i = 0; i < childIndents; i++) {
      intentPrefixBuilder.append(" ");
    }
    String intentPrefix = intentPrefixBuilder.toString();
    if (value instanceof Map<?, ?>) {
      List<String> baseComments = commentsMap.getOrDefault(key + ".cl", Collections.emptyList());
      if (baseComments.isEmpty()) {
        baseComments = commentsMap.getOrDefault(key, Collections.emptyList());
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
        if (v instanceof List<?>) {
          writeCommentsInsideMap(key, writer, commentsMap, intentPrefix, mapKey);
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
          write(mapKey, v, writer, commentsMap, childIndents + 2, true);
        } else {
          writeCommentsInsideMap(key, writer, commentsMap, intentPrefix, mapKey);
          if (!(v instanceof String)) {
            writer.println(intentPrefix + mapKey + ": " + v);
          } else {
            writer.println(intentPrefix + mapKey + ": \"" + v + "\"");
          }
        }
      }
    } else if (value instanceof List<?>) {
      writeComments(key, writer, commentsMap);
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
      writeComments(key, writer, commentsMap);
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

  private void writeComments(
      String key, PrintWriter writer, Map<String, List<String>> commentsMap) {
    List<String> comments = commentsMap.getOrDefault(key, Collections.emptyList());
    if (comments.isEmpty()) {
      comments = commentsMap.getOrDefault(key + ".cl", Collections.emptyList());
    }
    if (!comments.isEmpty()) {
      for (String comment : comments) {
        writer.println("# " + comment);
      }
    }
  }

  private void writeCommentsInsideMap(
      String key,
      PrintWriter writer,
      Map<String, List<String>> commentsMap,
      String intentPrefix,
      String mapKey) {
    List<String> comments = commentsMap.getOrDefault(key + "." + mapKey, Collections.emptyList());
    if (comments.isEmpty()) {
      comments = commentsMap.getOrDefault(mapKey, Collections.emptyList());
      if (comments.isEmpty()) {
        comments = commentsMap.getOrDefault(key + ".cl", Collections.emptyList());
      }
    }
    if (!comments.isEmpty()) {
      for (String comment : comments) {
        writer.println(intentPrefix + "# " + comment);
      }
    }
  }
}
