package com.mrivanplays.annotationconfig.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedList;
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

  public TomlValueWriter(TomlMapper defaultMapper) {
    this.defaultMapper = defaultMapper;
  }

  @Override
  public void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      CustomOptions options)
      throws IOException {
    TomlMapper tomlMapper = options.getAsOr(TomlConfig.MAPPER_KEY, TomlMapper.class, defaultMapper);
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      List<String> comments = getComments(entry.getKey(), fieldComments);
      if (!comments.isEmpty()) {
        for (String comment : comments) {
          writer.println("# " + comment);
        }
      }
      Object toWrite;
      if (entry.getValue() instanceof MultilineString) {
        toWrite = ((MultilineString) entry.getValue()).getString();
      } else {
        toWrite = entry.getValue();
      }
      writer.println(
          tomlMapper.writeValueAsString(Collections.singletonMap(entry.getKey(), toWrite)));
    }
  }

  private List<String> getComments(String key, Map<String, List<String>> toWriteComments) {
    List<String> ret = new LinkedList<>();
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
