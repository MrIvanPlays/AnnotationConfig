package com.mrivanplays.annotationconfig.core;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents a writer of field values for specific config type. Bellow everything about the input
 * of the write method is explained.
 *
 * <p>The key is the identifier of the value e.g. {@code key=value} ( syntax of a .properties
 * configuration )
 *
 * <p>The values inputted can be any of the following:
 *
 * <ul>
 *   <li>{@link java.util.Map} of String as a key and Object as a value
 *   <li>{@link java.util.List} of whatever Object
 *   <li>All of java's primitive values (string, int, boolean, byte, etc.)
 * </ul>
 *
 * The method has to handle the inputted value's type by its own and then write it to the {@link
 * PrintWriter} {@code writer} with the appropriate syntax for the specific config type.
 *
 * <p>The boolean value {@code sectionExists} represents if the method has to write specially about
 * an object, annotated with {@link com.mrivanplays.annotationconfig.core.annotations.ConfigObject}.
 * For an example, this is a configuration section in YAML:
 *
 * <pre>
 *   messages:
 *     no-permission: "You don't have permission to perform this."
 *     information-process: "We're processing the information. Please wait...."
 * </pre>
 *
 * In this situation, {@code sectionExists} will tell you if you are writing {@code no-permission}
 * under {@code messages}, or just as a regular value.
 *
 * <p>In the end we want this to generate clean writes and configs.
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
@FunctionalInterface
public interface ValueWriter {
  void write(String key, Object value, PrintWriter writer, boolean sectionExists)
      throws IOException;
}
