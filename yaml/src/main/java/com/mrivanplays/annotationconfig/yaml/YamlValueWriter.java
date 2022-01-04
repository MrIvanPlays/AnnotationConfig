package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the default yaml value writer. It uses homebrew writing to support comments.
 *
 * @author MrIvanPlays
 * @since 2.1.0
 */
@SuppressWarnings("unchecked")
public final class YamlValueWriter implements ValueWriter {

  @Override
  public void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      CustomOptions options) {
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      write(entry.getKey(), entry.getValue(), writer, fieldComments, 2, false, false);
    }
  }

  private void write(
      String key,
      Object value,
      PrintWriter writer,
      Map<String, List<String>> commentsMap,
      int childIndents,
      boolean child,
      boolean additional2Spaces) {
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
      writer.println((additional2Spaces ? intentPrefix : childPrefix) + key + ":");
      for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
        String mapKey = entry.getKey();
        Object v = entry.getValue();
        if (v instanceof List<?>) {
          writeCommentsInsideMap(key, writer, commentsMap, intentPrefix, mapKey);
          List<?> vList = (List<?>) v;
          if (vList.isEmpty()) {
            writer.println(intentPrefix + mapKey + ": []");
          } else {
            boolean hasMapInsideMap = false;
            OUT:
            for (Object b : vList) {
              if (b instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) b;
                for (Map.Entry<String, Object> e1 : map.entrySet()) {
                  if (e1.getValue() instanceof Map) {
                    hasMapInsideMap = true;
                    break OUT;
                  }
                }
              }
            }
            if (!hasMapInsideMap) {
              writer.println(intentPrefix + mapKey + ":");
            }
            for (Object b : vList) {
              if (!(b instanceof String)) {
                if (b instanceof Map) {
                  Map<String, Object> map = (Map<String, Object>) b;
                  boolean firstValue = true;
                  List<Map.Entry<String, Object>> reservedObjectsForLater = new ArrayList<>();
                  for (Map.Entry<String, Object> e : map.entrySet()) {
                    if (e.getValue() instanceof Map || e.getValue() instanceof List) {
                      reservedObjectsForLater.add(e);
                      continue;
                    }
                    StringBuilder valueAppend = new StringBuilder();
                    if (e.getValue() instanceof String) {
                      valueAppend.append('"').append(e.getValue()).append('"');
                    } else {
                      valueAppend.append(e.getValue());
                    }
                    if (firstValue) {
                      writer.println(intentPrefix + "- " + e.getKey() + ": " + valueAppend);
                      firstValue = false;
                    } else {
                      writer.println(intentPrefix + "  " + e.getKey() + ": " + valueAppend);
                    }
                  }
                  for (Map.Entry<String, Object> reserved : reservedObjectsForLater) {
                    write(
                        reserved.getKey(),
                        reserved.getValue(),
                        writer,
                        commentsMap,
                        childIndents + 2,
                        true,
                        true);
                  }
                } else {
                  writer.println(intentPrefix + "    - " + b);
                }
              } else {
                writer.println(intentPrefix + "    - \"" + b + "\"");
              }
            }
          }
        } else if (v instanceof Map<?, ?>) {
          write(mapKey, v, writer, commentsMap, childIndents + 2, true, additional2Spaces);
        } else {
          writeCommentsInsideMap(key, writer, commentsMap, intentPrefix, mapKey);
          if (!(v instanceof String)) {
            if (v instanceof MultilineString) {
              MultilineString multiline = (MultilineString) v;
              String toWrite = multiline.getString();
              char c = multiline.getMarkerChar();
              if (toWrite.indexOf('\n') != -1) {
                String[] parts = toWrite.split("\n");
                if (c == '|' || c == '>') {
                  writer.println(intentPrefix + mapKey + ": " + c);
                } else if (c != '"') {
                  throw new IllegalArgumentException(
                      "Invalid multiline string character '" + c + "' for YAML");
                } else {
                  writer.println(intentPrefix + mapKey + ": \"");
                }
                for (int i = 0; i < parts.length; i++) {
                  String part = parts[i];
                  if ((i + 1) == parts.length && c == '"') {
                    writer.println(intentPrefix + part + "\"");
                  } else {
                    writer.println(intentPrefix + part + "\\n");
                  }
                }
              } else {
                writer.println(
                    intentPrefix
                        + (additional2Spaces ? "  " : "")
                        + mapKey
                        + ": \""
                        + toWrite
                        + "\"");
              }
            } else {
              writer.println(intentPrefix + (additional2Spaces ? "  " : "") + mapKey + ": " + v);
            }
          } else {
            writer.println(
                intentPrefix + (additional2Spaces ? "  " : "") + mapKey + ": \"" + v + "\"");
          }
        }
      }
    } else if (value instanceof List<?>) {
      writeComments(key, writer, commentsMap);
      List<?> valueList = (List<?>) value;
      if (valueList.isEmpty()) {
        writer.println((child ? intentPrefix : "") + key + ": []");
      } else {
        boolean hasMapInsideMap = false;
        OUT:
        for (Object b : valueList) {
          if (b instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) b;
            for (Map.Entry<String, Object> e1 : map.entrySet()) {
              if (e1.getValue() instanceof Map) {
                hasMapInsideMap = true;
                break OUT;
              }
            }
          }
        }
        if (!hasMapInsideMap) {
          writer.println((child ? intentPrefix : "") + key + ":");
        }
        for (Object b : valueList) {
          if (!(b instanceof String)) {
            if (b instanceof Map) {
              Map<String, Object> map = (Map<String, Object>) b;
              boolean firstValue = true;
              List<Map.Entry<String, Object>> reservedObjectsForLater = new ArrayList<>();
              for (Map.Entry<String, Object> e : map.entrySet()) {
                if (e.getValue() instanceof Map || e.getValue() instanceof List) {
                  reservedObjectsForLater.add(e);
                  continue;
                }
                StringBuilder valueAppend = new StringBuilder();
                if (e.getValue() instanceof String) {
                  valueAppend.append('"').append(e.getValue()).append('"');
                } else {
                  valueAppend.append(e.getValue());
                }
                if (firstValue) {
                  writer.println(intentPrefix + "- " + e.getKey() + ": " + valueAppend);
                  firstValue = false;
                } else {
                  writer.println(intentPrefix + "  " + e.getKey() + ": " + valueAppend);
                }
              }
              for (Map.Entry<String, Object> reserved : reservedObjectsForLater) {
                write(
                    reserved.getKey(),
                    reserved.getValue(),
                    writer,
                    commentsMap,
                    childIndents + 2,
                    true,
                    true);
              }
            } else {
              writer.println(intentPrefix + "- " + b);
            }
          } else {
            writer.println(intentPrefix + "- \"" + b + "\"");
          }
        }
      }
    } else {
      writeComments(key, writer, commentsMap);
      if (!(value instanceof String)) {
        if (value instanceof MultilineString) {
          MultilineString multiline = (MultilineString) value;
          String toWrite = multiline.getString();
          char c = multiline.getMarkerChar();
          if (toWrite.indexOf('\n') != -1) {
            String[] parts = toWrite.split("\n");
            if (c == '|' || c == '>') {
              writer.println((child ? intentPrefix : "") + key + ": " + c);
            } else if (c != '"') {
              throw new IllegalArgumentException(
                  "Invalid multiline string character '" + c + "' for YAML");
            } else {
              writer.println((child ? intentPrefix : "") + key + ": \"");
            }
            for (int i = 0; i < parts.length; i++) {
              String part = parts[i];
              if ((i + 1) == parts.length && c == '"') {
                writer.println((child ? intentPrefix : "") + part + "\"");
              } else {
                writer.println((child ? intentPrefix : "") + part + "\\n");
              }
            }
          } else {
            writer.println((child ? intentPrefix : "") + key + ": \"" + toWrite + "\"");
          }
        } else {
          writer.println((child ? intentPrefix : "") + key + ": " + value);
        }
      } else {
        writer.println((child ? intentPrefix : "") + key + ": \"" + value + "\"");
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
