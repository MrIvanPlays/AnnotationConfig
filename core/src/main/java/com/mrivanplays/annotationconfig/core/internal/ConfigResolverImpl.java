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

import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.WritableObject;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.settings.Setting;
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
@SuppressWarnings("ResultOfMethodCallIgnored")
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
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, passthrough);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, passthrough);
    KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, passthrough);
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(
            annotatedConfig, reverseFields, findParentFields);
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        resolvedAnnotations,
        file,
        passthrough,
        commentPrefix,
        valueWriter,
        keyResolver,
        reverseFields,
        findParentFields);
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
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, passthrough);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, passthrough);
    KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, passthrough);
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        AnnotatedConfigResolver.resolveAnnotations(
            annotatedConfig, reverseFields, findParentFields),
        path,
        passthrough,
        commentPrefix,
        valueWriter,
        keyResolver,
        reverseFields,
        findParentFields);
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
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, passthrough);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, passthrough);
    KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, passthrough);
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(
            annotatedConfig, reverseFields, findParentFields);
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        resolvedAnnotations,
        writer,
        passthrough,
        commentPrefix,
        valueWriter,
        keyResolver,
        reverseFields,
        findParentFields);
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
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, settings);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(
            annotatedConfig, reverseFields, findParentFields);
    handleFileLoad(annotatedConfig, resolvedAnnotations, file, settings, findParentFields);
  }

  @Override
  public void load(Object annotatedConfig, Path path, Settings settings) {
    if (Files.notExists(path) || Files.isDirectory(path)) {
      return;
    }
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, settings);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(
            annotatedConfig, reverseFields, findParentFields);
    handlePathLoad(annotatedConfig, resolvedAnnotations, path, settings, findParentFields);
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
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, settings);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
    KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, settings);
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(
            annotatedConfig, reverseFields, findParentFields);
    NullReadHandleOption nullReadHandler =
        this.getSetting(ACDefaultSettings.NULL_READ_HANDLER, settings);
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        resolvedAnnotations,
        nullReadHandler,
        settings,
        keyResolver,
        reverseFields,
        findParentFields);
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
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, settings);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(
            annotatedConfig, reverseFields, findParentFields);
    Map<String, Object> values;
    try {
      try (reader) {
        values = valueReader.read(reader, settings);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    NullReadHandleOption nullReadHandler =
        this.getSetting(ACDefaultSettings.NULL_READ_HANDLER, settings);
    KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, settings);
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        resolvedAnnotations,
        nullReadHandler,
        settings,
        keyResolver,
        reverseFields,
        findParentFields);
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
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, settings);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(
            annotatedConfig, reverseFields, findParentFields);
    if (!file.exists()) {
      KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, settings);
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          resolvedAnnotations,
          file,
          settings,
          commentPrefix,
          valueWriter,
          keyResolver,
          reverseFields,
          findParentFields);
      return;
    }
    handleFileLoad(annotatedConfig, resolvedAnnotations, file, settings, findParentFields);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, Path path, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(settings, "settings");
    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("Cannot write a config FILE to a DIRECTORY " + path);
    }
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, settings);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(
            annotatedConfig, reverseFields, findParentFields);
    if (Files.notExists(path)) {
      KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, settings);
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          resolvedAnnotations,
          path,
          settings,
          commentPrefix,
          valueWriter,
          keyResolver,
          reverseFields,
          findParentFields);
      return;
    }
    handlePathLoad(annotatedConfig, resolvedAnnotations, path, settings, findParentFields);
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
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, settings);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
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
      Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations = null;
      for (File file : files) {
        if (file.isDirectory()) {
          continue;
        }
        T config = configToResolveTo.get();
        if (resolvedAnnotations == null) {
          resolvedAnnotations =
              AnnotatedConfigResolver.resolveAnnotations(config, reverseFields, findParentFields);
        }
        handleFileLoad(config, resolvedAnnotations, file, settings, findParentFields);
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
    boolean findParentFields = this.getSetting(ACDefaultSettings.FIND_PARENT_FIELDS, settings);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
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
        Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations = null;
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
          if (resolvedAnnotations == null) {
            resolvedAnnotations =
                AnnotatedConfigResolver.resolveAnnotations(config, reverseFields, findParentFields);
          }
          handlePathLoad(config, resolvedAnnotations, path, settings, findParentFields);
          ret.put(fileName, config);
        }
        return ret;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void handleFileLoad(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations,
      File file,
      Settings settings,
      boolean findParentFields) {
    Map<String, Object> values;
    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      values = valueReader.read(reader, settings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
    KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, settings);
    finishLoad(
        annotatedConfig,
        resolvedAnnotations,
        values,
        settings,
        () -> {
          file.delete();
          AnnotatedConfigResolver.dump(
              annotatedConfig,
              resolvedAnnotations,
              file,
              settings,
              commentPrefix,
              valueWriter,
              keyResolver,
              reverseFields,
              findParentFields);
        },
        findParentFields);
  }

  private void handlePathLoad(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations,
      Path path,
      Settings settings,
      boolean findParentFields) {
    Map<String, Object> values;
    try (Reader reader =
        new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      values = valueReader.read(reader, settings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
    KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, settings);
    this.finishLoad(
        annotatedConfig,
        resolvedAnnotations,
        values,
        settings,
        () -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          AnnotatedConfigResolver.dump(
              annotatedConfig,
              resolvedAnnotations,
              path,
              settings,
              commentPrefix,
              valueWriter,
              keyResolver,
              reverseFields,
              findParentFields);
        },
        findParentFields);
  }

  private void finishLoad(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations,
      Map<String, Object> values,
      Settings settings,
      Runnable missingOptionsAction,
      boolean findParentFields) {
    if (values.isEmpty()) {
      return;
    }
    NullReadHandleOption nullReadHandler =
        this.getSetting(ACDefaultSettings.NULL_READ_HANDLER, settings);
    boolean generateNewOptions = this.getSetting(ACDefaultSettings.GENERATE_NEW_OPTIONS, settings);
    boolean reverseFields = this.getSetting(ACDefaultSettings.SHOULD_REVERSE_FIELDS, settings);
    KeyResolver keyResolver = this.getSetting(ACDefaultSettings.KEY_RESOLVER, settings);
    boolean missingOptions =
        AnnotatedConfigResolver.setFields(
            annotatedConfig,
            values,
            resolvedAnnotations,
            nullReadHandler,
            settings,
            keyResolver,
            reverseFields,
            findParentFields);
    if (missingOptions && generateNewOptions) {
      missingOptionsAction.run();
    }
  }

  private <T> T getSetting(Setting<T> setting, Settings fromMethod) {
    //noinspection OptionalGetWithoutIsPresent
    return fromMethod
        .get(setting)
        .orElse(
            this.settings.get(setting).orElse(ACDefaultSettings.getDefault().get(setting).get()));
  }
}
