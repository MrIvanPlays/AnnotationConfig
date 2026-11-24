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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

  @Nullable private final String commentPrefix;
  private final ValueWriter valueWriter;
  private final ValueReader valueReader;
  private final Settings settings;
  private final List<String> fileExtensions;

  public ConfigResolverImpl(
      @Nullable String commentPrefix,
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
    if (settings == null) {
      this.settings = ACDefaultSettings.getDefault().copy();
    } else {
      this.settings = ACDefaultSettings.getDefault().copyWithOverrides(settings);
    }
  }

  private static void checkNotDirectory(Path path) {
    if (Files.isDirectory(path)) {
      throw new IllegalArgumentException("Cannot write a config file to a directory: " + path);
    }
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
  public void dump(Object annotatedConfig, File file, Settings settings) {
    Objects.requireNonNull(file, "file");
    this.dump(annotatedConfig, file.toPath(), settings);
  }

  @Override
  public void dump(Object annotatedConfig, Path path) {
    this.dump(annotatedConfig, path, this.settings);
  }

  @Override
  public void dump(Object annotatedConfig, Path path, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(settings, "settings");
    checkNotDirectory(path);
    Settings passed = this.passSettings(settings);
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, passed),
        path,
        passed,
        this.commentPrefix,
        this.valueWriter);
  }

  @Override
  public void dump(Object annotatedConfig, Writer writer) {
    this.dump(annotatedConfig, writer, this.settings);
  }

  @Override
  public void dump(Object annotatedConfig, Writer writer, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(writer, "writer");
    Objects.requireNonNull(settings, "settings");
    this.dumpWithPassedSettings(annotatedConfig, writer, this.passSettings(settings));
  }

  private void dumpWithPassedSettings(Object annotatedConfig, Writer writer, Settings passed) {
    AnnotatedConfigResolver.dump(
        annotatedConfig,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, passed),
        writer,
        passed,
        this.commentPrefix,
        this.valueWriter);
  }

  @Override
  public void load(Object annotatedConfig, File file) {
    this.load(annotatedConfig, file, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, Path path) {
    this.load(annotatedConfig, path, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, File file, Settings settings) {
    Objects.requireNonNull(file, "file");
    this.load(annotatedConfig, file.toPath(), settings);
  }

  @Override
  public void load(Object annotatedConfig, Path path, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(settings, "settings");
    // as dump does: a directory is never a config file, which used to be skipped silently
    checkNotDirectory(path);
    if (Files.notExists(path)) {
      return;
    }
    Settings passed = this.passSettings(settings);
    handlePathLoad(
        annotatedConfig,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, passed),
        path,
        passed);
  }

  @Override
  public void load(Object annotatedConfig, Map<String, Object> values) {
    this.load(annotatedConfig, values, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, Map<String, Object> values, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(values, "values");
    Objects.requireNonNull(settings, "settings");
    // empty values are read as every option missing, as from a file or a reader
    Settings passed = this.passSettings(settings);
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, passed),
        passed);
  }

  @Override
  public void load(Object annotatedConfig, Reader reader) {
    this.load(annotatedConfig, reader, this.settings);
  }

  @Override
  public void load(Object annotatedConfig, Reader reader, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(reader, "reader");
    Objects.requireNonNull(settings, "settings");
    Settings passed = this.passSettings(settings);
    Map<String, Object> values;
    try (reader) {
      values = this.valueReader.read(reader, passed);
    } catch (IOException e) {
      throw new RuntimeException("Could not read the configuration", e);
    }
    AnnotatedConfigResolver.setFields(
        annotatedConfig,
        values,
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, passed),
        passed);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, File file) {
    this.loadOrDump(annotatedConfig, file, this.settings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, Path path) {
    this.loadOrDump(annotatedConfig, path, this.settings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, File file, Settings settings) {
    Objects.requireNonNull(file, "file");
    this.loadOrDump(annotatedConfig, file.toPath(), settings);
  }

  @Override
  public void loadOrDump(Object annotatedConfig, Path path, Settings settings) {
    Objects.requireNonNull(annotatedConfig, "annotatedConfig");
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(settings, "settings");
    checkNotDirectory(path);
    Settings passed = this.passSettings(settings);
    Set<AnnotationHolder> holders =
        AnnotatedConfigResolver.resolveAnnotationHolders(annotatedConfig, passed);
    if (Files.notExists(path)) {
      AnnotatedConfigResolver.dump(
          annotatedConfig, holders, path, passed, this.commentPrefix, this.valueWriter);
      return;
    }
    this.handlePathLoad(annotatedConfig, holders, path, passed);
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile) {
    return this.resolveMultiple(dir, configToResolveTo, dumpFile, this.settings);
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      File dir, Supplier<T> configToResolveTo, WritableObject dumpFile, Settings settings) {
    Objects.requireNonNull(dir, "dir");
    return this.resolveMultiple(dir.toPath(), configToResolveTo, dumpFile, settings);
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      Path dir, Supplier<T> configToResolveTo, WritableObject dumpFile) {
    return this.resolveMultiple(dir, configToResolveTo, dumpFile, this.settings);
  }

  @Override
  public <T> Map<String, T> resolveMultiple(
      Path dir, Supplier<T> configToResolveTo, WritableObject dumpFile, Settings settings) {
    Objects.requireNonNull(dir, "dir");
    Objects.requireNonNull(configToResolveTo, "configToResolveTo");
    Objects.requireNonNull(dumpFile, "dumpFile");
    Objects.requireNonNull(settings, "settings");
    // a missing directory is created below; only an existing non-directory is an error
    if (Files.exists(dir) && !Files.isDirectory(dir)) {
      throw new IllegalArgumentException(dir + " is not a directory!");
    }
    Settings passed = this.passSettings(settings);
    List<Path> configFiles;
    try {
      Files.createDirectories(dir);
      try (Stream<Path> paths = Files.list(dir)) {
        // sorted, so that the returned map has the same order on every file system
        configFiles =
            paths.filter(Files::isRegularFile).filter(this::hasConfigExtension).sorted().toList();
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not read the configurations in " + dir, e);
    }
    if (configFiles.isEmpty()) {
      // the settings are combined already: they aren't passed through passSettings again
      this.dumpWithPassedSettings(configToResolveTo.get(), dumpFile.writer(), passed);
      return Collections.emptyMap();
    }
    Set<AnnotationHolder> holders = null;
    Map<String, T> ret = new LinkedHashMap<>();
    for (Path path : configFiles) {
      T config = configToResolveTo.get();
      if (holders == null) {
        holders = AnnotatedConfigResolver.resolveAnnotationHolders(config, passed);
      }
      this.handlePathLoad(config, holders, path, passed);
      ret.put(path.getFileName().toString(), config);
    }
    return ret;
  }

  /**
   * Whether the name of {@code path} ends with one of the file extensions, ignoring case, so that
   * {@code CONFIG.YML} is found too. An extension given without its dot ({@code "yml"}) gets one,
   * so it doesn't match the end of any name ({@code "html"}).
   */
  private boolean hasConfigExtension(Path path) {
    String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
    for (String extension : this.fileExtensions) {
      String suffix = extension.toLowerCase(Locale.ROOT);
      if (fileName.endsWith(suffix.startsWith(".") ? suffix : "." + suffix)) {
        return true;
      }
    }
    return false;
  }

  private void handlePathLoad(
      Object annotatedConfig, Set<AnnotationHolder> holders, Path path, Settings settings) {
    Map<String, Object> values;
    // not Files.newBufferedReader: it rejects malformed input, which this replaces
    try (Reader reader =
        new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      values = this.valueReader.read(reader, settings);
    } catch (IOException e) {
      throw new RuntimeException("Could not read the configuration " + path, e);
    }
    // an empty file misses every option, so with GENERATE_NEW_OPTIONS it gets them written; it
    // used to be left empty, also by loadOrDump
    boolean missingOptions =
        AnnotatedConfigResolver.setFields(annotatedConfig, values, holders, settings);
    if (missingOptions && settings.get(ACDefaultSettings.GENERATE_NEW_OPTIONS).orElseThrow()) {
      AnnotatedConfigResolver.dump(
          annotatedConfig, holders, path, settings, this.commentPrefix, this.valueWriter);
    }
  }

  private Settings passSettings(Settings passed) {
    if (passed.equals(this.settings)) {
      return this.settings;
    }
    if (passed.immutable()) {
      throw new IllegalArgumentException(
          "Settings passed in #load, #dump and #loadOrDump must be mutable");
    }
    return passed.combine(this.settings);
  }
}
