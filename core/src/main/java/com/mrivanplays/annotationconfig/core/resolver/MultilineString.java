package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Represents a {@link String} value, which is annotated with {@link Multiline}, marking that it
 * should be dumped as a multiline string, if the configuration type used supports them, otherwise
 * should be dumped as a regular {@code String}.
 *
 * <p>If a {@link Field} is annotated with {@code Multiline}, then this object will be put into the
 * map which a {@link ValueWriter} would receive. Example of handling this:
 *
 * <pre>{@code
 * Object value = // ...
 * if (value instanceof MultilineString) {
 *   MultilineString multiline = (MultilineString) value;
 *   String toWrite = multiline.getString();
 *   char c = multiline.getMarkerChar();
 *
 *   // do writer logic
 * }
 *
 * }</pre>
 *
 * @since 2.1.0
 * @author MrIvanPlays
 * @see Multiline
 */
public final class MultilineString {

  private final String value;
  private final char c;

  public MultilineString(String value, char c) {
    this.value = Objects.requireNonNull(value, "value");
    this.c = c;
  }

  /**
   * Get the {@code String} to write.
   *
   * @return to write
   */
  public String getString() {
    return value;
  }

  /**
   * Returns the {@link Character} marker.
   *
   * <p>A marker character could be the "pipe" or '|', could be the double quote or whatever {@code
   * char} the configuration type this is going to be dumped to is supported.
   *
   * <p>In YAML's scenario, the '|' or the '>' characters are used before the multiline string is
   * specified, which makes them "marker" characters e.g.
   *
   * <pre>
   * foo: |
   *   Lorem ipsum\n
   *   dolor sit amet
   * </pre>
   *
   * Or
   *
   * <pre>
   * foo: >
   *   Lorem ipsum\n
   *   dolor sit amet
   * </pre>
   *
   * <p>As shown by the examples above, this character in YAML's scenario could return '|' or '>' or
   * the double quotes.
   *
   * @return marker character
   */
  public char getMarkerChar() {
    return c;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MultilineString that = (MultilineString) o;

    if (c != that.getMarkerChar()) {
      return false;
    }
    return value.equals(that.getString());
  }

  @Override
  public int hashCode() {
    int result = value.hashCode();
    result = 31 * result + (int) c;
    return result;
  }

  @Override
  public String toString() {
    return "MultilineString{string='" + value + '\'' + ", markerChar=" + c + '}';
  }
}
