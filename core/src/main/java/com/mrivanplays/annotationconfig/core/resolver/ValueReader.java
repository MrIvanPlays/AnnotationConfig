package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

/**
 * Represents a reader of values from a {@link Reader} to a {@link Map} of primitive options (using
 * {@link String} as a key). When it is implemented, it should implement 1 of the 2 methods,
 * otherwise a {@link IllegalArgumentException} is thrown if none of the methods is implemented.
 *
 * @author MrIvanPlays
 * @since 2.0.0
 */
@FunctionalInterface
public interface ValueReader {

  /**
   * Should read the specified reader to a Map. Can use the specified {@link Settings} to manipulate
   * the output of this method, or the ways the reader is parsed to the needed output.
   *
   * @param reader the reader we need read
   * @param settings the settings
   * @return the values read, represented as a map, or empty map if no values have been read
   * @throws IOException if an io occurs
   */
  Map<String, Object> read(Reader reader, Settings settings) throws IOException;
}
