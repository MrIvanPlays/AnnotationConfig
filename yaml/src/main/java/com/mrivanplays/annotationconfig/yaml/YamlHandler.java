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

package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Writes values as YAML using SnakeYAML (2.x). The values are converted into a SnakeYAML node tree,
 * and the comments are attached to the key nodes, so SnakeYAML takes care of indentation, quoting
 * and escaping.
 *
 * <p>The Yaml instance comes from {@code settings.get(YamlConfig.YAML_INSTANCE)}. A configured
 * instance must be created with {@code DumperOptions.setProcessComments(true)}, otherwise SnakeYAML
 * drops the comments. Without a configured instance, one default instance is created on first use
 * and reused.
 *
 * <p>Comment lookup uses the dotted path of the key from the root ({@code database.pool.max}). For
 * keys that hold a map, the plain key ({@code pool}) is used as a fallback when the dotted path has
 * no comments. Comments for keys inside list elements are not written.
 *
 * @author claude, Ivan Pekov
 * @since 3.0.4
 */
public final class YamlHandler implements ValueReader, ValueWriter {

  /** Used when no instance is configured. Created on first use, then reused for every write. */
  private Yaml defaultYaml;

  @Override
  @NonNull
  public Map<String, Object> read(@NonNull Reader reader, @NonNull Settings settings) {
    Yaml yaml = settings.get(YamlConfig.YAML_INSTANCE).orElseGet(this::defaultYaml);
    synchronized (yaml) {
      //noinspection unchecked
      return Objects.requireNonNullElse(
          yaml.loadAs(reader, LinkedHashMap.class), Collections.emptyMap());
    }
  }

  @Override
  public void write(
      @NonNull Map<String, Object> values,
      @NonNull Map<String, List<String>> fieldComments,
      @NonNull PrintWriter writer,
      @NonNull Settings settings) {
    Yaml yaml = settings.get(YamlConfig.YAML_INSTANCE).orElseGet(this::defaultYaml);
    // A YAML instance is not thread safe (its representer keeps state while converting values),
    // and it is reused between writes, so only one thread may use it at a time.
    synchronized (yaml) {
      MappingNode root = new TreeBuilder(yaml, fieldComments).mapping(values, "", true);
      if (root.getValue().isEmpty()) {
        return;
      }
      yaml.serialize(root, writer); // uses the instance's own options and resolver
    }
  }

  private synchronized Yaml defaultYaml() {
    if (defaultYaml == null) {
      DumperOptions options = new DumperOptions();
      options.setProcessComments(true); // required, otherwise comments are silently dropped
      options.setDefaultFlowStyle(FlowStyle.BLOCK);
      options.setIndent(2);
      options.setIndicatorIndent(2); // list items are indented under their key
      options.setIndentWithIndicator(true);
      options.setSplitLines(false); // don't wrap long strings
      defaultYaml = new Yaml(options);
    }
    return defaultYaml;
  }

  /** Converts plain Java values into a SnakeYAML node tree. Created once per write call. */
  private record TreeBuilder(Yaml yaml, Map<String, List<String>> comments) {

    /**
     * @param path dotted path of this map ("" for the root), or null to write no comments below it
     * @param topLevel true for the root map: a blank line is added between its entries
     */
    MappingNode mapping(Map<?, ?> map, String path, boolean topLevel) {
      List<NodeTuple> tuples = new ArrayList<>();
      boolean first = true;
      for (Entry<?, ?> entry : map.entrySet()) {
        if (entry.getValue() == null) {
          continue; // YAML has no "absent" value, so null entries are skipped
        }
        String key = String.valueOf(entry.getKey());
        String childPath = path == null ? null : path.isEmpty() ? key : path + "." + key;
        Node value = node(entry.getValue(), childPath);

        List<CommentLine> lines = new ArrayList<>();
        if (topLevel && !first) {
          lines.add(new CommentLine(null, null, "", CommentType.BLANK_LINE));
        }
        if (childPath != null) {
          lines.addAll(commentLines(childPath, key, value instanceof MappingNode));
        }
        ScalarNode keyNode = new ScalarNode(Tag.STR, key, null, null, ScalarStyle.PLAIN);
        if (!lines.isEmpty()) {
          keyNode.setBlockComments(lines);
        }
        tuples.add(new NodeTuple(keyNode, value));
        first = false;
      }
      return new MappingNode(Tag.MAP, tuples, FlowStyle.BLOCK);
    }

    private Node node(Object value, String path) {
      if (value instanceof Map<?, ?> map) {
        return mapping(map, path, false);
      }
      if (value instanceof Collection<?> collection) {
        List<Node> items = new ArrayList<>(collection.size());
        for (Object item : collection) {
          items.add(item(item));
        }
        return new SequenceNode(Tag.SEQ, items, FlowStyle.BLOCK);
      }
      if (value.getClass().isArray()) { // primitive and object arrays, written as [a, b, c]
        int length = Array.getLength(value);
        List<Node> items = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
          items.add(item(Array.get(value, i)));
        }
        return new SequenceNode(Tag.SEQ, items, FlowStyle.FLOW);
      }
      if (value instanceof MultilineString multiline) {
        return multiline(multiline);
      }
      if (value instanceof String string) {
        return new ScalarNode(Tag.STR, string, null, null, ScalarStyle.DOUBLE_QUOTED);
      }
      if (value instanceof Number || value instanceof Boolean) {
        return yaml.represent(value); // correct tag and formatting (.nan, .inf, ...)
      }
      // enums and anything else: plain text, SnakeYAML quotes it if it would be ambiguous
      return new ScalarNode(Tag.STR, String.valueOf(value), null, null, ScalarStyle.PLAIN);
    }

    /** List and array elements: no comments, and null is written as an explicit null. */
    private Node item(Object value) {
      if (value == null) {
        return new ScalarNode(Tag.NULL, "null", null, null, ScalarStyle.PLAIN);
      }
      return node(value, null);
    }

    private static ScalarNode multiline(MultilineString multiline) {
      String text = multiline.getString();
      char marker = multiline.getMarkerChar();
      ScalarStyle style;
      if (text.indexOf('\n') < 0) {
        style = ScalarStyle.DOUBLE_QUOTED; // single line: nothing to fold or keep
      } else {
        style =
            switch (marker) {
              case '|' -> ScalarStyle.LITERAL;
              case '>' -> ScalarStyle.FOLDED;
              case '"' -> ScalarStyle.DOUBLE_QUOTED;
              default ->
                  throw new IllegalArgumentException(
                      "Invalid multiline string character '" + marker + "' for YAML");
            };
      }
      return new ScalarNode(Tag.STR, text, null, null, style);
    }

    private List<CommentLine> commentLines(String path, String key, boolean isMap) {
      List<String> found = comments.get(path);
      if ((found == null || found.isEmpty()) && isMap && !path.equals(key)) {
        found = comments.get(key);
      }
      if (found == null || found.isEmpty()) {
        return List.of();
      }
      List<CommentLine> lines = new ArrayList<>();
      for (String comment : found) {
        for (String line : comment.split("\\R", -1)) { // one '#' per line
          lines.add(
              new CommentLine(null, null, line.isEmpty() ? "" : " " + line, CommentType.BLOCK));
        }
      }
      return lines;
    }
  }
}
