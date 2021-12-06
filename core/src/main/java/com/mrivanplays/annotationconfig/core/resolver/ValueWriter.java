package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Represents a value writer.
 *
 * <p>A value writer is a writer of values for a specified configuration type.
 *
 * @since 2.0.0 but heavily modified in 2.1.0
 * @author MrIvanPlays
 */
@FunctionalInterface
public interface ValueWriter {

  /**
   * AnnotationConfig calls this method in order to write the specified {@code values} with the
   * specified {@code fieldComments} to the specified {@link PrintWriter} {@code writer}. The
   * written values or the method of writing could be altered via {@link CustomOptions} {@code
   * options}.
   *
   * @param values the values needed to be written
   * @param fieldComments the comments of the fields
   * @param writer the writer to write
   * @param options custom options
   * @throws IOException if an i/o occurs
   */
  void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      CustomOptions options)
      throws IOException;
}
