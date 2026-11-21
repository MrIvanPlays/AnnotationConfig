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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a writable holder. <br>
 * A writable subject can be a {@link File}, {@link Path}, {@link Writer} and {@link OutputStream}
 *
 * @author Ivan Pekov
 * @since 3.0.0
 */
@NullMarked
public interface WritableObject {

  /**
   * Creates a new {@link WritableObject} from a {@link File}
   *
   * @param file writable object file
   * @return new writable object
   */
  static WritableObject createFromFile(File file) {
    Objects.requireNonNull(file, "file");
    return () -> {
      try {
        if (!file.exists()) {
          //noinspection ResultOfMethodCallIgnored
          file.createNewFile();
        }
        return new PrintWriter(
            new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  /**
   * Creates a new {@link WritableObject} from a {@link Path}
   *
   * @param path writable object path
   * @return new writable object
   */
  static WritableObject createFromPath(Path path) {
    Objects.requireNonNull(path, "path");
    return () -> {
      try {
        if (Files.notExists(path)) {
          Files.createFile(path);
        }
        return new PrintWriter(Files.newBufferedWriter(path));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  /**
   * Creates a new {@link WritableObject} from a {@link Writer}
   *
   * @param writer writable object writer
   * @return new writable object
   */
  static WritableObject createFromWriter(Writer writer) {
    Objects.requireNonNull(writer, "writer");
    return () -> writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);
  }

  /**
   * Creates a new {@link WritableObject} from a {@link OutputStream}
   *
   * @param os writable object output stream
   * @return new writable object
   */
  static WritableObject createFromOutputStream(OutputStream os) {
    Objects.requireNonNull(os, "os");
    return () -> new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
  }

  /**
   * Returns a {@link PrintWriter} object, which suits AnnotationConfig's needs.
   *
   * @return writer
   */
  PrintWriter writer();
}
