package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Represents a reader of values from a {@link File} to a {@link Map} of primitive options (using
 * {@link String} as a key). When it is implemented, it should implement 1 of the 2 methods,
 * otherwise a {@link IllegalArgumentException} is thrown if none of the methods is implemented.
 *
 * @author MrIvanPlays
 * @since 2.0.0
 */
public interface ValueReader {

  /**
   * Should read the specified file to a Map.
   *
   * @param file the file we need read
   * @return the values read, represented as a map, or empty map if no values have been read
   * @throws IOException if an io occurs
   */
  default Map<String, Object> read(File file) throws IOException {
    throw new IllegalArgumentException("ValueReader not implemented");
  }

  /**
   * Should read the specified file to a Map. Can use the specified custom options to manipulate the
   * output of this method, or the ways the file is parsed to the needed output.
   *
   * @param file the file we need read
   * @param customOptions the read options
   * @return the values read, represented as a map, or empty map if no values have been read
   * @throws IOException if an io occurs
   */
  default Map<String, Object> read(File file, CustomOptions customOptions) throws IOException {
    return read(file);
  }
}
