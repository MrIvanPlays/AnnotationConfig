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

import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @hidden
 */
@SuppressWarnings({"unchecked", "rawtypes", "ConstantValue"})
@NullMarked
final class AnnotatedConfigResolver {

  private static final String INACCESSIBLE = "A field became inaccessible";

  private AnnotatedConfigResolver() {
    throw new IllegalStateException("Instantiation of utility-type class");
  }

  /**
   * The commented classes and the fields of {@code annotatedClass}'s class, in the order they are
   * written. The set is cached per class and settings, and can't be modified.
   */
  public static Set<AnnotationHolder> resolveAnnotationHolders(
      Object annotatedClass, Settings settings) {
    boolean findParentFields = settings.get(ACDefaultSettings.FIND_PARENT_FIELDS).orElseThrow();
    boolean reverseFields = settings.get(ACDefaultSettings.SHOULD_REVERSE_FIELDS).orElseThrow();
    boolean findStaticFields = settings.get(ACDefaultSettings.FIND_STATIC_FIELDS).orElseThrow();
    return ClassInfo.of(annotatedClass.getClass())
        .holders(findParentFields, reverseFields, findStaticFields);
  }

  public static void dump(
      Object annotatedConfig,
      Set<AnnotationHolder> holders,
      Path path,
      Settings settings,
      @Nullable String commentChar,
      ValueWriter valueWriter) {
    Path tmp = null;
    Path created = null;
    try {
      // a symbolic link stays a link: the file it points to is written
      Path target = followLinks(path.toAbsolutePath());
      Path parent = target.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }

      if (Files.notExists(target)) {
        // nothing to keep if writing fails, so the file is written in place, and gets the
        // permissions a new file gets; a temporary file is only readable by its owner
        created = target;
        writeTo(target, annotatedConfig, holders, commentChar, valueWriter, settings);
        created = null;
        return;
      }

      // the old file is replaced only once the new one is complete
      tmp = Files.createTempFile(parent, target.getFileName().toString() + ".", ".tmp");
      writeTo(tmp, annotatedConfig, holders, commentChar, valueWriter, settings);
      // the temporary file is only readable by its owner; the config keeps its permissions
      if (Files.getFileAttributeView(target, PosixFileAttributeView.class) != null) {
        Files.setPosixFilePermissions(tmp, Files.getPosixFilePermissions(target));
      }
      try {
        Files.move(
            tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to dump configuration to " + path, e);
    } finally {
      // best effort clean-up of a temporary file, or of a new file that couldn't be written
      for (Path leftOver : new Path[] {tmp, created}) {
        if (leftOver != null) {
          try {
            Files.deleteIfExists(leftOver);
          } catch (IOException e) {
            // ignored
          }
        }
      }
    }
  }

  public static void dump(
      Object annotatedConfig,
      Set<AnnotationHolder> holders,
      Writer writerFeed,
      Settings settings,
      @Nullable String commentChar,
      ValueWriter valueWriter) {
    try (PrintWriter writer = new PrintWriter(writerFeed)) {
      toWriter(annotatedConfig, writer, holders, commentChar, valueWriter, settings);

      if (writer.checkError()) {
        throw new IOException("Failed to write configuration");
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to dump configuration to writer", e);
    }
  }

  private static void writeTo(
      Path file,
      Object annotatedConfig,
      Set<AnnotationHolder> holders,
      @Nullable String commentChar,
      ValueWriter valueWriter,
      Settings settings)
      throws IOException {
    try (PrintWriter writer =
        new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
      toWriter(annotatedConfig, writer, holders, commentChar, valueWriter, settings);
      // checkError() flushes first
      if (writer.checkError()) {
        throw new IOException("Failed to write configuration");
      }
    }
  }

  /** The file {@code path} points to, following symbolic links, also to a file that is missing. */
  private static Path followLinks(Path path) throws IOException {
    Path current = path;
    // a cycle of links is not followed forever
    for (int i = 0; i < 40 && Files.isSymbolicLink(current); i++) {
      Path link = Files.readSymbolicLink(current);
      Path parent = current.getParent();
      current = (parent == null ? link : parent.resolve(link)).normalize();
    }
    return current;
  }

  private static void toWriter(
      Object annotatedConfig,
      PrintWriter writer,
      Set<AnnotationHolder> holders,
      @Nullable String commentChar,
      ValueWriter valueWriter,
      Settings settings)
      throws IOException {
    WriteContext context =
        new WriteContext(
            settings,
            settings.get(ACDefaultSettings.KEY_RESOLVER).orElseThrow(),
            // a writer without comments gets none, so they aren't collected
            valueWriter.writesComments() ? new LinkedHashMap<>() : null);
    Map<String, Object> toWrite = new LinkedHashMap<>();
    FieldInfo rawConfigField = null;
    for (AnnotationHolder holder : holders) {
      if (holder.isClass()) {
        // the blank line separates the comments from the values; without comments (e.g. JSON,
        // which has none) it would be a stray leading line
        if (commentChar != null) {
          List<String> comments = ClassInfo.of(holder.getClazz()).comments();
          if (!comments.isEmpty()) {
            for (String comment : comments) {
              writer.print(commentChar + comment);
              writer.append('\n');
            }
            writer.append('\n');
          }
        }
        continue;
      }
      FieldInfo field = holder.getInfo();
      if (field.rawConfig()) {
        rawConfigField = field;
        continue;
      }
      try {
        writeField(annotatedConfig, field, context, toWrite, "");
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException(INACCESSIBLE, e);
      }
    }
    if (rawConfigField != null) {
      setRawConfig(rawConfigField, annotatedConfig, toWrite);
    }
    Map<String, List<String>> fieldComments =
        context.comments() == null ? Collections.emptyMap() : context.comments();
    valueWriter.write(toWrite, fieldComments, writer, settings);
  }

  /** The written form of a serialized value: the value itself, a list, or a map for a section. */
  private static @Nullable Object unwrap(DataObject serialized) {
    return serialized.isSingleValue() ? serialized.getAsObject() : serialized.getAsMap();
  }

  /** The registered serializer of {@code type}, or the default serializer. */
  private static FieldTypeSerializer serializerFor(Type type) {
    SerializerRegistry registry = SerializerRegistry.INSTANCE;
    return registry.getSerializer(type).orElse(registry.getDefaultSerializer());
  }

  private static DataObject requireSerialized(
      @Nullable DataObject serialized, Field field, Class<?> type) {
    if (serialized == null) {
      throw new NullPointerException(
          "Expected DataObject, but got null ; Field: "
              + field.getName()
              + " ; Field type: "
              + type.getName());
    }
    return serialized;
  }

  /** Puts the read or written values into a {@code @RawConfig} field. */
  private static void setRawConfig(
      FieldInfo info, Object annotatedConfig, Map<String, Object> values) {
    Field field = info.ensureAccessible();
    if (!field.getType().isAssignableFrom(DataObject.class)) {
      throw new IllegalArgumentException("@RawConfig on a field which is not DataObject");
    }
    setField(field, annotatedConfig, new DataObject(values, true));
  }

  /** The section of {@code info} in {@code annotatedConfig}, for error messages. */
  private static String describe(FieldInfo info, Object annotatedConfig) {
    return "the section '"
        + info.key()
        + "' (field "
        + info.name()
        + " of "
        + annotatedConfig.getClass().getName()
        + ")";
  }

  /**
   * Reads the {@code @Polymorphic} section {@code section} into {@code field}: the value it holds
   * is filled when the section names its class, and otherwise replaced by a new instance of the
   * class the section names.
   *
   * @return whether options are missing, the discriminator included
   */
  private static boolean setPolymorphic(
      FieldInfo info,
      PolymorphicInfo polymorphic,
      Object annotatedConfig,
      Map<String, Object> section,
      Settings settings) {
    Field field = info.field();
    Object current;
    try {
      current = field.get(annotatedConfig);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(INACCESSIBLE, e);
    }
    Class<?> type = polymorphic.typeOf(section, current, () -> describe(info, annotatedConfig));
    boolean missingOptions = polymorphic.lacksDiscriminator(section);
    boolean sameType = current != null && current.getClass() == type;
    if (type.isRecord()) {
      // as a record @ConfigObject: read whole, the components missing from the section taking
      // their values from the record the field holds if it is of that type
      Object read =
          serializerFor(type)
              .deserialize(
                  new DataObject(section, true),
                  SerializationContext.of(
                      sameType ? current : null, type, type, annotatedConfig, settings),
                  AnnotationAccessor.EMPTY);
      if (read != null) {
        setField(field, annotatedConfig, read);
      }
      return missingOptions;
    }
    Object value = sameType ? current : ClassInfo.newInstance(type);
    missingOptions |=
        setFields(value, section, resolveAnnotationHolders(value, settings), settings);
    if (!sameType) {
      // set once it is read, so a failure leaves the field as it was
      setField(field, annotatedConfig, value);
    }
    return missingOptions;
  }

  /**
   * Serializes {@code field} of {@code annotatedConfig} into {@code toWrite}, the values of the
   * section it is in, and its comments into the context under {@code commentPath} + its key.
   *
   * @param commentPath the dotted path of the section followed by a dot, "" at the top level
   */
  private static void writeField(
      Object annotatedConfig,
      FieldInfo info,
      WriteContext context,
      Map<String, Object> toWrite,
      String commentPath)
      throws IllegalAccessException {
    Field field = info.ensureAccessible();
    String keyName = info.key();
    Map<String, List<String>> allComments = context.comments();
    if (info.configObject()) {
      Object cfgObject = field.get(annotatedConfig);
      if (cfgObject == null) {
        throw new IllegalArgumentException(
            "Non initialized config object '"
                + field.getName()
                + "' found in annotated config '"
                + annotatedConfig.getClass().getSimpleName()
                + "'");
      }
      Map<String, Object> sectionValues = new LinkedHashMap<>();
      PolymorphicInfo polymorphic = info.polymorphic();
      if (polymorphic != null) {
        // first, so that it is the first key of the section
        sectionValues.put(
            polymorphic.discriminator(),
            polymorphic.nameOf(cfgObject.getClass(), () -> describe(info, annotatedConfig)));
      }
      // the comments of the section's classes, used when the field itself has none
      List<String> classComments = null;
      String sectionPath = allComments == null ? "" : commentPath + keyName + ".";
      for (AnnotationHolder child : resolveAnnotationHolders(cfgObject, context.settings())) {
        if (child.isClass()) {
          // the holder's class, not cfgObject's: with FIND_PARENT_FIELDS a holder can be a
          // parent class, whose comments were replaced by a duplicate of the child's
          if (allComments != null) {
            List<String> comments = ClassInfo.of(child.getClazz()).comments();
            if (!comments.isEmpty()) {
              if (classComments == null) {
                classComments = new ArrayList<>(comments.size());
              }
              classComments.addAll(comments);
            }
          }
          continue;
        }
        writeField(cfgObject, child.getInfo(), context, sectionValues, sectionPath);
      }
      combineMapToData(toWrite, keyName, sectionValues);

      if (allComments != null) {
        List<String> comments = info.comments();
        if (!comments.isEmpty()) {
          allComments.put(commentPath + keyName, comments);
        } else if (classComments != null) {
          allComments.put(commentPath + keyName, classComments);
        }
      }
      return;
    }
    Object defaultsToValueObject = field.get(annotatedConfig);
    if (defaultsToValueObject == null) {
      throw new IllegalArgumentException("No default value for field '" + field.getName() + "'");
    }
    DataObject serialized =
        requireSerialized(
            serializerFor(info.genericType())
                .serialize(
                    defaultsToValueObject,
                    SerializationContext.of(
                        field.getName(),
                        defaultsToValueObject,
                        field.getType(),
                        info.genericType(),
                        annotatedConfig,
                        context.settings()),
                    info.accessor()),
            field,
            field.getType());
    if (serialized.isEmpty()) {
      return;
    }
    defaultsToValueObject = unwrap(serialized);
    // check for multiline string
    Multiline multiline = info.multiline();
    if (multiline != null) {
      if (!(defaultsToValueObject instanceof String string)) {
        throw new IllegalArgumentException("@Multiline put on a value which is not a String!");
      }
      defaultsToValueObject = new MultilineString(string, multiline.value());
    }
    // check for custom annotations writeValue implementations
    CustomAnnotationRegistry caRegistry = CustomAnnotationRegistry.INSTANCE;
    if (!caRegistry.isEmpty()) {
      Class<? extends Annotation> foundWriteAnnotation = null;
      for (Annotation annotation : info.annotations()) {
        Class<? extends Annotation> type = annotation.annotationType();
        if (AnnotationType.match(type).isPresent()) {
          continue;
        }
        Optional<AnnotationValidator<? extends Annotation>> validatorOpt =
            caRegistry.getValidator(type);
        if (validatorOpt.isPresent()) {
          AnnotationValidator validator = validatorOpt.get();
          Object val = validator.writeValue(defaultsToValueObject);
          if (val == null || Objects.equals(val, defaultsToValueObject)) {
            continue;
          }
          if (foundWriteAnnotation == null) {
            // the context describes val, which is what is serialized now
            DataObject newSerialized =
                requireSerialized(
                    serializerFor(val.getClass())
                        .serialize(
                            val,
                            SerializationContext.of(
                                field.getName(),
                                val,
                                val.getClass(),
                                val.getClass(),
                                annotatedConfig,
                                context.settings()),
                            info.accessor()),
                    field,
                    val.getClass());
            if (newSerialized.isEmpty()) {
              continue;
            }
            defaultsToValueObject = unwrap(newSerialized);
            foundWriteAnnotation = type;
          } else {
            throw new IllegalArgumentException(
                "Found 2 custom annotations on field '"
                    + field.getName()
                    + "' which implement 'writeValue': @"
                    + foundWriteAnnotation.getSimpleName()
                    + " and @"
                    + type.getSimpleName());
          }
        }
      }
    }
    // manipulate the defaultsToValueObject once again before sending it to the writer
    Map<String, Object> boxed = context.boxed();
    boxed.clear();
    context.keyResolver().boxTo(keyName, defaultsToValueObject, boxed);
    if (boxed.size() != 1) {
      throw new IllegalArgumentException("Invalid key resolver.");
    }
    Map.Entry<String, Object> entry = boxed.entrySet().iterator().next();
    combineMapToData(toWrite, entry.getKey(), entry.getValue());

    if (allComments != null) {
      allComments.put(commentPath + keyName, info.comments());
    }
  }

  private static void combineMapToData(Map<String, Object> toWrite, String key, Object value) {
    Object stored = toWrite.get(key);
    if (stored == null && !toWrite.containsKey(key)) {
      toWrite.put(key, value);
      return;
    }
    if (!(value instanceof Map)) {
      throw new IllegalArgumentException("Duplicate key '" + key + "'");
    }
    if (!(stored instanceof Map)) {
      throw new IllegalArgumentException(
          "Key '" + key + "' is both a value and a section ; check your annotated config.");
    }
    // populateFirst merges into the stored map in place
    MapUtils.populateFirst((Map<String, Object>) stored, (Map<String, Object>) value);
  }

  // returns whether it's missing options
  public static boolean setFields(
      Object annotatedConfig,
      Map<String, Object> values,
      Set<AnnotationHolder> holders,
      Settings settings) {
    KeyResolver keyResolver = settings.get(ACDefaultSettings.KEY_RESOLVER).orElseThrow();
    NullReadHandleOption nullReadHandler =
        settings.get(ACDefaultSettings.NULL_READ_HANDLER).orElseThrow();
    boolean missingOptions = false;
    for (AnnotationHolder holder : holders) {
      if (holder.isClass()) {
        continue;
      }
      FieldInfo info = holder.getInfo();
      if (info.rawConfig()) {
        setRawConfig(info, annotatedConfig, values);
        continue;
      }
      Field field = info.ensureAccessible();
      String keyName = info.key();
      Object value = keyResolver.unbox(keyName, values);
      boolean thisMissingOption = value == null;
      missingOptions |= thisMissingOption;
      if (info.configObject()) {
        PolymorphicInfo polymorphic = info.polymorphic();
        if (polymorphic != null) {
          // as for @ConfigObject: a value that is no section keeps the field's value
          if (value instanceof Map) {
            missingOptions |=
                setPolymorphic(
                    info, polymorphic, annotatedConfig, (Map<String, Object>) value, settings);
          }
          continue;
        }
        Object section;
        try {
          section = field.get(annotatedConfig);
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException(
              "Could not get config object from annotated config '"
                  + annotatedConfig.getClass().getSimpleName()
                  + "'",
              e);
        }
        if (section == null) {
          throw new IllegalArgumentException(
              "Non initialized config object found in annotated config '"
                  + annotatedConfig.getClass().getSimpleName()
                  + "'");
        }
        if (!(value instanceof Map)) {
          // sections not supported, continue on
          continue;
        }
        if (field.getType().isRecord()) {
          // directly deserialize the value and set the field, do NOT try to set record fields
          Type fieldType = info.genericType();
          Object deserialized =
              serializerFor(fieldType)
                  .deserialize(
                      new DataObject(value, true),
                      SerializationContext.of(
                          field.getName(),
                          section,
                          field.getType(),
                          fieldType,
                          annotatedConfig,
                          settings),
                      info.accessor());
          if (!skipNull(deserialized, thisMissingOption, nullReadHandler)) {
            setField(field, annotatedConfig, deserialized);
          }
          continue;
        }
        missingOptions |=
            setFields(
                section,
                (Map<String, Object>) value,
                resolveAnnotationHolders(section, settings),
                settings);
        continue;
      }
      Object deserialized;
      try {
        deserialized =
            serializerFor(info.genericType())
                .deserialize(
                    new DataObject(value, true),
                    SerializationContext.fromField(field, annotatedConfig, settings),
                    info.accessor());
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException(INACCESSIBLE, e);
      }
      if (skipNull(deserialized, thisMissingOption, nullReadHandler)
          || (deserialized == null && field.getType().isPrimitive())) {
        // a primitive field can't hold null, so it keeps its value even with SET_NULL
        continue;
      }
      // @Min, @Max and custom annotations; a validator that fails silently keeps the field's value
      if (!FieldValidation.validate(info, keyName, deserialized, settings)) {
        continue;
      }
      setField(field, annotatedConfig, deserialized);
    }
    return missingOptions;
  }

  /**
   * Whether a deserialized {@code null} should leave the field untouched: always when the option is
   * missing, and when it is present but read as null only with {@code USE_DEFAULT_VALUE}.
   */
  private static boolean skipNull(
      @Nullable Object deserialized, boolean missingOption, NullReadHandleOption handler) {
    return deserialized == null
        && (missingOption || handler == NullReadHandleOption.USE_DEFAULT_VALUE);
  }

  private static void setField(Field field, Object target, @Nullable Object value) {
    try {
      field.set(target, value);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(INACCESSIBLE, e);
    }
  }

  /**
   * What stays the same while one config is dumped.
   *
   * @param comments where the comments of all options go, under their dotted path from the root;
   *     {@code null} when the writer doesn't write comments
   * @param boxed a map reused for every {@link KeyResolver#boxTo} call, instead of a new one per
   *     option
   */
  private record WriteContext(
      Settings settings,
      KeyResolver keyResolver,
      @Nullable Map<String, List<String>> comments,
      Map<String, Object> boxed) {

    WriteContext(
        Settings settings, KeyResolver keyResolver, @Nullable Map<String, List<String>> comments) {
      this(settings, keyResolver, comments, new LinkedHashMap<>(4));
    }
  }
}
