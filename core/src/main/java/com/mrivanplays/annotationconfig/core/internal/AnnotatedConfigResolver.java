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

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.annotations.custom.ValidationResponse;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.internal.MinMaxHandler.NumberResult;
import com.mrivanplays.annotationconfig.core.internal.MinMaxHandler.State;
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * @hidden
 */
@SuppressWarnings({"unchecked", "rawtypes", "ConstantValue", "OptionalGetWithoutIsPresent"})
@NullMarked
public final class AnnotatedConfigResolver {

  public static Set<AnnotationHolder> resolveAnnotationHolders(
      Object annotatedClass, Settings settings) {
    boolean findParentFields = settings.get(ACDefaultSettings.FIND_PARENT_FIELDS).orElseThrow();
    boolean reverseFields = settings.get(ACDefaultSettings.SHOULD_REVERSE_FIELDS).orElseThrow();

    Set<AnnotationHolder> ret = new TreeSet<>();
    Class<?> theClass = annotatedClass.getClass();
    if (theClass.getDeclaredAnnotations().length > 0) {
      ret.add(new AnnotationHolder(theClass));
    }
    List<ParentData> parentData = findParentFields ? getParentData(theClass) : null;
    List<Field> fields = new ArrayList<>(Arrays.asList(theClass.getDeclaredFields()));
    if (parentData != null) {
      for (ParentData parent : parentData) {
        if (parent.annotations() != null) {
          ret.add(new AnnotationHolder(parent.parent()));
        }
        if (parent.fields() != null) {
          fields.addAll(parent.fields());
        }
      }
    }

    if (reverseFields) {
      Collections.reverse(fields);
    }

    int order = 1;
    for (Field field : fields) {
      if (AnnotationUtils.isIgnored(field)) {
        continue;
      }
      ret.add(new AnnotationHolder(field, order));
      order++;
    }
    return ret;
  }

  private static List<ParentData> getParentData(Class<?> theClass) {
    List<ParentData> ret = new ArrayList<>();
    Class<?> superClass = null;
    while (true) {
      if (superClass == null) {
        superClass = theClass.getSuperclass();
      } else {
        superClass = superClass.getSuperclass();
      }
      if (superClass == null
          || superClass.getCanonicalName().equalsIgnoreCase("java.lang.Object")) {
        break;
      }
      List<Field> currentFields = null;
      if (superClass.getDeclaredFields().length > 0) {
        currentFields = Arrays.asList(superClass.getDeclaredFields());
      }
      Annotation[] currentAnnotations = null;
      if (superClass.getAnnotations().length > 0) {
        currentAnnotations = superClass.getAnnotations();
      }
      ret.add(new ParentData(superClass, currentFields, currentAnnotations));
    }
    return ret;
  }

  @NullUnmarked
  private record ParentData(
      Class<?> parent, @Nullable List<Field> fields, @Nullable Annotation[] annotations) {}

  public static void dump(
      Object annotatedConfig,
      Set<AnnotationHolder> holders,
      File file,
      Settings settings,
      String commentChar,
      ValueWriter valueWriter) {
    try {
      file.createNewFile();
      try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
        toWriter(annotatedConfig, writer, holders, commentChar, valueWriter, settings);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void dump(
      Object annotatedConfig,
      Set<AnnotationHolder> holders,
      Path path,
      Settings settings,
      String commentChar,
      ValueWriter valueWriter) {
    try {
      Files.deleteIfExists(path);
      Files.createFile(path);
      try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
        toWriter(annotatedConfig, writer, holders, commentChar, valueWriter, settings);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    } catch (IOException e) {
      throw new RuntimeException(e);
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
        if (commentChar != null && !comments.isEmpty()) {
          for (String comment : comments) {
            writer.println(commentChar + comment);
          }
        }
        writer.append('\n');
        continue;
      }
      Field field = holder.getField();
      if (AnnotationUtils.isRawConfigAccess(field)) {
        rawConfigField = field;
        continue;
      }
      try {
        WriteData current = getWriteData(annotatedConfig, holder, settings);
        for (Map.Entry<String, Object> childDataWrite : current.getToWrite().entrySet()) {
          String childDataKey = childDataWrite.getKey();
          Object childDataValue = childDataWrite.getValue();
          combineMapToData(parentData, childDataKey, childDataValue);
        }
        parentData.getFieldComments().putAll(current.getFieldComments());
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
        rawConfigField.set(annotatedConfig, new DataObject(parentData.getToWrite(), true));
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException(
            "Could not set a field's value ; field not accessible anymore", e);
      }
    }
    valueWriter.write(parentData.getToWrite(), parentData.getFieldComments(), writer, settings);
  }

  private static class WriteData {

    private final List<String> classComments = new LinkedList<>();
    private final Map<String, Object> toWrite = new LinkedHashMap<>();
    private final Map<String, List<String>> fieldComments = new LinkedHashMap<>();

    public List<String> getClassComments() {
      return classComments;
    }

    public Map<String, Object> getToWrite() {
      return toWrite;
    }

    public Map<String, List<String>> getFieldComments() {
      return fieldComments;
    }
  }

  private static WriteData getWriteData(
      Object annotatedConfig, AnnotationHolder holder, Settings settings)
      throws IllegalAccessException {
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    WriteData ret = new WriteData();
    if (holder.isClass()) {
      ret.getClassComments()
          .addAll(
              AnnotationUtils.getComments(
                  AnnotationAccessor.createFromClass(annotatedConfig.getClass()), true));
      return ret;
    }
    Field field = holder.getField();
    field.setAccessible(true);
    String keyName = AnnotationUtils.getKey(field);
    List<String> comments =
        AnnotationUtils.getComments(AnnotationAccessor.createFromField(field), true);
    boolean configObject = AnnotationUtils.isConfigObject(field);
    Character multilineCharacter = null;
    if (field.isAnnotationPresent(Multiline.class)) {
      multilineCharacter = field.getDeclaredAnnotation(Multiline.class).value();
    }
    if (configObject) {
      Object cfgObject = field.get(annotatedConfig);
      Set<AnnotationHolder> annotations = resolveAnnotationHolders(cfgObject, settings);

      WriteData combinedData = new WriteData();
      for (AnnotationHolder e1 : annotations) {
        WriteData childData = getWriteData(cfgObject, e1, settings);
        combinedData.getClassComments().addAll(childData.getClassComments());
        for (Map.Entry<String, Object> childDataWrite : childData.getToWrite().entrySet()) {
          String childDataKey = childDataWrite.getKey();
          Object childDataValue = childDataWrite.getValue();
          combineMapToData(combinedData, childDataKey, childDataValue);
        }
        combinedData.getFieldComments().putAll(childData.getFieldComments());
      }
      ret.getToWrite().put(keyName, combinedData.getToWrite());
      for (Map.Entry<String, List<String>> fieldComment :
          combinedData.getFieldComments().entrySet()) {
        ret.getFieldComments().put(keyName + "." + fieldComment.getKey(), fieldComment.getValue());
      }

      if (!comments.isEmpty()) {
        ret.getFieldComments().put(keyName, comments);
      } else {
        if (!combinedData.getClassComments().isEmpty()) {
          ret.getFieldComments().put(keyName, combinedData.getClassComments());
        }
      }
      return ret;
    }
    Object defaultsToValueObject = field.get(annotatedConfig);
    if (defaultsToValueObject == null) {
      throw new IllegalArgumentException("No default value for field '" + field.getName() + "'");
    }
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
    if (serialized.isSingleValue()) { // single value includes list
      defaultsToValueObject = serialized.getAsObject();
    } else {
      defaultsToValueObject = serialized.getAsMap();
    }
    // check for multiline string
    if (multilineCharacter != null) {
      if (!(defaultsToValueObject instanceof String)) {
        throw new IllegalArgumentException("@Multiline put on a value which is not a String!");
      }
      defaultsToValueObject =
          new MultilineString((String) defaultsToValueObject, multilineCharacter);
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
            if (newSerialized.isSingleValue()) {
              defaultsToValueObject = newSerialized.getAsObject();
            } else {
              defaultsToValueObject = newSerialized.getAsMap();
            }
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
    String keyToWrite = keyName;
    Object valueToWrite = defaultsToValueObject;
    for (Map.Entry<String, Object> dummyEntry : dummyValues.entrySet()) {
      keyToWrite = dummyEntry.getKey();
      valueToWrite = dummyEntry.getValue();
    }
    combineMapToData(ret, keyToWrite, valueToWrite);

    ret.getFieldComments().put(keyName, comments);
    return ret;
  }

  private static void combineMapToData(
      WriteData combinedData, String childDataKey, Object childDataValue) {
    if (combinedData.getToWrite().containsKey(childDataKey)) {
      if (childDataValue instanceof Map) {
        Map<String, Object> map2 = (Map<String, Object>) childDataValue;
        Object stored = combinedData.getToWrite().get(childDataKey);
        if (!(stored instanceof Map)) {
          throw new IllegalArgumentException(
              "Something's wrong I can feel it ; check your annotated config.");
        }
        Map<String, Object> map1 = (Map<String, Object>) stored;
        MapUtils.populateFirst(map1, map2);
        combinedData.getToWrite().replace(childDataKey, map1);
      } else {
        throw new IllegalArgumentException("Duplicate key");
      }
    } else {
      combinedData.getToWrite().put(childDataKey, childDataValue);
    }
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
        try {
          field.set(annotatedConfig, new DataObject(values, true));
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException(
              "Could not set a field's value ; field not accessible anymore", e);
        }
        continue;
      }
      String keyName = AnnotationUtils.getKey(field);
      boolean configObject = false;
      Object section = null;
      if (AnnotationUtils.isConfigObject(field)) {
        configObject = true;
        try {
          section = field.get(annotatedConfig);
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException(
              "Could not get config object from annotated config '"
                  + annotatedConfig.getClass().getSimpleName()
                  + "'",
              e);
        }
      }
      NumberResult min = NumberResult.stateOnly(State.START);
      if (AnnotationUtils.hasMinimumRequirement(field)) {
        min = MinMaxHandler.getNumber(field.getDeclaredAnnotation(Min.class));
      }
      NumberResult max = NumberResult.stateOnly(State.START);
      if (AnnotationUtils.hasMaximumRequirement(field)) {
        max = MinMaxHandler.getNumber(field.getDeclaredAnnotation(Max.class));
      }
      Object value = keyResolver.unbox(keyName, values);
      boolean thisMissingOption = false;
      if (value == null) {
        thisMissingOption = true;
        if (!missingOptions) {
          missingOptions = true;
        }
      }
      if (configObject) {
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
          if (deserialized == null && !thisMissingOption) {
            if (nullReadHandler == NullReadHandleOption.USE_DEFAULT_VALUE) {
              continue;
            }
          } else if (deserialized == null) {
            continue;
          }
          try {
            field.set(annotatedConfig, deserialized);
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                "Could not set a field's value ; field not accessible anymore", e);
          }
          continue;
        }
        boolean thMissing =
            setFields(
                section,
                (Map<String, Object>) value,
                resolveAnnotationHolders(section, settings),
                settings);
        if (thMissing && !missingOptions) {
          missingOptions = true;
        }
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
      if (deserialized == null && !thisMissingOption) {
        if (nullReadHandler == NullReadHandleOption.USE_DEFAULT_VALUE) {
          continue;
        }
      } else if (deserialized == null) {
        continue;
      }
      if (deserialized instanceof Number comparable) {
        State comparison = MinMaxHandler.compare(min, max, comparable);
        handleComparison(comparison, keyName, comparable, fieldType, min, max, Number.class);
      } else if (deserialized instanceof String comparable) {
        State comparison = MinMaxHandler.compare(min, max, comparable);
        handleComparison(
            comparison, keyName, comparable.length(), fieldType, min, max, String.class);
      } else {
        if (min.getState() != State.START) {
          throw new IllegalArgumentException("@Min annotation placed on invalid field type");
        }
        if (max.getState() != State.START) {
          throw new IllegalArgumentException("@Max annotation placed on invalid field type");
        }
      }
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
            //noinspection DataFlowIssue
            ValidationResponse validatorResponse =
                validator.validate(field.getAnnotation(type), deserialized, settings, field);
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
      try {
        field.set(annotatedConfig, deserialized);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException(
            "Could not set a field's value ; field not accessible anymore", e);
      }
    }
    return missingOptions;
  }

  private static void handleComparison(
      State comparison,
      String key,
      Number compared,
      Type fieldType,
      NumberResult minResult,
      NumberResult maxResult,
      Class<?> comparedType) {
    if (comparison != State.ALRIGHT) {
      if (comparison == State.INVALID_MIN) {
        throw new IllegalArgumentException(
            fieldType.getTypeName()
                + " "
                + key
                + " ; invalid @Min specified - it should implement annotation member ( e.g @Min(minInt = -22) )");
      }
      if (comparison == State.MORE_THAN_ONE_MIN) {
        throw new IllegalArgumentException(
            fieldType.getTypeName()
                + " "
                + key
                + " ; invalid @Min specified - it should implement only one annotation member");
      }
      if (comparison == State.INVALID_MAX) {
        throw new IllegalArgumentException(
            fieldType.getTypeName()
                + " "
                + key
                + " ; invalid @Max specified - it should implement annotation member ( e.g. @Max(maxInt = 3) )");
      }
      if (comparison == State.MORE_THAN_ONE_MAX) {
        throw new IllegalArgumentException(
            fieldType.getTypeName()
                + " "
                + key
                + " ; invalid @Max specified - it should implement only one annotation member");
      }
      if (comparison == State.UNDER) {
        Number min = minResult.getNumber().get();
        String message;
        if (comparedType.isAssignableFrom(String.class)) {
          message =
              fieldType.getTypeName()
                  + " ; deserialized String's length is under the minimal length allowed ("
                  + min
                  + ") ; string length: "
                  + compared;
        } else {
          message =
              fieldType.getTypeName()
                  + " ; deserialized value is under the minimal allowed ("
                  + min
                  + ") ; number: "
                  + compared;
        }
        throw new IllegalArgumentException(message);
      }
      if (comparison == State.ABOVE) {
        Number max = maxResult.getNumber().get();
        String message;
        if (comparedType.isAssignableFrom(String.class)) {
          message =
              fieldType.getTypeName()
                  + " ; deserialized String's length is above the maximum length allowed ("
                  + max
                  + ") ; string length: "
                  + compared;
        } else {
          message =
              fieldType.getTypeName()
                  + " ; deserialized value is above the maximum allowed ("
                  + max
                  + ") ; number: "
                  + compared;
        }
        throw new IllegalArgumentException(message);
      }
    }
  }

  private AnnotatedConfigResolver() {
    throw new IllegalArgumentException("Initialisation of utility-type class.");
  }
}
