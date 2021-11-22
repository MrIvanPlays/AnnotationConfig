package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Represents a value writer.
 *
 * <p>A value writer is a writer of values for a specified configuration type by storing the parts
 * and writing it all at once, after that clearing after itself.
 *
 * @since 2.0.0 but heavily modified in 2.1.0
 * @author MrIvanPlays
 */
public interface ValueWriter {

  /**
   * AnnotationConfig calls this method whenever it needs to more specially write a {@link Map}. An
   * example about this are the configuration sections in yaml. It is needed to also handle comments
   * for these. If your configuration implementation does not have such sections, you can ignore
   * implementing this method.
   *
   * <p>An example of a yaml section:
   *
   * <pre>{@code
   * # Such comments have been supported since 1.0, but:
   * foo:
   *   bar:
   *     # Such comments have not! And this method is all about it.
   *     baz: "asd"
   * }</pre>
   *
   * @param key the base key of the map. e.g. if we have a dotted key "a.b.c" this will be "a"
   * @param value the map value to handle
   * @param options custom options
   * @param comments the comments map associated. the map keys are the value keys e.g. if we have a
   *     dotted key "a.b.c" a key would be "c"
   */
  void handleMapPart(
      String key,
      Map<String, Object> value,
      CustomOptions options,
      Map<String, List<String>> comments);

  /**
   * AnnotationConfig calls this method whenever it needs to write a value of whatever object. Even
   * though there is {@link #handleMapPart(String, Map, CustomOptions, Map)}, map values may also be
   * forced to be handled here. An example of a map value to get forced from here is a config object
   * with no comments, or a config object defined such as:
   *
   * <pre>{@code
   * @Comment("This is an example annotation")
   * private Foo foo = new Foo("bar", "baz");
   *
   * public static final class Foo {
   *
   *   private String bar;
   *   private String baz;
   *   // ...
   * }
   *
   * }</pre>
   *
   * or a config object/section defined such as:
   *
   * <pre>{@code
   * @Key("bar.baz.foo")
   * private String foo = "bar";
   *
   * @Key("bar.baz.bar")
   * private String bar = "foo";
   *
   * }</pre>
   *
   * In the second case, this method will be called twice, and it's your responsibility to handle it
   * properly. Example handling of the 2nd case section/object:
   *
   * <pre>{@code
   * Map<String, Object> values = // ...
   * // skipping the check where it is a map
   * Map<String, Object> mapValue = (Map<String, Object>) value;
   * if (values.get(key) != null) {
   *   Map<String, Object> containing = (Map<String, Object>) values.get(key);
   *   if (containing.containsKey(getFirstKey(mapValue)) {
   *     // putAll logic which handles dotted key example
   *     values.replace(key);
   *   }
   * } else {
   *   values.put(key, mapValue);
   * }
   * }</pre>
   *
   * If called with a {@link List} or a primitive value, you just store it as-is.
   *
   * @param key the base key of the value
   * @param value the value
   * @param options custom options
   * @param comments comments
   */
  void handlePart(String key, Object value, CustomOptions options, List<String> comments);

  /**
   * AnnotationConfig calls this method after every value has been handled for the values to be
   * written. The handling in this method should be to write all the values with the comments as the
   * proper syntax for the implemented configuration type, and then everything stored to be cleaned.
   *
   * @param options custom options
   * @param writer the writer to write to
   * @throws IOException if an i/o occurs
   */
  void writeAndFlush(CustomOptions options, PrintWriter writer) throws IOException;
}
