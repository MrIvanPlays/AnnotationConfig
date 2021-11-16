package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSettings;
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
public interface ValueReader {

  /**
   * Should read the specified reader to a Map.
   *
   * @param reader the reader we need read
   * @return the values read, represented as a map, or empty map if no values have been read
   * @throws IOException if an io occurs
   */
  default Map<String, Object> read(Reader reader) throws IOException {
    throw new IllegalArgumentException("ValueReader not implemented");
  }

  /**
   * Should read the specified reader to a Map. Can use the specified {@link CustomOptions} to
   * manipulate the output of this method, or the ways the reader is parsed to the needed output.
   *
   * @param reader the reader we need read
   * @param customOptions the read options
   * @return the values read, represented as a map, or empty map if no values have been read
   * @throws IOException if an io occurs
   */
  default Map<String, Object> read(Reader reader, CustomOptions customOptions) throws IOException {
    return read(reader);
  }

  /**
   * Should read the specified reader to a Map. Can use the specified {@link CustomOptions} and
   * {@link LoadSettings} to manipulate the output of this method, or the ways the reader is parsed
   * to the needed output.
   *
   * @param reader the reader we need read
   * @param customOptions the read options
   * @param loadSettings the load settings
   * @return the values read, represented as a map, or empty map if no values have been read
   * @throws IOException if an io occurs
   */
  default Map<String, Object> read(
      Reader reader, CustomOptions customOptions, LoadSettings loadSettings) throws IOException {
    return read(reader, customOptions);
  }
}
