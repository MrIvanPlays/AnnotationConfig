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

import com.mrivanplays.annotationconfig.core.internal.LinesHelper;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.utils.MapUtils;
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
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
@NullMarked
public final class YamlHandler implements ValueReader, ValueWriter {

  /** Used when no instance is configured. Created on first use, then reused for every write. */
  @Nullable private Yaml defaultYaml;

  @Override
  public Map<String, Object> read(Reader reader, Settings settings) {
    Yaml yaml = settings.get(YamlConfig.YAML_INSTANCE).orElseGet(this::defaultYaml);
    synchronized (yaml) {
      //noinspection unchecked
      return Objects.requireNonNullElse(
          yaml.loadAs(reader, LinkedHashMap.class), Collections.emptyMap());
    }
  }

  @Override
  public void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      Settings settings) {
    Yaml configured = settings.get(YamlConfig.YAML_INSTANCE).orElse(null);
    // numbers and booleans are only built by hand for the default instance, a configured one may
    // represent them differently
    boolean defaultInstance = configured == null;
    Yaml yaml = defaultInstance ? defaultYaml() : configured;
    // SnakeYAML emits token by token; collecting the output and writing it once avoids thousands
    // of small writes through the PrintWriter, each of which takes its lock
    StringBuilderWriter output = new StringBuilderWriter();
    // A YAML instance is not thread safe (its representer keeps state while converting values),
    // and it is reused between writes, so only one thread may use it at a time.
    synchronized (yaml) {
      // without comments there is nothing to look up, so no dotted paths are built
      String rootPath = fieldComments.isEmpty() ? null : "";
      // options without comments have an empty list, which MapUtils#parentPaths skips
      MappingNode root =
          new TreeBuilder(yaml, fieldComments, MapUtils.parentPaths(fieldComments), defaultInstance)
              .mapping(values, rootPath, true);
      if (root.getValue().isEmpty()) {
        return;
      }
      // uses the instance's own options and resolver
      yaml.serialize(root, output);
    }
    writer.append(output.builder);
  }

  private synchronized Yaml defaultYaml() {
    Yaml yaml = this.defaultYaml;
    if (yaml == null) {
      DumperOptions options = new DumperOptions();
      // required, otherwise comments are silently dropped
      options.setProcessComments(true);
      options.setDefaultFlowStyle(FlowStyle.BLOCK);
      options.setIndent(2);
      // list items are indented under their key
      options.setIndicatorIndent(2);
      options.setIndentWithIndicator(true);
      // don't wrap long strings
      options.setSplitLines(false);
      yaml = new Yaml(options);
      this.defaultYaml = yaml;
    }
    return yaml;
  }

  /**
   * Converts plain Java values into a SnakeYAML node tree. Created once per write call.
   *
   * @param sections the dotted paths of the maps that hold a commented key, at any depth ({@code a}
   *     and {@code a.b} for {@code a.b.c}): in other maps the dotted paths of the keys aren't
   *     built, as nothing is found under them
   * @param fastScalars whether numbers and booleans may be built directly instead of through {@link
   *     Yaml#represent(Object)}, which is only correct for the default instance
   */
  private record TreeBuilder(
      Yaml yaml, Map<String, List<String>> comments, Set<String> sections, boolean fastScalars) {

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

    private static ScalarNode multiline(MultilineString multiline) {
      String text = multiline.getString();
      char marker = multiline.getMarkerChar();
      ScalarStyle style;
      if (text.indexOf('\n') < 0) {
        // single line: nothing to fold or keep
        style = ScalarStyle.DOUBLE_QUOTED;
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

    private static List<CommentLine> commentLines(@Nullable List<String> found) {
      if (found == null || found.isEmpty()) {
        return List.of();
      }
      List<CommentLine> lines = new ArrayList<>(found.size());
      for (String comment : found) {
        // one '#' per line
        for (String line : LinesHelper.split(comment)) {
          lines.add(
              new CommentLine(null, null, line.isEmpty() ? "" : " " + line, CommentType.BLOCK));
        }
      }
      return lines;
    }

    /**
     * @param path dotted path of this map ("" for the root), or null to write no comments below it
     * @param topLevel true for the root map: a blank line is added between its entries
     */
    MappingNode mapping(Map<?, ?> map, @Nullable String path, boolean topLevel) {
      // No commented key below this map: no dotted paths are built. A key that holds a map is
      // still looked up by its plain key, which commentLines falls back to, so the path given to
      // the maps below stays one that isn't in sections.
      boolean pruned = path != null && !path.isEmpty() && !sections.contains(path);
      List<NodeTuple> tuples = new ArrayList<>(map.size());
      boolean first = true;
      for (Entry<?, ?> entry : map.entrySet()) {
        Object entryValue = entry.getValue();
        if (entryValue == null) {
          // YAML has no "absent" value, so null entries are skipped
          continue;
        }
        String key = String.valueOf(entry.getKey());
        String childPath = null;
        if (path != null && !pruned) {
          childPath = path.isEmpty() ? key : path + "." + key;
        }
        Node value = node(entryValue, pruned ? path : childPath);

        // most keys have neither a blank line nor comments, so the list is only made when needed
        List<CommentLine> lines = null;
        if (topLevel && !first) {
          lines = new ArrayList<>();
          lines.add(new CommentLine(null, null, "", CommentType.BLANK_LINE));
        }
        List<CommentLine> found = List.of();
        if (childPath != null) {
          found = commentLines(childPath, key, value instanceof MappingNode);
        } else if (pruned && value instanceof MappingNode) {
          found = commentLines(comments.get(key));
        }
        if (!found.isEmpty()) {
          if (lines == null) {
            lines = new ArrayList<>(found.size());
          }
          lines.addAll(found);
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

    private Node node(Object value, @Nullable String path) {
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
      // primitive and object arrays, written as [a, b, c]
      if (value.getClass().isArray()) {
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

    /** List and array elements: no comments, and null is written as an explicit null. */
    private Node item(@Nullable Object value) {
      if (value == null) {
        return new ScalarNode(Tag.NULL, "null", null, null, ScalarStyle.PLAIN);
      }
      return node(value, null);
    }

    private List<CommentLine> commentLines(String path, String key, boolean isMap) {
      List<String> found = comments.get(path);
      if ((found == null || found.isEmpty()) && isMap && !path.equals(key)) {
        found = comments.get(key);
      }
      return commentLines(found);
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
    public void write(char[] buffer, int offset, int length) {
      builder.append(buffer, offset, length);
    }

    @Override
    public void write(String string) {
      builder.append(string);
    }

    @Override
    public void write(String string, int offset, int length) {
      builder.append(string, offset, offset + length);
    }

    @Override
    public Writer append(@Nullable CharSequence sequence) {
      builder.append(sequence);
      return this;
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}
  }
}
