package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comments;
import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.internal.MinMaxHandler.NumberResult;
import com.mrivanplays.annotationconfig.core.internal.MinMaxHandler.State;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class AnnotatedConfigResolver {

  public static Map<AnnotationHolder, Set<AnnotationType>> resolveAnnotations(
      Object annotatedClass, boolean reverseFields) {
    Map<AnnotationHolder, Set<AnnotationType>> annotationData = new TreeMap<>();
    AnnotationHolder CLASS_ANNOTATION_HOLDER = new AnnotationHolder();
    Class<?> theClass = annotatedClass.getClass();
    for (Annotation annotation : theClass.getAnnotations()) {
      Optional<AnnotationType> typeOpt = AnnotationType.match(annotation.annotationType());
      if (!typeOpt.isPresent()) {
        continue;
      }
      populate(CLASS_ANNOTATION_HOLDER, typeOpt.get(), annotationData);
    }

    List<Field> fields = Arrays.asList(theClass.getDeclaredFields());
    if (reverseFields) {
      Collections.reverse(fields);
    }

    int order = 1;
    for (Field field : fields) {
      if (field.getDeclaredAnnotations().length == 1) {
        Annotation anno = field.getDeclaredAnnotations()[0];
        if (AnnotationType.IGNORE.is(anno.annotationType())) {
          continue;
        }
      }
      AnnotationHolder holder = new AnnotationHolder(field, order);
      if (field.getDeclaredAnnotations().length == 0) {
        annotationData.put(holder, Collections.emptySet());
      } else {
        for (Annotation annotation : field.getDeclaredAnnotations()) {
          Optional<AnnotationType> typeOpt = AnnotationType.match(annotation.annotationType());
          if (!typeOpt.isPresent()) {
            continue;
          }
          populate(holder, typeOpt.get(), annotationData);
        }
        if (!annotationData.containsKey(holder)) {
          annotationData.put(holder, Collections.emptySet());
        }
      }
      order++;
    }
    return annotationData;
  }

  public static void dump(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> map,
      File file,
      CustomOptions options,
      String commentChar,
      ValueWriter valueWriter,
      boolean reverseFields) {
    try {
      file.createNewFile();
      try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
        toWriter(
            annotatedConfig,
            writer,
            map,
            commentChar,
            valueWriter,
            options,
            false,
            null,
            reverseFields);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void dump(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> map,
      Writer writerFeed,
      CustomOptions options,
      String commentChar,
      ValueWriter valueWriter,
      boolean reverseFields) {
    try (PrintWriter writer = new PrintWriter(writerFeed)) {
      toWriter(
          annotatedConfig,
          writer,
          map,
          commentChar,
          valueWriter,
          options,
          false,
          null,
          reverseFields);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void toWriter(
      Object annotatedConfig,
      PrintWriter writer,
      Map<AnnotationHolder, Set<AnnotationType>> map,
      String commentChar,
      ValueWriter valueWriter,
      CustomOptions options,
      boolean isSection,
      String sectionKey,
      boolean reverseFields)
      throws IOException {
    Map<String, Object> toWrite = null;
    if (isSection) {
      toWrite = new LinkedHashMap<>();
    }
    for (Map.Entry<AnnotationHolder, Set<AnnotationType>> entry : map.entrySet()) {
      AnnotationHolder holder = entry.getKey();
      if (holder.isClass()) {
        Class<?> theClass = annotatedConfig.getClass();
        for (AnnotationType type : entry.getValue()) {
          handleComments(type, null, theClass, commentChar, writer);
        }
        if (!isSection) {
          writer.append('\n');
        }
      } else {
        Field field = holder.getField();
        field.setAccessible(true);
        String keyName = field.getName();
        boolean configObject = false;
        for (AnnotationType type : entry.getValue()) {
          if (!isSection) {
            handleComments(type, field, null, commentChar, writer);
          }
          if (type.is(AnnotationType.KEY)) {
            keyName = field.getDeclaredAnnotation(Key.class).value();
          }
          if (type.is(AnnotationType.CONFIG_OBJECT)) {
            configObject = true;
            try {
              Object section = field.get(annotatedConfig);
              if (section == null) {
                throw new IllegalArgumentException(
                    "Section not initialized for field '" + field.getName() + "'");
              }
              toWriter(
                  section,
                  writer,
                  resolveAnnotations(section, reverseFields),
                  commentChar,
                  valueWriter,
                  options,
                  true,
                  keyName,
                  reverseFields);
            } catch (IllegalAccessException e) {
              throw new IllegalArgumentException(
                  "Could not config object value; field became inaccessible");
            }
          }
        }
        if (configObject) {
          continue;
        }
        try {
          Object defaultsToValueObject = field.get(annotatedConfig);
          if (defaultsToValueObject == null) {
            throw new IllegalArgumentException(
                "No default value for field '" + field.getName() + "'");
          }
          defaultsToValueObject = getWriteValue(field, defaultsToValueObject);
          if (!isSection) {
            valueWriter.write(keyName, defaultsToValueObject, writer, options, false);
          } else {
            toWrite.put(keyName, defaultsToValueObject);
          }
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException("lost access to field '" + field.getName() + "'");
        }
      }
    }

    if (isSection) {
      valueWriter.write(sectionKey, toWrite, writer, options, false);
    }
  }

  private static class WriteData {

    private List<String> comments;
    private Map<String, Object> writeData;

    WriteData(List<String> comments, Map<String, Object> writeData) {
      this.comments = comments;
      this.writeData = writeData;
    }

    public List<String> getComments() {
      return comments;
    }

    public Map<String, Object> getWriteData() {
      return writeData;
    }
  }

  private static WriteData getWriteData(
      Object baseClass,
      Field baseField,
      String baseKey,
      boolean reverseFields,
      boolean configObject)
      throws IllegalAccessException {
    Object value = baseField.get(baseClass);
    if (value == null) {
      throw new IllegalArgumentException(
          "No default value for field '" + baseField.getName() + "'");
    }
    List<String> comments = new ArrayList<>();

    if (configObject) {
      Map<AnnotationHolder, Set<AnnotationType>> annotationData =
          resolveAnnotations(value, reverseFields);

      Map<String, Object> objectWriteData = new LinkedHashMap<>();

      for (Map.Entry<AnnotationHolder, Set<AnnotationType>> entry : annotationData.entrySet()) {
        AnnotationHolder holder = entry.getKey();
        if (holder.isClass()) {
          if (entry.getValue().size() == 1) {
            AnnotationType first = entry.getValue().stream().findFirst().orElse(null);
            comments.addAll(getComments(first, null, value.getClass()));
          } else if (entry.getValue().size() > 1) {
            // very unlikely though but lets be on the safe side
            for (AnnotationType type : entry.getValue()) {
              List<String> typeComments = getComments(type, null, value.getClass());
              if (!typeComments.isEmpty()) {
                comments.addAll(typeComments);
              }
            }
          }
        } else {
          Field field = holder.getField();
          field.setAccessible(true);
          String key = field.getName();
          boolean confObject = false;
          for (AnnotationType type : entry.getValue()) {
            if (type.is(AnnotationType.KEY)) {
              key = field.getDeclaredAnnotation(Key.class).value();
            }
            if (type.is(AnnotationType.CONFIG_OBJECT)) {
              // ah shit, here we go again
              confObject = true;
              try {
                Object section = field.get(value);
                if (section == null) {
                  throw new IllegalArgumentException(
                      "Section not initialized for field '" + field.getName() + "'");
                }

                WriteData data = getWriteData(value, field, key, reverseFields, true);
                objectWriteData.put(key, data.getWriteData());
              } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(
                    "Could not config object value; field became inaccessible");
              }
            }
          }
          if (confObject) {
            continue;
          }
          Object defaultValue = field.get(value);
          if (defaultValue == null) {
            throw new IllegalArgumentException(
                "No default value for field '" + field.getName() + "'");
          }
          defaultValue = getWriteValue(field, defaultValue);
          objectWriteData.put(key, defaultValue);
        }
      }
      return new WriteData(comments, Collections.singletonMap(baseKey, objectWriteData));
    } else {
      Annotation[] annos = baseField.getDeclaredAnnotations();
      if (annos.length > 0) {
        for (Annotation anno : annos) {
          Optional<AnnotationType> typeOpt = AnnotationType.match(anno.annotationType());
          if (!typeOpt.isPresent()) {
            continue;
          }
          AnnotationType type = typeOpt.get();
          List<String> annoComment = getComments(type, baseField, null);
          if (!annoComment.isEmpty()) {
            comments.addAll(annoComment);
          }
        }
      }
      value = getWriteValue(baseField, value);
      return new WriteData(comments, Collections.singletonMap(baseKey, value));
    }
  }

  private static Object getWriteValue(Field baseField, Object value) {
    Optional<FieldTypeSerializer<?>> serializerOpt =
        SerializerRegistry.INSTANCE.getSerializer(baseField.getType());
    FieldTypeSerializer serializer;
    if (serializerOpt.isPresent()) {
      serializer = serializerOpt.get();
    } else {
      serializer = SerializerRegistry.INSTANCE.getDefaultSerializer();
    }
    DataObject serialized = serializer.serialize(value, baseField);
    if (serialized == null) {
      throw new NullPointerException(
          "Expected DataObject, got null ; Field: "
              + baseField.getName()
              + " ; Field type: "
              + baseField.getType().getName());
    }
    if (serialized.isSingleValue()) { // single value includes list
      value = serialized.getAsObject();
    } else {
      value = serialized.getAsMap();
    }
    return value;
  }

  public static void setFields(
      Object annotatedConfig,
      Map<String, Object> values,
      Map<AnnotationHolder, Set<AnnotationType>> map,
      NullReadHandleOption nullReadHandler,
      CustomOptions options,
      boolean reverseFields) {
    for (Map.Entry<AnnotationHolder, Set<AnnotationType>> entry : map.entrySet()) {
      AnnotationHolder holder = entry.getKey();
      if (holder.isClass()) {
        continue;
      }
      Field field = holder.getField();
      field.setAccessible(true);
      String keyName = field.getName();
      boolean configObject = false;
      Object section = null;
      NumberResult min = NumberResult.stateOnly(State.START);
      NumberResult max = NumberResult.stateOnly(State.START);
      for (AnnotationType type : entry.getValue()) {
        if (type.is(AnnotationType.KEY)) {
          keyName = field.getDeclaredAnnotation(Key.class).value();
        }
        if (type.is(AnnotationType.MIN)) {
          min = MinMaxHandler.getNumber(field.getDeclaredAnnotation(Min.class));
        }
        if (type.is(AnnotationType.MAX)) {
          max = MinMaxHandler.getNumber(field.getDeclaredAnnotation(Max.class));
        }
        if (type.is(AnnotationType.CONFIG_OBJECT)) {
          configObject = true;
          try {
            section = field.get(annotatedConfig);
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                "Could not get config object from annotated config '"
                    + annotatedConfig.getClass().getSimpleName()
                    + "'");
          }
        }
      }
      Object value = values.get(keyName);
      if (value == null) {
        continue;
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
        setFields(
            section,
            (Map<String, Object>) value,
            resolveAnnotations(section, reverseFields),
            nullReadHandler,
            options,
            reverseFields);
        continue;
      }
      handleFieldSet(annotatedConfig, field, options, nullReadHandler, value, min, max, keyName);
    }
  }

  private static void handleFieldSet(
      Object annotatedConfig,
      Field field,
      CustomOptions options,
      NullReadHandleOption nullReadHandler,
      Object value,
      NumberResult min,
      NumberResult max,
      String keyName) {
    Class<?> fieldType = field.getType();
    Optional<FieldTypeSerializer<?>> serializerOpt =
        SerializerRegistry.INSTANCE.getSerializer(fieldType);
    FieldTypeSerializer<?> serializer;
    if (!serializerOpt.isPresent()) {
      serializer = SerializerRegistry.INSTANCE.getDefaultSerializer();
    } else {
      serializer = serializerOpt.get();
    }
    Object deserialized = serializer.deserialize(new DataObject(value), field);
    if (deserialized == null) {
      if (nullReadHandler == NullReadHandleOption.USE_DEFAULT_VALUE) {
        return;
      }
    }
    if (deserialized instanceof Number) {
      Number comparable = (Number) deserialized;
      State comparison = MinMaxHandler.compare(min, max, comparable);
      handleComparison(comparison, keyName, comparable, fieldType, min, max, Number.class);
    } else if (deserialized instanceof String) {
      String comparable = (String) deserialized;
      State comparison = MinMaxHandler.compare(min, max, comparable);
      handleComparison(comparison, keyName, comparable.length(), fieldType, min, max, String.class);
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
          if (!validator.validate(field.getAnnotation(type), deserialized, options, field)) {
            failed = true;
            error = validator.error();
            break;
          }
        }
      }
      if (error != null) {
        throw new RuntimeException(error);
      }
      // error wasn't thrown, so just silently skip if the checks failed
      if (failed) {
        return;
      }
    }
    try {
      field.set(annotatedConfig, deserialized);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(
          "Could not set a field's value ; field not accessible anymore");
    }
  }

  public static void setFields(
      Object annotatedConfig,
      Map<String, Object> values,
      Map<AnnotationHolder, Set<AnnotationType>> map,
      String commentChar,
      ValueWriter valueWriter,
      File file,
      CustomOptions options,
      NullReadHandleOption nullReadHandler,
      boolean shouldGenerateNonExistentFields,
      boolean reverseFields,
      boolean isSection,
      String sectionBaseName) {
    for (Map.Entry<AnnotationHolder, Set<AnnotationType>> entry : map.entrySet()) {
      AnnotationHolder holder = entry.getKey();
      if (holder.isClass()) {
        continue;
      }
      Field field = holder.getField();
      field.setAccessible(true);
      String keyName = field.getName();
      boolean configObject = false;
      Object section = null;
      NumberResult min = NumberResult.stateOnly(State.START);
      NumberResult max = NumberResult.stateOnly(State.START);
      for (AnnotationType type : entry.getValue()) {
        if (type.is(AnnotationType.KEY)) {
          keyName = field.getDeclaredAnnotation(Key.class).value();
        }
        if (type.is(AnnotationType.MIN)) {
          min = MinMaxHandler.getNumber(field.getDeclaredAnnotation(Min.class));
        }
        if (type.is(AnnotationType.MAX)) {
          max = MinMaxHandler.getNumber(field.getDeclaredAnnotation(Max.class));
        }
        if (type.is(AnnotationType.CONFIG_OBJECT)) {
          configObject = true;
          try {
            section = field.get(annotatedConfig);
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                "Could not get config object from annotated config '"
                    + annotatedConfig.getClass().getSimpleName()
                    + "'");
          }
        }
      }
      Object value = values.get(keyName);
      if (value == null) {
        if (shouldGenerateNonExistentFields) {
          try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
            WriteData writeData =
                getWriteData(annotatedConfig, field, keyName, reverseFields, configObject);
            for (String comment : writeData.getComments()) {
              writer.println(commentChar + comment);
            }
            if (isSection) {
              Map<String, Object> toWrite = new LinkedHashMap<>();
              toWrite.put(sectionBaseName, writeData.getWriteData());
              valueWriter.write(keyName, toWrite, writer, options, true);
            } else {
              Map<String, Object> toWrite = writeData.getWriteData();
              valueWriter.write(keyName, toWrite.get(keyName), writer, options, false);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Field became inaccessible");
          }
        }
        continue;
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
        setFields(
            section,
            (Map<String, Object>) value,
            resolveAnnotations(section, reverseFields),
            commentChar,
            valueWriter,
            file,
            options,
            nullReadHandler,
            shouldGenerateNonExistentFields,
            reverseFields,
            true,
            keyName);
        continue;
      }
      handleFieldSet(annotatedConfig, field, options, nullReadHandler, value, min, max, keyName);
    }
  }

  private static void handleComparison(
      State comparison,
      String key,
      Number compared,
      Class<?> fieldType,
      NumberResult minResult,
      NumberResult maxResult,
      Class<?> comparedType) {
    if (comparison != State.ALRIGHT) {
      if (comparison == State.INVALID_MIN) {
        throw new IllegalArgumentException(
            fieldType.getName()
                + " "
                + key
                + " ; invalid @Min specified - it should implement annotation member ( e.g @Min(minInt = -22) )");
      }
      if (comparison == State.MORE_THAN_ONE_MIN) {
        throw new IllegalArgumentException(
            fieldType.getName()
                + " "
                + key
                + " ; invalid @Min specified - it should implement only one annotation member");
      }
      if (comparison == State.INVALID_MAX) {
        throw new IllegalArgumentException(
            fieldType.getName()
                + " "
                + key
                + " ; invalid @Max specified - it should implement annotation member ( e.g. @Max(maxInt = 3) )");
      }
      if (comparison == State.MORE_THAN_ONE_MAX) {
        throw new IllegalArgumentException(
            fieldType.getName()
                + " "
                + key
                + " ; invalid @Max specified - it should implement only one annotation member");
      }
      if (comparison == State.UNDER) {
        Number min = minResult.getNumber().get();
        String message;
        if (comparedType.isAssignableFrom(String.class)) {
          message =
              fieldType.getName()
                  + " ; deserialized String's length is under the minimal length allowed ("
                  + min
                  + ") ; string length: "
                  + compared;
        } else {
          message =
              fieldType.getName()
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
              fieldType.getName()
                  + " ; deserialized String's length is above the maximum length allowed ("
                  + max
                  + ") ; string length: "
                  + compared;
        } else {
          message =
              fieldType.getName()
                  + " ; deserialized value is above the maximum allowed ("
                  + max
                  + ") ; number: "
                  + compared;
        }
        throw new IllegalArgumentException(message);
      }
    }
  }

  private static List<String> getComments(AnnotationType type, Field field, Class<?> aClass) {
    if (!type.is(AnnotationType.COMMENT) && !type.is(AnnotationType.COMMENTS)) {
      return Collections.emptyList();
    }
    List<String> ret = new ArrayList<>();
    if (type.is(AnnotationType.COMMENT)) {
      ret.add(getAnnotation(field, aClass, Comment.class).value());
    }
    if (type.is(AnnotationType.COMMENTS)) {
      for (Comment comment : getAnnotation(field, aClass, Comments.class).value()) {
        ret.add(comment.value());
      }
    }
    return ret;
  }

  private static void handleComments(
      AnnotationType type, Field field, Class<?> aClass, String commentChar, PrintWriter writer) {
    List<String> comments = getComments(type, field, aClass);
    if (!comments.isEmpty()) {
      for (String comment : comments) {
        writer.println(commentChar + comment);
      }
    }
  }

  private static <T extends Annotation> T getAnnotation(
      Field field, Class<?> theClass, Class<T> annotationType) {
    if (field != null) {
      return field.getDeclaredAnnotation(annotationType);
    } else {
      return theClass.getDeclaredAnnotation(annotationType);
    }
  }

  private static void populate(
      AnnotationHolder holder,
      AnnotationType putData,
      Map<AnnotationHolder, Set<AnnotationType>> map) {
    if (map.containsKey(holder)) {
      Set<AnnotationType> data = map.get(holder);
      data.add(putData);
      map.replace(holder, data);
    } else {
      Set<AnnotationType> data = new TreeSet<>();
      data.add(putData);
      map.put(holder, data);
    }
  }

  private AnnotatedConfigResolver() {
    throw new IllegalArgumentException("Initialisation of utility-type class.");
  }
}
