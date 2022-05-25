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

/**
 * Represents a writable holder. <br>
 * A writable subject can be a {@link File}, {@link Path}, {@link Writer} and {@link OutputStream}
 *
 * @author MrIvanPlays
 * @since 3.0.0
 */
public interface WritableObject {

  /**
   * Creates a new {@link WritableObject} from a {@link File}
   *
   * @param file writable object file
   * @return new writable object
   */
  static WritableObject createFromFile(File file) {
    return () -> {
      try {
        if (!file.exists()) {
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
    return () -> {
      try {
        if (!Files.exists(path)) {
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
    return () -> writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);
  }

  /**
   * Creates a new {@link WritableObject} from a {@link OutputStream}
   *
   * @param os writable object output stream
   * @return new writable object
   */
  static WritableObject createFromOutputStream(OutputStream os) {
    return () -> new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
  }

  /**
   * Returns a {@link PrintWriter} object, which suits AnnotationConfig's needs.
   *
   * @return writer
   */
  PrintWriter writer();
}
