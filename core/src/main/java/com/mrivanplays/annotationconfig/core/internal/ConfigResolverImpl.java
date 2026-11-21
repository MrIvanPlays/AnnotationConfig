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

package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.WritableObject;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @hidden
 */
@NullMarked
public final class ConfigResolverImpl implements ConfigResolver {

  private final String commentPrefix;
  private final ValueWriter valueWriter;
  private final ValueReader valueReader;
  private final Settings settings;
  private final List<String> fileExtensions;

  public ConfigResolverImpl(
      String commentPrefix,
      ValueWriter valueWriter,
      ValueReader valueReader,
      @Nullable Settings settings,
      List<String> fileExtensions) {
    this.commentPrefix = commentPrefix;
    this.valueWriter = Objects.requireNonNull(valueWriter, "valueWriter");
    this.valueReader = Objects.requireNonNull(valueReader, "valueReader");
    this.fileExtensions = Objects.requireNonNull(fileExtensions, "fileExtensions");
    if (this.fileExtensions.isEmpty()) {
      throw new IllegalArgumentException("No file extensions specified.");
    }
    this.settings = Objects.requireNonNullElse(settings, ACDefaultSettings.getDefault().copy());
  }

  @Override
  public Settings settings() {
    return this.settings;
  }

  @Override
  public void dump(Object annotatedConfig, File file) {
    this.dump(annotatedConfig, file, this.settings);
  }

  @Override
  public void dump(Object annotatedConfig, File file, Settings passthrough) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(passthrough, "settings");
    if (file.exists()) {
      file.delete();
    }
    Set<AnnotationHolder> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, passthrough);
    AnnotatedConfigResolver.dump(
        annotatedConfig, resolvedAnnotations, file, passthrough, commentPrefix, valueWriter);
  }

  @Override
  public void dump(Object annotatedConfig, Path path) {
    this.dump(annotatedConfig, path, this.settings);
  }

  @Override
  public void dump(Object annotatedConfig, Path path, Settings passthrough) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(passthrough, "settings");
    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("Cannot dump a config FILE to a DIRECTORY: " + path);
    }
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, passthrough),
        path,
        passthrough,
        commentPrefix,
        valueWriter);
  }

  @Override
  public void dump(Object annotatedConfig, Writer writer) {
    this.dump(annotatedConfig, writer, this.settings);
  }

  @Override
  public void dump(Object annotatedConfig, Writer writer, Settings passthrough) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(passthrough, "settings");
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, passthrough),
        writer,
        passthrough,
        commentPrefix,
        valueWriter);
  }

  @Override
  public void load(Object annotatedConfig, File file) {
    load(annotatedConfig, file, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, Path path) {
    load(annotatedConfig, path, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, File file, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(settings, "settings");
    if (!file.exists()) {
      return;
    }
    handleFileLoad(
        annotatedConfig,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, settings),
        file,
        settings);
  }

  @Override
  public void load(Object annotatedConfig, Path path, Settings settings) {
    if (Files.notExists(path) || Files.isDirectory(path)) {
      return;
    }
    handlePathLoad(
        annotatedConfig,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, settings),
        path,
        settings);
  }

  @Override
  public void load(Object annotatedConfig, Map<String, Object> values) {
    load(annotatedConfig, values, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, Map<String, Object> values, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(values, "values");
    Objects.requireNonNull(settings, "settings");
    if (values.isEmpty()) {
      return;
    }
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, settings),
        settings);
  }

  @Override
  public void load(Object annotatedConfig, Reader reader) {
    load(annotatedConfig, reader, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, Reader reader, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(reader, "reader");
    Objects.requireNonNull(settings, "settings");
    Map<String, Object> values;
    try {
      try (reader) {
        values = valueReader.read(reader, settings);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, settings),
        settings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, File file) {
    loadOrDump(annotatedConfig, file, this.settings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, Path path) {
    loadOrDump(annotatedConfig, path, this.settings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, File file, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(file, "file");
    Objects.requireNonNull(settings, "settings");
    Set<AnnotationHolder> holders =
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, settings);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(
          annotatedConfig, holders, file, settings, commentPrefix, valueWriter);
      return;
    }
    handleFileLoad(annotatedConfig, holders, file, settings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, Path path, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(settings, "settings");
    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("Cannot write a config FILE to a DIRECTORY " + path);
    }
    Set<AnnotationHolder> holders =
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, settings);
    if (Files.notExists(path)) {
      AnnotatedConfigResolver.dump(
          annotatedConfig, holders, path, settings, commentPrefix, valueWriter);
      return;
    }
    handlePathLoad(annotatedConfig, holders, path, settings);
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile) {
    return resolveMultiple(dir, configToResolveTo, dumpFile, settings);
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile, Settings settings) {
    Objects.requireNonNull(dir, "dir");
    Objects.requireNonNull(configToResolveTo, "configToResolveTo");
    Objects.requireNonNull(dumpFile, "dumpFile");
    Objects.requireNonNull(settings, "settings");
    if (!dir.isDirectory()) {
      throw new IllegalArgumentException(dir + " is not a directory!");
    }
    if (!dir.exists()) {
      dir.mkdirs();
      T config = configToResolveTo.get();
      dump(config, dumpFile.writer());
      return Collections.emptyMap();
    } else {
      File[] files =
          dir.listFiles((_, name) -> {
            for (String extension : fileExtensions) {
              if (name.endsWith(extension)) {
                return true;
              }
            }
            return false;
          });
      if (files == null || files.length == 0) {
        T config = configToResolveTo.get();
        dump(config, dumpFile.writer());
        return Collections.emptyMap();
      }
      Map<String, T> ret = new LinkedHashMap<>();
      Set<AnnotationHolder> holders = null;
      for (File file : files) {
        if (file.isDirectory()) {
          continue;
        }
        T config = configToResolveTo.get();
        if (holders == null) {
          holders = AnnotatedConfigResolver.resolveAnnotationHolders(config, settings);
        }
        handleFileLoad(config, holders, file, settings);
        ret.put(file.getName(), config);
      }
      return ret;
    }
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      Path dir, Supplier<T> configToResolveTo, WritableObject dumpFile) {
    return resolveMultiple(dir, configToResolveTo, dumpFile, settings);
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      Path dir, Supplier<T> configToResolveTo, WritableObject dumpFile, Settings settings) {
    Objects.requireNonNull(dir, "dir");
    Objects.requireNonNull(configToResolveTo, "configToResolveTo");
    Objects.requireNonNull(dumpFile, "dumpFile");
    Objects.requireNonNull(settings, "settings");
    if (!Files.isDirectory(dir)) {
      throw new IllegalArgumentException(dir + " is not a directory!");
    }
    if (Files.notExists(dir)) {
      try {
        Files.createDirectories(dir);
        T config = configToResolveTo.get();
        dump(config, dumpFile.writer());
        return Collections.emptyMap();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      try (Stream<Path> paths = Files.list(dir)) {
        Iterator<Path> iterator = paths.iterator();
        if (!iterator.hasNext()) {
          T config = configToResolveTo.get();
          dump(config, dumpFile.writer());
          return Collections.emptyMap();
        }
        Set<AnnotationHolder> holders = null;
        Map<String, T> ret = new LinkedHashMap<>();
        while (iterator.hasNext()) {
          Path path = iterator.next();
          if (Files.isDirectory(path)) {
            continue;
          }
          String fileName = path.toFile().getName();
          boolean matches = false;
          for (String extension : fileExtensions) {
            if (fileName.endsWith(extension)) {
              matches = true;
              break;
            }
          }
          if (!matches) {
            continue;
          }
          T config = configToResolveTo.get();
          if (holders == null) {
            holders = AnnotatedConfigResolver.resolveAnnotationHolders(config, settings);
          }
          handlePathLoad(config, holders, path, settings);
          ret.put(fileName, config);
        }
        return ret;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void handleFileLoad(
      Object annotatedConfig, Set<AnnotationHolder> holders, File file, Settings settings) {
    Map<String, Object> values;
    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      values = valueReader.read(reader, settings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    finishLoad(
        annotatedConfig,
        holders,
        values,
        settings,
        () -> {
          file.delete();
          AnnotatedConfigResolver.dump(
              annotatedConfig, holders, file, settings, commentPrefix, valueWriter);
        });
  }

  private void handlePathLoad(
      Object annotatedConfig, Set<AnnotationHolder> holders, Path path, Settings settings) {
    Map<String, Object> values;
    try (Reader reader =
        new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      values = valueReader.read(reader, settings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.finishLoad(
        annotatedConfig,
        holders,
        values,
        settings,
        () -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          AnnotatedConfigResolver.dump(
              annotatedConfig, holders, path, settings, commentPrefix, valueWriter);
        });
  }

  private void finishLoad(
      Object annotatedConfig,
      Set<AnnotationHolder> holders,
      Map<String, Object> values,
      Settings settings,
      Runnable missingOptionsAction) {
    if (values.isEmpty()) {
      return;
    }
    boolean generateNewOptions =
        settings
            .get(ACDefaultSettings.GENERATE_NEW_OPTIONS)
            .orElse(
                this.settings
                    .get(ACDefaultSettings.GENERATE_NEW_OPTIONS)
                    .orElse(this.settings.get(ACDefaultSettings.GENERATE_NEW_OPTIONS).get()));
    boolean missingOptions =
        AnnotatedConfigResolver.setFields(annotatedConfig, values, holders, settings);
    if (missingOptions && generateNewOptions) {
      missingOptionsAction.run();
    }
  }
}
