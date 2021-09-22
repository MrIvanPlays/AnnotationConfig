package com.mrivanplays.annotationconfig.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Represents annotation registry for custom annotations. */
public final class CustomAnnotationRegistry {

  /**
   * Represents annotation resolver, responsible for........ wait for it....... resolving custom
   * annotations!!!
   *
   * @param <T> type of the annotation resolved
   */
  public interface AnnotationResolver<T extends Annotation> {

    /**
     * Called when a brand new config is being generated, should write the custom annotation's
     * values to the writer with the desired syntax of the config type. There are syntax exceptions
     * for config types the project maintains itself, and they are the following:
     *
     * <ul>
     *   <li>TOML: When writing something, it needs to be in the format <code>key=value</code>, or
     *       be a map.
     * </ul>
     *
     * @param writer writer
     * @param annotation annotation written
     * @param context writer context
     */
    void write(AnnotationWriter writer, T annotation, AnnotationResolverContext context);

    /**
     * Called when it is being read from the config, should return a {@link Supplier} of the
     * annotation's {@link FieldTypeResolver}.
     *
     * @return field type resolver supplier
     */
    Supplier<FieldTypeResolver> typeResolver();
  }

  /** Represents a {@link AnnotationResolver} context. */
  public static final class AnnotationResolverContext {

    private final Class<?> configType;
    private final Field field;
    private final Object annotatedConfig;
    private final Object defaultsToValue;
    private final String keyName;
    private final boolean partOfConfigObject;

    public AnnotationResolverContext(
        Class<?> configType,
        Field field,
        Object annotatedConfig,
        Object defaultsToValue,
        String keyName,
        boolean partOfConfigObject) {
      this.configType = configType;
      this.field = field;
      this.annotatedConfig = annotatedConfig;
      this.defaultsToValue = defaultsToValue;
      this.keyName = keyName;
      this.partOfConfigObject = partOfConfigObject;
    }

    /**
     * Returns the config type, which triggered the write method.
     *
     * @return config type
     */
    public Class<?> getConfigType() {
      return configType;
    }

    /**
     * Returns the field, holder of the written annotation.
     *
     * @return field
     */
    public Field getField() {
      return field;
    }

    /**
     * Returns the annotated config, holder of the field.
     *
     * @return annotated config
     */
    public Object getAnnotatedConfig() {
      return annotatedConfig;
    }

    /**
     * Returns the defaults value.
     *
     * @return defaults
     */
    public Object getDefaultsToValue() {
      return defaultsToValue;
    }

    /**
     * Returns the preferred name of the field for configuration use.
     *
     * @return key
     */
    public String getKeyName() {
      return keyName;
    }

    /**
     * Returns whether or not the field is a part of config object, and the {@link
     * #getAnnotatedConfig()} is a config object.
     *
     * @return config object or not
     */
    public boolean isPartOfConfigObject() {
      return partOfConfigObject;
    }
  }

  /**
   * Represents a wrapped writer.
   *
   * <p>The reason behind why we don't use java's writer is because different file formats have
   * different ways of writing things, and so we want to make advantage of that.
   */
  public static final class AnnotationWriter {

    private Map<WriteFunction, Object> toWrite = new HashMap<>();

    /**
     * Returns an unmodifiable copy of the map, containing the values the writer needs to write. If
     * the map is empty, this will return {@link Collections#emptyMap()}
     *
     * @return map, probably with values
     */
    public Map<WriteFunction, Object> toWrite() {
      if (toWrite.isEmpty()) {
        return Collections.emptyMap();
      }
      return Collections.unmodifiableMap(toWrite);
    }

    /**
     * Writes a string.
     *
     * @param s string
     */
    public void write(String s) {
      toWrite.put(WriteFunction.WRITE, s);
    }

    /**
     * Writes a character array. This will probably be converted to string upon writing.
     *
     * @param chars character array
     */
    public void write(char[] chars) {
      toWrite.put(WriteFunction.WRITE, chars);
    }

    /**
     * Writes a object.
     *
     * @param obj object
     */
    public void write(Object obj) {
      toWrite.put(WriteFunction.WRITE, obj);
    }

    /**
     * Writes a single character.
     *
     * @param c character
     */
    public void write(char c) {
      toWrite.put(WriteFunction.WRITE, c);
    }

    /**
     * Appends the typed character.
     *
     * @param c character
     */
    public void append(char c) {
      toWrite.put(WriteFunction.APPEND, c);
    }

    public enum WriteFunction {
      APPEND,
      WRITE
    }
  }

  private Map<AnnotationType, AnnotationResolver<? extends Annotation>> registry = new HashMap<>();

  /**
   * Registers a new annotation type.
   *
   * @param rawAnnoType raw annotation type
   * @param annoWriter annotation resolver
   * @param <T> type of annotation registered
   */
  public <T extends Annotation> void register(
      Class<T> rawAnnoType, AnnotationResolver<T> annoWriter) {
    registry.put(new AnnotationType(rawAnnoType), annoWriter);
  }

  /**
   * Returns an unmodifiable copy of the annotation registry map. If the map this {@link
   * CustomAnnotationRegistry} has is empty, this method will return {@link Collections#emptyMap()}
   *
   * <p>It is recommended to use the map only when you want to loop. If you want to get the {@link
   * AnnotationResolver} of a registered annotation, please use the appropriate API methods.
   *
   * @return annotation registry map
   */
  public Map<AnnotationType, AnnotationResolver<? extends Annotation>> registryMap() {
    if (registry.isEmpty()) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(registry);
  }

  /**
   * Returns the {@link AnnotationResolver} of the specified raw annotation. If there is none, it
   * will return an empty {@link Optional}
   *
   * @param rawAnnoType the raw annotation type
   * @return a populated optional with the value or an empty one
   */
  public Optional<AnnotationResolver<? extends Annotation>> getAnnotationResolver(
      Class<? extends Annotation> rawAnnoType) {
    if (registry.isEmpty()) {
      return Optional.empty();
    }
    for (AnnotationType type : registry.keySet()) {
      if (type.is(rawAnnoType)) {
        return Optional.of(registry.get(type));
      }
    }
    return Optional.empty();
  }

  /**
   * Returns the {@link AnnotationResolver} of the specified {@link AnnotationType}. If there is
   * none, it will return an empty {@link Optional}
   *
   * @param annotationType the annotation type you want the resolver for
   * @return a populated optional with the value or an empty one
   */
  public Optional<AnnotationResolver<? extends Annotation>> getAnnotationResolver(
      AnnotationType annotationType) {
    if (registry.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(registry.get(annotationType));
  }
}
