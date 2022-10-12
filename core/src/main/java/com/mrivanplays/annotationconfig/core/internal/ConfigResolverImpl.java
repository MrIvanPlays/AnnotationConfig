package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.WritableObject;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ConfigResolverImpl implements ConfigResolver {

  private final String commentPrefix;
  private final ValueWriter valueWriter;
  private final ValueReader valueReader;
  private final Settings settings;
  private final KeyResolver keyResolver;
  private final String[] fileExtensions;
  private final boolean reverseFields;

  public ConfigResolverImpl(
      String commentPrefix,
      ValueWriter valueWriter,
      ValueReader valueReader,
      Settings settings,
      KeyResolver keyResolver,
      String[] fileExtensions,
      boolean reverseFields) {
    this.commentPrefix = Objects.requireNonNull(commentPrefix, "commentPrefix");
    this.valueWriter = Objects.requireNonNull(valueWriter, "valueWriter");
    this.valueReader = Objects.requireNonNull(valueReader, "valueReader");
    this.fileExtensions = Objects.requireNonNull(fileExtensions, "fileExtensions");
    if (settings == null) {
      this.settings = ACDefaultSettings.getDefault().copy();
    } else {
      this.settings = settings;
    }
    if (keyResolver == null) {
      this.keyResolver = KeyResolver.DEFAULT;
    } else {
      this.keyResolver = keyResolver;
    }
    this.reverseFields = reverseFields;
  }

  @Override
  public Settings settings() {
    return this.settings;
  }

  @Override
  public void dump(Object annotatedConfig, File file) {
    if (file.exists()) {
      file.delete();
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        resolvedAnnotations,
        file,
        settings,
        commentPrefix,
        valueWriter,
        keyResolver,
        reverseFields);
  }

  @Override
  public void dump(Object annotatedConfig, Path path) {
    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("Cannot dump a config FILE to a DIRECTORY: " + path);
    }
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields),
        path,
        settings,
        commentPrefix,
        valueWriter,
        keyResolver,
        reverseFields);
  }

  @Override
  public void dump(Object annotatedConfig, Writer writer) {
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        resolvedAnnotations,
        writer,
        settings,
        commentPrefix,
        valueWriter,
        keyResolver,
        reverseFields);
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
    if (!file.exists()) {
      return;
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    handleFileLoad(annotatedConfig, resolvedAnnotations, file, settings);
  }

  @Override
  public void load(Object annotatedConfig, Path path, Settings settings) {
    if (Files.notExists(path) || Files.isDirectory(path)) {
      return;
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    handlePathLoad(annotatedConfig, resolvedAnnotations, path, settings);
  }

  @Override
  public void load(Object annotatedConfig, Map<String, Object> values) {
    load(annotatedConfig, values, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, Map<String, Object> values, Settings settings) {
    if (values.isEmpty()) {
      return;
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    NullReadHandleOption nullReadHandler =
        settings
            .get(ACDefaultSettings.NULL_READ_HANDLER)
            .orElse(
                this.settings
                    .get(ACDefaultSettings.NULL_READ_HANDLER)
                    .orElse(
                        ACDefaultSettings.getDefault()
                            .get(ACDefaultSettings.NULL_READ_HANDLER)
                            .get()));
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        resolvedAnnotations,
        nullReadHandler,
        settings,
        keyResolver,
        reverseFields);
  }

  @Override
  public void load(Object annotatedConfig, Reader reader) {
    load(annotatedConfig, reader, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, Reader reader, Settings settings) {
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    Map<String, Object> values;
    try {
      try {
        values = valueReader.read(reader, settings);
      } finally {
        reader.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    NullReadHandleOption nullReadHandler =
        settings
            .get(ACDefaultSettings.NULL_READ_HANDLER)
            .orElse(
                this.settings
                    .get(ACDefaultSettings.NULL_READ_HANDLER)
                    .orElse(
                        ACDefaultSettings.getDefault()
                            .get(ACDefaultSettings.NULL_READ_HANDLER)
                            .get()));
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        resolvedAnnotations,
        nullReadHandler,
        settings,
        keyResolver,
        reverseFields);
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
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          resolvedAnnotations,
          file,
          settings,
          commentPrefix,
          valueWriter,
          keyResolver,
          reverseFields);
      return;
    }
    handleFileLoad(annotatedConfig, resolvedAnnotations, file, settings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, Path path, Settings settings) {
    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("Cannot write a config FILE to a DIRECTORY " + path);
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    if (Files.notExists(path)) {
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          resolvedAnnotations,
          path,
          settings,
          commentPrefix,
          valueWriter,
          keyResolver,
          reverseFields);
      return;
    }
    handlePathLoad(annotatedConfig, resolvedAnnotations, path, settings);
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile) {
    return resolveMultiple(dir, configToResolveTo, dumpFile, settings);
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile, Settings settings) {
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
          dir.listFiles(
              ($, name) -> {
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
          resolvedAnnotations = AnnotatedConfigResolver.resolveAnnotations(config, reverseFields);
        }
        handleFileLoad(config, resolvedAnnotations, file, settings);
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
            resolvedAnnotations = AnnotatedConfigResolver.resolveAnnotations(config, reverseFields);
          }
          handlePathLoad(config, resolvedAnnotations, path, settings);
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
      Settings settings) {
    Map<String, Object> values;
    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      values = valueReader.read(reader, settings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
              reverseFields);
        });
  }

  private void handlePathLoad(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations,
      Path path,
      Settings settings) {
    Map<String, Object> values;
    try (Reader reader =
        new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      values = valueReader.read(reader, settings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
              reverseFields);
        });
  }

  private void finishLoad(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations,
      Map<String, Object> values,
      Settings settings,
      Runnable missingOptionsAction) {
    if (values.isEmpty()) {
      return;
    }
    NullReadHandleOption nullReadHandler =
        settings
            .get(ACDefaultSettings.NULL_READ_HANDLER)
            .orElse(
                this.settings
                    .get(ACDefaultSettings.NULL_READ_HANDLER)
                    .orElse(
                        ACDefaultSettings.getDefault()
                            .get(ACDefaultSettings.NULL_READ_HANDLER)
                            .get()));
    boolean generateNewOptions =
        settings
            .get(ACDefaultSettings.GENERATE_NEW_OPTIONS)
            .orElse(
                this.settings
                    .get(ACDefaultSettings.GENERATE_NEW_OPTIONS)
                    .orElse(
                        ACDefaultSettings.getDefault()
                            .get(ACDefaultSettings.GENERATE_NEW_OPTIONS)
                            .get()));
    boolean missingOptions =
        AnnotatedConfigResolver.setFields(
            annotatedConfig,
            values,
            resolvedAnnotations,
            nullReadHandler,
            settings,
            keyResolver,
            reverseFields);
    if (missingOptions && generateNewOptions) {
      missingOptionsAction.run();
    }
  }
}
