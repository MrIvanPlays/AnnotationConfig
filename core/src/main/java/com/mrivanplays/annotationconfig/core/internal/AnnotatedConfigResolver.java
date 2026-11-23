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
import com.mrivanplays.annotationconfig.core.annotations.custom.ValidationResponse;
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
import com.mrivanplays.annotationconfig.core.utils.AnnotationUtils;
import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @hidden
 */
@SuppressWarnings({"unchecked", "rawtypes", "ConstantValue"})
@NullMarked
public final class AnnotatedConfigResolver {

  public static Set<AnnotationHolder> resolveAnnotationHolders(
      Object annotatedClass, Settings settings) {
    boolean findParentFields = settings.get(ACDefaultSettings.FIND_PARENT_FIELDS).orElseThrow();
    boolean reverseFields = settings.get(ACDefaultSettings.SHOULD_REVERSE_FIELDS).orElseThrow();
    boolean findStaticFields = settings.get(ACDefaultSettings.FIND_STATIC_FIELDS).orElseThrow();

    Set<AnnotationHolder> ret = new TreeSet<>();
    Class<?> theClass = annotatedClass.getClass();
    if (theClass.getDeclaredAnnotations().length > 0) {
      ret.add(new AnnotationHolder(theClass));
    }
    List<Field> fields = new ArrayList<>(Arrays.asList(theClass.getDeclaredFields()));
    if (findParentFields) {
      // compared by identity: getCanonicalName() is null for local and anonymous classes
      for (Class<?> parent = theClass.getSuperclass();
          parent != null && parent != Object.class;
          parent = parent.getSuperclass()) {
        if (parent.getAnnotations().length > 0) {
          ret.add(new AnnotationHolder(parent));
        }
        Collections.addAll(fields, parent.getDeclaredFields());
      }
    }

    if (reverseFields) {
      Collections.reverse(fields);
    }

    int order = 1;
    for (Field field : fields) {
      if (AnnotationUtils.isIgnored(field) && !theClass.isRecord()) {
        continue;
      }
      if (!findStaticFields && Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      ret.add(new AnnotationHolder(field, order));
      order++;
    }
    return ret;
  }

  public static void dump(
      Object annotatedConfig,
      Set<AnnotationHolder> holders,
      Path path,
      Settings settings,
      String commentChar,
      ValueWriter valueWriter) {
    Path absolutePath = path.toAbsolutePath();
    Path parent = absolutePath.getParent();
    Path tmp = null;

    try {
      tmp = Files.createTempFile(parent, absolutePath.getFileName().toString() + ".", ".tmp");

      try (PrintWriter writer =
          new PrintWriter(Files.newBufferedWriter(tmp, StandardCharsets.UTF_8))) {

        toWriter(annotatedConfig, writer, holders, commentChar, valueWriter, settings);

        writer.flush();
        if (writer.checkError()) {
          throw new IOException("Failed to write configuration");
        }
      }

      try {
        Files.move(
            tmp, absolutePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(tmp, absolutePath, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to dump configuration to " + path, e);
    } finally {
      if (tmp != null) {
        try {
          Files.deleteIfExists(tmp);
        } catch (IOException e) {
          // best effort clean-up ignored
        }
      }
    }
  }

  public static void dump(
      Object annotatedConfig,
      Set<AnnotationHolder> holders,
      Writer writerFeed,
      Settings settings,
      String commentChar,
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

  private static void toWriter(
      Object annotatedConfig,
      PrintWriter writer,
      Set<AnnotationHolder> holders,
      String commentChar,
      ValueWriter valueWriter,
      Settings settings)
      throws IOException {
    WriteData parentData = new WriteData();
    Field rawConfigField = null;
    for (AnnotationHolder holder : holders) {
      if (holder.isClass()) {
        List<String> comments =
            AnnotationUtils.getComments(
                AnnotationAccessor.createFromClass(holder.getClazz()), true);
        // the blank line separates the comments from the values; without comments (e.g. JSON,
        // which has none) it would be a stray leading line
        if (commentChar != null && !comments.isEmpty()) {
          for (String comment : comments) {
            writer.print(commentChar + comment);
            writer.append('\n');
          }
          writer.append('\n');
        }
        continue;
      }
      Field field = holder.getField();
      if (AnnotationUtils.isRawConfigAccess(field)) {
        rawConfigField = field;
        continue;
      }
      try {
        mergeInto(parentData, getWriteData(annotatedConfig, holder, settings));
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("A field became inaccessible", e);
      }
    }
    if (rawConfigField != null) {
      rawConfigField.setAccessible(true);
      if (!rawConfigField.getType().isAssignableFrom(DataObject.class)) {
        throw new IllegalArgumentException("@RawConfig on a field which is not DataObject");
      }
      try {
        rawConfigField.set(annotatedConfig, new DataObject(parentData.toWrite(), true));
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException(
            "Could not set a field's value ; field not accessible anymore", e);
      }
    }
    valueWriter.write(parentData.toWrite(), parentData.fieldComments(), writer, settings);
  }

  private record WriteData(
      List<String> classComments,
      Map<String, Object> toWrite,
      Map<String, List<String>> fieldComments) {

    WriteData() {
      this(new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }
  }

  /** Merges the values and field comments of {@code child} into {@code target}. */
  private static void mergeInto(WriteData target, WriteData child) {
    for (Map.Entry<String, Object> entry : child.toWrite().entrySet()) {
      combineMapToData(target, entry.getKey(), entry.getValue());
    }
    target.fieldComments().putAll(child.fieldComments());
  }

  /** The written form of a serialized value: the value itself, a list, or a map for a section. */
  private static Object unwrap(DataObject serialized) {
    return serialized.isSingleValue() ? serialized.getAsObject() : serialized.getAsMap();
  }

  private static WriteData getWriteData(
      Object annotatedConfig, AnnotationHolder holder, Settings settings)
      throws IllegalAccessException {
    WriteData ret = new WriteData();
    if (holder.isClass()) {
      // the holder's class, not annotatedConfig's: with FIND_PARENT_FIELDS a holder can be a
      // parent class, whose comments were replaced by a duplicate of the child's
      ret.classComments()
          .addAll(
              AnnotationUtils.getComments(
                  AnnotationAccessor.createFromClass(holder.getClazz()), true));
      return ret;
    }
    Field field = holder.getField();
    field.setAccessible(true);
    String keyName = AnnotationUtils.getKey(field);
    List<String> comments =
        AnnotationUtils.getComments(AnnotationAccessor.createFromField(field), true);
    if (AnnotationUtils.isConfigObject(field)) {
      Object cfgObject = field.get(annotatedConfig);
      if (cfgObject == null) {
        throw new IllegalArgumentException(
            "Non initialized config object '"
                + field.getName()
                + "' found in annotated config '"
                + annotatedConfig.getClass().getSimpleName()
                + "'");
      }
      WriteData combinedData = new WriteData();
      for (AnnotationHolder childHolder : resolveAnnotationHolders(cfgObject, settings)) {
        WriteData childData = getWriteData(cfgObject, childHolder, settings);
        combinedData.classComments().addAll(childData.classComments());
        mergeInto(combinedData, childData);
      }
      ret.toWrite().put(keyName, combinedData.toWrite());
      for (Map.Entry<String, List<String>> fieldComment : combinedData.fieldComments().entrySet()) {
        ret.fieldComments().put(keyName + "." + fieldComment.getKey(), fieldComment.getValue());
      }

      if (!comments.isEmpty()) {
        ret.fieldComments().put(keyName, comments);
      } else if (!combinedData.classComments().isEmpty()) {
        ret.fieldComments().put(keyName, combinedData.classComments());
      }
      return ret;
    }
    Object defaultsToValueObject = field.get(annotatedConfig);
    if (defaultsToValueObject == null) {
      throw new IllegalArgumentException("No default value for field '" + field.getName() + "'");
    }
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    FieldTypeSerializer serializer =
        serializerRegistry
            .getSerializer(field.getGenericType())
            .orElse(serializerRegistry.getDefaultSerializer());
    DataObject serialized =
        serializer.serialize(
            defaultsToValueObject,
            SerializationContext.of(
                field.getName(),
                defaultsToValueObject,
                field.getType(),
                field.getGenericType(),
                annotatedConfig,
                settings),
            AnnotationAccessor.createFromField(field));
    if (serialized == null) {
      throw new NullPointerException(
          "Expected DataObject, got null ; Field: "
              + field.getName()
              + " ; Field type: "
              + field.getType().getName());
    }
    if (serialized.isEmpty()) {
      return ret;
    }
    defaultsToValueObject = unwrap(serialized);
    // check for multiline string
    Multiline multiline = field.getDeclaredAnnotation(Multiline.class);
    if (multiline != null) {
      if (!(defaultsToValueObject instanceof String string)) {
        throw new IllegalArgumentException("@Multiline put on a value which is not a String!");
      }
      defaultsToValueObject = new MultilineString(string, multiline.value());
    }
    // check for custom annotations writeValue implementations
    CustomAnnotationRegistry caRegistry = CustomAnnotationRegistry.INSTANCE;
    if (!caRegistry.isEmpty()) {
      Class<?> foundWriteAnnotation = null;
      for (Annotation annotation : field.getDeclaredAnnotations()) {
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
            FieldTypeSerializer newSerializer =
                serializerRegistry
                    .getSerializer(val.getClass())
                    .orElse(serializerRegistry.getDefaultSerializer());
            DataObject newSerialized =
                newSerializer.serialize(
                    val,
                    SerializationContext.of(
                        field.getName(),
                        defaultsToValueObject,
                        field.getType(),
                        field.getGenericType(),
                        annotatedConfig,
                        settings),
                    AnnotationAccessor.createFromField(field));
            if (newSerialized == null) {
              throw new NullPointerException(
                  "Expected DataObject, but got null ; Field: "
                      + field.getName()
                      + " ; Field type: "
                      + val.getClass().getName());
            }
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
    Map<String, Object> dummyValues = new LinkedHashMap<>();
    KeyResolver keyResolver = settings.get(ACDefaultSettings.KEY_RESOLVER).orElseThrow();
    keyResolver.boxTo(keyName, defaultsToValueObject, dummyValues);
    if (dummyValues.size() != 1) {
      throw new IllegalArgumentException("Invalid key resolver.");
    }
    Map.Entry<String, Object> boxed = dummyValues.entrySet().iterator().next();
    combineMapToData(ret, boxed.getKey(), boxed.getValue());

    ret.fieldComments().put(keyName, comments);
    return ret;
  }

  private static void combineMapToData(WriteData combinedData, String key, Object value) {
    Map<String, Object> toWrite = combinedData.toWrite();
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
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    for (AnnotationHolder holder : holders) {
      if (holder.isClass()) {
        continue;
      }
      Field field = holder.getField();
      field.setAccessible(true);
      if (AnnotationUtils.isRawConfigAccess(field)) {
        if (!field.getType().isAssignableFrom(DataObject.class)) {
          throw new IllegalArgumentException("@RawConfig on a field which is not DataObject");
        }
        setField(field, annotatedConfig, new DataObject(values, true));
        continue;
      }
      String keyName = AnnotationUtils.getKey(field);
      Object value = keyResolver.unbox(keyName, values);
      boolean thisMissingOption = value == null;
      missingOptions |= thisMissingOption;
      if (AnnotationUtils.isConfigObject(field)) {
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
          Type fieldType = field.getGenericType();
          FieldTypeSerializer serializer =
              serializerRegistry
                  .getSerializer(fieldType)
                  .orElse(serializerRegistry.getDefaultSerializer());
          Object deserialized =
              serializer.deserialize(
                  new DataObject(value, true),
                  SerializationContext.of(
                      field.getName(),
                      section,
                      field.getType(),
                      fieldType,
                      annotatedConfig,
                      settings),
                  AnnotationAccessor.createFromField(field));
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
      Type fieldType = field.getGenericType();
      FieldTypeSerializer<?> serializer =
          serializerRegistry
              .getSerializer(fieldType)
              .orElse(serializerRegistry.getDefaultSerializer());
      Object deserialized;
      try {
        deserialized =
            serializer.deserialize(
                new DataObject(value, true),
                SerializationContext.fromField(field, annotatedConfig, settings),
                AnnotationAccessor.createFromField(field));
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Field became inaccessible", e);
      }
      if (skipNull(deserialized, thisMissingOption, nullReadHandler)) {
        continue;
      }
      MinMaxValidator.validate(field, keyName, deserialized);
      // check for custom annotations
      CustomAnnotationRegistry cARegistry = CustomAnnotationRegistry.INSTANCE;
      if (!cARegistry.isEmpty()) {
        Throwable error = null;
        boolean failed = false;
        for (Annotation annotation : field.getAnnotations()) {
          Class<? extends Annotation> type = annotation.annotationType();
          if (AnnotationType.match(type).isPresent()) {
            // do not handle any validation of our own annotations even if someone registered a
            // validator for them.
            continue;
          }
          Optional<AnnotationValidator<? extends Annotation>> validatorOpt =
              cARegistry.getValidator(type);
          if (validatorOpt.isPresent()) {
            AnnotationValidator validator = validatorOpt.get();
            ValidationResponse validatorResponse =
                validator.validate(annotation, deserialized, settings, field);
            if (validatorResponse.throwError() != null) {
              error = validatorResponse.throwError();
              break;
            }
            if (validatorResponse.shouldFailSilently()) {
              failed = validatorResponse.shouldFailSilently();
              break;
            }
            Runnable onSuccess = validatorResponse.onSuccess();
            if (onSuccess != null) {
              onSuccess.run();
            }
          }
        }
        if (error != null) {
          throw new RuntimeException(error);
        }
        // error wasn't thrown, so just silently skip if the checks failed
        if (failed) {
          continue;
        }
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
      throw new IllegalArgumentException(
          "Could not set a field's value ; field not accessible anymore", e);
    }
  }

  private AnnotatedConfigResolver() {
    throw new IllegalArgumentException("Initialisation of utility-type class.");
  }
}
