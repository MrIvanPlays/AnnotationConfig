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
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;
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
 * @since 3.1.0
 */
public final class YamlHandler implements ValueReader, ValueWriter {

  private static final Pattern LINE_BREAK = Pattern.compile("\\R");

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
    Yaml yaml = settings.get(YamlConfig.YAML_INSTANCE).orElse(null);
    // numbers and booleans are only built by hand for the default instance, a configured one may
    // represent them differently
    boolean defaultInstance = yaml == null;
    if (defaultInstance) {
      yaml = defaultYaml();
    }
    // SnakeYAML emits token by token; collecting the output and writing it once avoids thousands
    // of small writes through the PrintWriter, each of which takes its lock
    StringBuilderWriter output = new StringBuilderWriter();
    // A YAML instance is not thread safe (its representer keeps state while converting values),
    // and it is reused between writes, so only one thread may use it at a time.
    synchronized (yaml) {
      // without comments there is nothing to look up, so no dotted paths are built
      String rootPath = fieldComments.isEmpty() ? null : "";
      MappingNode root =
          new TreeBuilder(yaml, fieldComments, defaultInstance).mapping(values, rootPath, true);
      if (root.getValue().isEmpty()) {
        return;
      }
      yaml.serialize(root, output); // uses the instance's own options and resolver
    }
    writer.append(output.builder);
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

  /**
   * Converts plain Java values into a SnakeYAML node tree. Created once per write call.
   *
   * @param fastScalars whether numbers and booleans may be built directly instead of through {@link
   *     Yaml#represent(Object)}, which is only correct for the default instance
   */
  private record TreeBuilder(Yaml yaml, Map<String, List<String>> comments, boolean fastScalars) {

    /**
     * @param path dotted path of this map ("" for the root), or null to write no comments below it
     * @param topLevel true for the root map: a blank line is added between its entries
     */
    MappingNode mapping(Map<?, ?> map, String path, boolean topLevel) {
      List<NodeTuple> tuples = new ArrayList<>(map.size());
      boolean first = true;
      for (Entry<?, ?> entry : map.entrySet()) {
        if (entry.getValue() == null) {
          continue; // YAML has no "absent" value, so null entries are skipped
        }
        String key = String.valueOf(entry.getKey());
        String childPath = path == null ? null : path.isEmpty() ? key : path + "." + key;
        Node value = node(entry.getValue(), childPath);

        // most keys have neither a blank line nor comments, so the list is only made when needed
        List<CommentLine> lines = null;
        if (topLevel && !first) {
          lines = new ArrayList<>();
          lines.add(new CommentLine(null, null, "", CommentType.BLANK_LINE));
        }
        if (childPath != null) {
          List<CommentLine> found = commentLines(childPath, key, value instanceof MappingNode);
          if (!found.isEmpty()) {
            if (lines == null) {
              lines = new ArrayList<>(found.size());
            }
            lines.addAll(found);
          }
        }
        ScalarNode keyNode = new ScalarNode(Tag.STR, key, null, null, ScalarStyle.PLAIN);
        if (lines != null) {
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
        // correct tag and formatting (.nan, .inf, ...)
        return fastScalars ? scalar(value) : yaml.represent(value);
      }
      // enums and anything else: plain text, SnakeYAML quotes it if it would be ambiguous
      return new ScalarNode(Tag.STR, String.valueOf(value), null, null, ScalarStyle.PLAIN);
    }

    /**
     * The node the default instance's representer would create for a number or boolean (see
     * SnakeYAML's {@code SafeRepresenter.RepresentNumber} and {@code RepresentBoolean}), without
     * its per-call bookkeeping: {@link Yaml#represent(Object)} clears an identity map every time.
     */
    private static ScalarNode scalar(Object value) {
      if (value instanceof Boolean bool) {
        return new ScalarNode(Tag.BOOL, bool ? "true" : "false", null, null, ScalarStyle.PLAIN);
      }
      if (value instanceof Byte
          || value instanceof Short
          || value instanceof Integer
          || value instanceof Long
          || value instanceof BigInteger) {
        return new ScalarNode(Tag.INT, value.toString(), null, null, ScalarStyle.PLAIN);
      }
      Number number = (Number) value;
      String text;
      // the same checks as SnakeYAML: a Float NaN is not equal to Double.NaN, so it is written as
      // Float.toString writes it
      if (number.equals(Double.NaN)) {
        text = ".NaN";
      } else if (number.equals(Double.POSITIVE_INFINITY)) {
        text = ".inf";
      } else if (number.equals(Double.NEGATIVE_INFINITY)) {
        text = "-.inf";
      } else {
        text = number.toString();
      }
      return new ScalarNode(Tag.FLOAT, text, null, null, ScalarStyle.PLAIN);
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
        for (String line : LINE_BREAK.split(comment, -1)) { // one '#' per line
          lines.add(
              new CommentLine(null, null, line.isEmpty() ? "" : " " + line, CommentType.BLOCK));
        }
      }
      return lines;
    }
  }

  /** A {@link Writer} into a {@link StringBuilder}, without the locking of a StringWriter. */
  private static final class StringBuilderWriter extends Writer {

    private final StringBuilder builder = new StringBuilder(1024);

    @Override
    public void write(int c) {
      builder.append((char) c);
    }

    @Override
    public void write(char @NonNull [] buffer, int offset, int length) {
      builder.append(buffer, offset, length);
    }

    @Override
    public void write(@NonNull String string) {
      builder.append(string);
    }

    @Override
    public void write(@NonNull String string, int offset, int length) {
      builder.append(string, offset, offset + length);
    }

    @Override
    public Writer append(CharSequence sequence) {
      builder.append(sequence);
      return this;
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}
  }
}
