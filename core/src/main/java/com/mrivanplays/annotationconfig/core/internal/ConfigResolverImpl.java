package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSetting;
import com.mrivanplays.annotationconfig.core.resolver.settings.LoadSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ConfigResolverImpl implements ConfigResolver {

  private final String commentPrefix;
  private final ValueWriter valueWriter;
  private final ValueReader valueReader;
  private final CustomOptions options;
  private final LoadSettings defaultLoadSettings;
  private final KeyResolver keyResolver;
  private final boolean reverseFields;

  public ConfigResolverImpl(
      String commentPrefix,
      ValueWriter valueWriter,
      ValueReader valueReader,
      CustomOptions options,
      LoadSettings loadSettings,
      KeyResolver keyResolver,
      boolean reverseFields) {
    this.commentPrefix = Objects.requireNonNull(commentPrefix, "commentPrefix");
    this.valueWriter = Objects.requireNonNull(valueWriter, "valueWriter");
    this.valueReader = Objects.requireNonNull(valueReader, "valueReader");
    if (options == null) {
      this.options = CustomOptions.empty();
    } else {
      this.options = options;
    }
    if (loadSettings == null) {
      this.defaultLoadSettings = LoadSettings.getDefault();
    } else {
      this.defaultLoadSettings = loadSettings;
    }
    if (keyResolver == null) {
      this.keyResolver = KeyResolver.DEFAULT;
    } else {
      this.keyResolver = keyResolver;
    }
    this.reverseFields = reverseFields;
  }

  @Override
  public CustomOptions options() {
    return options;
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
        options,
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
        options,
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
        options,
        commentPrefix,
        valueWriter,
        keyResolver,
        reverseFields);
  }

  @Override
  public void load(Object annotatedConfig, File file) {
    load(annotatedConfig, file, this.defaultLoadSettings);
  }

  @Override
  public void load(Object annotatedConfig, Path path) {
    load(annotatedConfig, path, this.defaultLoadSettings);
  }

  @Override
  public void load(Object annotatedConfig, File file, LoadSettings loadSettings) {
    if (!file.exists()) {
      return;
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    handleFileLoad(annotatedConfig, resolvedAnnotations, file, loadSettings);
  }

  @Override
  public void load(Object annotatedConfig, Path path, LoadSettings loadSettings) {
    if (!Files.exists(path) || Files.isDirectory(path)) {
      return;
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    handlePathLoad(annotatedConfig, resolvedAnnotations, path, loadSettings);
  }

  @Override
  public void load(Object annotatedConfig, Map<String, Object> values) {
    load(annotatedConfig, values, this.defaultLoadSettings);
  }

  @Override
  public void load(Object annotatedConfig, Map<String, Object> values, LoadSettings loadSettings) {
    if (values.isEmpty()) {
      return;
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    NullReadHandleOption nullReadHandler =
        loadSettings
            .get(LoadSetting.NULL_READ_HANDLER)
            .orElse(
                defaultLoadSettings
                    .get(LoadSetting.NULL_READ_HANDLER)
                    .orElse(LoadSettings.getDefault().get(LoadSetting.NULL_READ_HANDLER).get()));
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        resolvedAnnotations,
        nullReadHandler,
        options,
        keyResolver,
        reverseFields);
  }

  @Override
  public void load(Object annotatedConfig, Reader reader) {
    load(annotatedConfig, reader, this.defaultLoadSettings);
  }

  @Override
  public void load(Object annotatedConfig, Reader reader, LoadSettings loadSettings) {
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    Map<String, Object> values;
    try {
      try {
        values = valueReader.read(reader, options, loadSettings);
      } finally {
        reader.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    NullReadHandleOption nullReadHandler =
        loadSettings
            .get(LoadSetting.NULL_READ_HANDLER)
            .orElse(
                defaultLoadSettings
                    .get(LoadSetting.NULL_READ_HANDLER)
                    .orElse(LoadSettings.getDefault().get(LoadSetting.NULL_READ_HANDLER).get()));
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        resolvedAnnotations,
        nullReadHandler,
        options,
        keyResolver,
        reverseFields);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, File file) {
    loadOrDump(annotatedConfig, file, this.defaultLoadSettings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, Path path) {
    loadOrDump(annotatedConfig, path, this.defaultLoadSettings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, File file, LoadSettings loadSettings) {
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    if (!file.exists()) {
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          resolvedAnnotations,
          file,
          options,
          commentPrefix,
          valueWriter,
          keyResolver,
          reverseFields);
      return;
    }
    handleFileLoad(annotatedConfig, resolvedAnnotations, file, loadSettings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, Path path, LoadSettings loadSettings) {
    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("Cannot write a config FILE to a DIRECTORY " + path);
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    if (!Files.exists(path)) {
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          resolvedAnnotations,
          path,
          options,
          commentPrefix,
          valueWriter,
          keyResolver,
          reverseFields);
      return;
    }
    handlePathLoad(annotatedConfig, resolvedAnnotations, path, loadSettings);
  }

  private void handleFileLoad(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations,
      File file,
      LoadSettings loadSettings) {
    Map<String, Object> values;
    try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
      values = valueReader.read(reader, options, loadSettings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (values.isEmpty()) {
      return;
    }
    NullReadHandleOption nullReadHandler =
        loadSettings
            .get(LoadSetting.NULL_READ_HANDLER)
            .orElse(
                defaultLoadSettings
                    .get(LoadSetting.NULL_READ_HANDLER)
                    .orElse(LoadSettings.getDefault().get(LoadSetting.NULL_READ_HANDLER).get()));
    boolean generateNewOptions =
        loadSettings
            .get(LoadSetting.GENERATE_NEW_OPTIONS)
            .orElse(
                defaultLoadSettings
                    .get(LoadSetting.GENERATE_NEW_OPTIONS)
                    .orElse(LoadSettings.getDefault().get(LoadSetting.GENERATE_NEW_OPTIONS).get()));
    boolean missingOptions =
        AnnotatedConfigResolver.setFields(
            annotatedConfig,
            values,
            resolvedAnnotations,
            nullReadHandler,
            options,
            keyResolver,
            reverseFields);
    if (missingOptions && generateNewOptions) {
      file.delete();
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          resolvedAnnotations,
          file,
          options,
          commentPrefix,
          valueWriter,
          keyResolver,
          reverseFields);
    }
  }

  public void handlePathLoad(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations,
      Path path,
      LoadSettings loadSettings) {
    Map<String, Object> values;
    try (Reader reader =
        new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      values = valueReader.read(reader, options, loadSettings);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (values.isEmpty()) {
      return;
    }
    NullReadHandleOption nullReadHandler =
        loadSettings
            .get(LoadSetting.NULL_READ_HANDLER)
            .orElse(
                defaultLoadSettings
                    .get(LoadSetting.NULL_READ_HANDLER)
                    .orElse(LoadSettings.getDefault().get(LoadSetting.NULL_READ_HANDLER).get()));
    boolean generateNewOptions =
        loadSettings
            .get(LoadSetting.GENERATE_NEW_OPTIONS)
            .orElse(
                defaultLoadSettings
                    .get(LoadSetting.GENERATE_NEW_OPTIONS)
                    .orElse(LoadSettings.getDefault().get(LoadSetting.GENERATE_NEW_OPTIONS).get()));
    boolean missingOptions =
        AnnotatedConfigResolver.setFields(
            annotatedConfig,
            values,
            resolvedAnnotations,
            nullReadHandler,
            options,
            keyResolver,
            reverseFields);
    if (missingOptions && generateNewOptions) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      AnnotatedConfigResolver.dump(
          annotatedConfig,
          resolvedAnnotations,
          path,
          options,
          commentPrefix,
          valueWriter,
          keyResolver,
          reverseFields);
    }
  }
}
