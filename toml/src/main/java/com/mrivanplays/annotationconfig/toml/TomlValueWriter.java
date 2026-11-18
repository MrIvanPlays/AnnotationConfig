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

package com.mrivanplays.annotationconfig.toml;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
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
      Settings settings)
      throws IOException {
    TomlMapper tomlMapper = settings.get(TomlConfig.MAPPER_KEY).orElse(defaultMapper);
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
