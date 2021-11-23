package com.mrivanplays.annotationconfig.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the default toml value writer. Has a lot of stuff homebrew but the main dumping is
 * done via jackson's {@link TomlMapper}.
 *
 * @author MrIvanPlays
 * @since 2.1.0
 */
public final class TomlValueWriter implements ValueWriter {

  private final TomlMapper defaultMapper;
  private Map<String, Object> toWrite = new HashMap<>();
  private Map<String, List<String>> toWriteComments = new HashMap<>();

  public TomlValueWriter(TomlMapper defaultMapper) {
    this.defaultMapper = defaultMapper;
  }

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
      for (Map.Entry<String, List<String>> commentEntry : comments.entrySet()) {
        toWriteComments.put(key + commentEntry.getKey(), commentEntry.getValue());
      }
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
      toWriteComments.put(key, comments);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void writeAndFlush(CustomOptions options, PrintWriter writer) throws IOException {
    TomlMapper tomlMapper = options.getAsOr(TomlConfig.MAPPER_KEY, TomlMapper.class, defaultMapper);
    try {
      for (Map.Entry<String, Object> entry : toWrite.entrySet()) {
        List<String> comments = getComments(entry.getKey());
        if (!comments.isEmpty()) {
          for (String comment : comments) {
            writer.println("# " + comment);
          }
        }
        writer.println(
            tomlMapper.writeValueAsString(
                Collections.singletonMap(entry.getKey(), entry.getValue())));
      }
    } finally {
      toWrite.clear();
      toWriteComments.clear();
    }
  }

  private List<String> getComments(String key) {
    List<String> ret = new ArrayList<>();
    if (toWriteComments.containsKey(key)) {
      ret.addAll(toWriteComments.get(key));
    } else {
      for (Map.Entry<String, List<String>> entry : toWriteComments.entrySet()) {
        if (entry.getKey().startsWith(key)) {
          ret.addAll(entry.getValue());
        }
      }
    }
    return ret;
  }
}
