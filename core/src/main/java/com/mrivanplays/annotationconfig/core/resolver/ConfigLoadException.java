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

package com.mrivanplays.annotationconfig.core.resolver;

import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * Thrown by a load with {@link ACDefaultSettings#REPORT_ALL_ERRORS} on when options could not be
 * read: it lists every one of them, in the order they were read. Each failure is also a suppressed
 * exception of this one.
 *
 * <p>It is an {@link IllegalArgumentException}, as the first failure is without the setting.
 *
 * <pre>{@code
 * try {
 *   resolver.load(config, path, new Settings().put(ACDefaultSettings.REPORT_ALL_ERRORS, true));
 * } catch (ConfigLoadException e) {
 *   for (ConfigLoadException.Problem problem : e.problems()) {
 *     logger.warn(problem.key() + ": " + problem.cause().getMessage());
 *   }
 * }
 * }</pre>
 *
 * @author Ivan Pekov
 * @since 3.1.1
 */
@NullMarked
public final class ConfigLoadException extends IllegalArgumentException {

  @Serial private static final long serialVersionUID = 1L;

  @SuppressWarnings("serial") // an unmodifiable list of records; never serialized in practice
  private final List<Problem> problems;

  /**
   * Creates a new exception for {@code problems}.
   *
   * @param source what was loaded, for the message, such as {@code "the configuration config.yml"}
   * @param problems the options that could not be read; at least one
   */
  public ConfigLoadException(String source, List<Problem> problems) {
    super(message(Objects.requireNonNull(source, "source"), problems));
    this.problems = List.copyOf(problems);
    for (Problem problem : this.problems) {
      this.addSuppressed(problem.cause());
    }
  }

  private static String message(String source, List<Problem> problems) {
    if (problems.isEmpty()) {
      throw new IllegalArgumentException("No problems");
    }
    StringBuilder message = new StringBuilder();
    message
        .append(problems.size())
        .append(problems.size() == 1 ? " option" : " options")
        .append(" of ")
        .append(source)
        .append(" could not be read:");
    for (Problem problem : problems) {
      message
          .append("\n- '")
          .append(problem.key())
          .append("': ")
          .append(problem.cause().getMessage());
    }
    return message.toString();
  }

  /**
   * The options that could not be read, in the order they were read.
   *
   * @return the problems; never empty, and can't be modified
   */
  public List<Problem> problems() {
    return this.problems;
  }

  /**
   * An option that could not be read.
   *
   * @param key the option's key, with the keys of the sections it is in, joined by dots ({@code
   *     server.port})
   * @param cause why it could not be read: the exception loading would have stopped at without
   *     {@link ACDefaultSettings#REPORT_ALL_ERRORS}
   * @since 3.1.1
   */
  public record Problem(String key, RuntimeException cause) {

    /**
     * Creates a new problem.
     *
     * @param key the option's key
     * @param cause why it could not be read
     */
    public Problem {
      Objects.requireNonNull(key, "key");
      Objects.requireNonNull(cause, "cause");
    }
  }
}
