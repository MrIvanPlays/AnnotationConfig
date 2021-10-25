package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.ValueReader;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ConfigResolverImpl implements ConfigResolver {

  private final String commentPrefix;
  private final ValueWriter valueWriter;
  private final ValueReader valueReader;
  private final CustomOptions options;
  private final boolean reverseFields;

  public ConfigResolverImpl(
      String commentPrefix,
      ValueWriter valueWriter,
      ValueReader valueReader,
      CustomOptions options,
      boolean reverseFields) {
    this.commentPrefix = Objects.requireNonNull(commentPrefix, "commentPrefix");
    this.valueWriter = Objects.requireNonNull(valueWriter, "valueWriter");
    this.valueReader = Objects.requireNonNull(valueReader, "valueReader");
    if (options == null) {
      this.options = CustomOptions.empty();
    } else {
      this.options = options;
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
        reverseFields);
  }

  @Override
  public void load(Object annotatedConfig, File file, boolean generateNewOptions) {
    if (!file.exists()) {
      return;
    }
    Map<String, Object> values;
    try {
      values = valueReader.read(file, options);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (values.isEmpty()) {
      return;
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        resolvedAnnotations,
        commentPrefix,
        valueWriter,
        file,
        options,
        generateNewOptions,
        reverseFields,
        false,
        null);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, File file, boolean generateNewOptions) {
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
          reverseFields);
      return;
    }
    Map<String, Object> values;
    try {
      values = valueReader.read(file, options);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (values.isEmpty()) {
      return;
    }
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        resolvedAnnotations,
        commentPrefix,
        valueWriter,
        file,
        options,
        generateNewOptions,
        reverseFields,
        false,
        null);
  }
}
