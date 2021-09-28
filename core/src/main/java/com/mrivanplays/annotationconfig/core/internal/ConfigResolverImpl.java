package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.ConfigResolver;
import com.mrivanplays.annotationconfig.core.ValueWriter;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class ConfigResolverImpl implements ConfigResolver {

  private final String commentPrefix;
  private final ValueWriter valueWriter;
  private final Function<File, Map<String, Object>> valueReader;
  private final boolean reverseFields;

  public ConfigResolverImpl(
      String commentPrefix,
      ValueWriter valueWriter,
      Function<File, Map<String, Object>> valueReader,
      boolean reverseFields) {
    this.commentPrefix = Objects.requireNonNull(commentPrefix, "commentPrefix");
    this.valueWriter = Objects.requireNonNull(valueWriter, "valueWriter");
    this.valueReader = Objects.requireNonNull(valueReader, "valueReader");
    this.reverseFields = reverseFields;
  }

  @Override
  public void dump(Object annotatedConfig, File file) {
    if (file.exists()) {
      file.delete();
    }
    Map<AnnotationHolder, Set<AnnotationType>> resolvedAnnotations =
        AnnotatedConfigResolver.resolveAnnotations(annotatedConfig, reverseFields);
    AnnotatedConfigResolver.dump(
        annotatedConfig, resolvedAnnotations, file, commentPrefix, valueWriter, reverseFields);
  }

  @Override
  public void load(Object annotatedConfig, File file, boolean generateNewOptions) {
    if (!file.exists()) {
      return;
    }
    Map<String, Object> values = valueReader.apply(file);
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
          annotatedConfig, resolvedAnnotations, file, commentPrefix, valueWriter, reverseFields);
      return;
    }
    Map<String, Object> values = valueReader.apply(file);
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
        generateNewOptions,
        reverseFields,
        false,
        null);
  }
}
