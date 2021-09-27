package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.ValueWriter;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comments;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import com.mrivanplays.annotationconfig.core.serialization.registry.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.serialization.registry.primitive.PrimitiveSerializersRegistrar;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class AnnotatedConfigResolver {

  static {
    PrimitiveSerializersRegistrar.register();
  }

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
      }
      order++;
    }
    return annotationData;
  }

  public static void dump(
      Object annotatedConfig,
      Map<AnnotationHolder, Set<AnnotationType>> map,
      File file,
      String commentChar,
      ValueWriter valueWriter,
      boolean reverseFields) {
    try {
      file.createNewFile();
      try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
        toWriter(
            annotatedConfig, writer, map, commentChar, valueWriter, false, null, reverseFields);
      }
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
      boolean isSection,
      String sectionKey,
      boolean reverseFields)
      throws IOException {
    Map<String, Object> toWrite = null;
    if (isSection) {
      toWrite = new HashMap<>();
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
          Class<?> fieldType = field.getType();
          Optional<FieldTypeSerializer<?>> serializerOpt =
              SerializerRegistry.INSTANCE.getSerializer(fieldType);
          Object defaultsToValueObject = field.get(annotatedConfig);
          if (defaultsToValueObject == null) {
            throw new IllegalArgumentException(
                "No default value for field '" + field.getName() + "'");
          }
          if (serializerOpt.isPresent()) {
            FieldTypeSerializer serializer = serializerOpt.get();
            SerializedObject serialized = serializer.serialize(defaultsToValueObject, field);
            if (serialized == null) {
              throw new NullPointerException(
                  "Expected SerializedObject, got null ; Field: "
                      + field.getName()
                      + " ; Field type: "
                      + field.getType().getName());
            }
            switch (serialized.getPresentValue()) {
              case LIST:
                defaultsToValueObject = serialized.getSerializedList();
                break;
              case MAP:
                defaultsToValueObject = serialized.getSerializedMap();
                break;
              case OBJECT:
                defaultsToValueObject = serialized.getSerializedObject();
                break;
              default:
                throw new IllegalArgumentException(
                    "Illegal serialized.getPresentValue() (AnnotatedConfigResolver#toWrite)");
            }
          }
          if (!isSection) {
            valueWriter.write(keyName, defaultsToValueObject, writer, false);
          } else {
            toWrite.put(keyName, defaultsToValueObject);
          }
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException("lost access to field '" + field.getName() + "'");
        }
      }
    }

    if (isSection) {
      valueWriter.write(sectionKey, toWrite, writer, false);
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

      Map<String, Object> objectWriteData = new HashMap<>();

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
          Optional<FieldTypeSerializer<?>> serializerOpt =
              SerializerRegistry.INSTANCE.getSerializer(field.getType());
          Object defaultValue = field.get(value);
          if (defaultValue == null) {
            throw new IllegalArgumentException(
                "No default value for field '" + field.getName() + "'");
          }
          if (serializerOpt.isPresent()) {
            FieldTypeSerializer serializer = serializerOpt.get();
            SerializedObject serialized = serializer.serialize(defaultValue, field);
            if (serialized == null) {
              throw new NullPointerException(
                  "Expected SerializedObject, got null ; Field: "
                      + field.getName()
                      + " ; Field type: "
                      + field.getType().getName());
            }
            switch (serialized.getPresentValue()) {
              case MAP:
                defaultValue = serialized.getSerializedMap();
                break;
              case LIST:
                defaultValue = serialized.getSerializedList();
                break;
              case OBJECT:
                defaultValue = serialized.getSerializedObject();
                break;
              default:
                throw new IllegalArgumentException(
                    "Illegal serialized.getPresentValue() (AnnotatedConfigResolver#getWriteData)");
            }
          }
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
      Optional<FieldTypeSerializer<?>> serializerOpt =
          SerializerRegistry.INSTANCE.getSerializer(baseField.getType());
      if (serializerOpt.isPresent()) {
        FieldTypeSerializer serializer = serializerOpt.get();
        SerializedObject serialized = serializer.serialize(value, baseField);
        if (serialized == null) {
          throw new NullPointerException(
              "Expected SerializedObject, got null ; Field: "
                  + baseField.getName()
                  + " ; Field type: "
                  + baseField.getType().getName());
        }
        switch (serialized.getPresentValue()) {
          case MAP:
            value = serialized.getSerializedMap();
            break;
          case LIST:
            value = serialized.getSerializedList();
            break;
          case OBJECT:
            value = serialized.getSerializedObject();
            break;
          default:
            throw new IllegalArgumentException(
                "Illegal serialized.getPresentValue() (AnnotatedConfigResolver#getWriteData)");
        }
      }
      return new WriteData(comments, Collections.singletonMap(baseKey, value));
    }
  }

  public static void setFields(
      Object annotatedConfig,
      Map<String, Object> values,
      Map<AnnotationHolder, Set<AnnotationType>> map,
      String commentChar,
      ValueWriter valueWriter,
      File file,
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
      Number min = MinMaxHandler.START;
      Number max = MinMaxHandler.START;
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
      Class<?> fieldType = field.getType();
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
              Map<String, Object> toWrite = new HashMap<>();
              toWrite.put(sectionBaseName, writeData.getWriteData());
              valueWriter.write(keyName, toWrite, writer, true);
            } else {
              Map<String, Object> toWrite = writeData.getWriteData();
              valueWriter.write(keyName, toWrite.get(keyName), writer, false);
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
            shouldGenerateNonExistentFields,
            reverseFields,
            true,
            keyName);
        continue;
      }
      Optional<FieldTypeSerializer<?>> serializerOpt =
          SerializerRegistry.INSTANCE.getSerializer(fieldType);
      if (!serializerOpt.isPresent()) {
        throw new IllegalArgumentException(
            "Cannot find serializer for field '"
                + field.getName()
                + "', field type: "
                + fieldType.getSimpleName());
      }
      FieldTypeSerializer<?> serializer = serializerOpt.get();
      Object deserialized = serializer.deserialize(new ConfigDataObject(value), field);
      if (deserialized instanceof Number) {
        Number comparable = (Number) deserialized;
        byte comparison = MinMaxHandler.compare(min, max, comparable);
        handleComparison(comparison, comparable, fieldType, min, max, Number.class);
      }
      if (deserialized instanceof String) {
        String comparable = (String) deserialized;
        byte comparison = MinMaxHandler.compare(min, max, comparable);
        handleComparison(comparison, comparable.length(), fieldType, min, max, String.class);
      }
      try {
        field.set(annotatedConfig, deserialized);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException(
            "Could not set a field's value ; field not accessible anymore");
      }
    }
  }

  private static void handleComparison(
      byte comparison,
      Number compared,
      Class<?> fieldType,
      Number min,
      Number max,
      Class<?> comparedType) {
    if (comparison != MinMaxHandler.ALRIGHT) {
      if (comparison == MinMaxHandler.INVALID_MIN) {
        throw new IllegalArgumentException(
            fieldType.getName()
                + " ; invalid @Min specified - it should implement annotation member ( e.g @Min(minInt = -22) )");
      }
      if (comparison == MinMaxHandler.MORE_THAN_ONE_MIN) {
        throw new IllegalArgumentException(
            fieldType.getName()
                + " ; invalid @Min specified - it should implement only one annotation member");
      }
      if (comparison == MinMaxHandler.INVALID_MAX) {
        throw new IllegalArgumentException(
            fieldType.getName()
                + " ; invalid @Max specified - it should implement annotation member ( e.g. @Max(maxInt = 3) )");
      }
      if (comparison == MinMaxHandler.MORE_THAN_ONE_MAX) {
        throw new IllegalArgumentException(
            fieldType.getName()
                + " ; invalid @Max specified - it should implement only one annotation member");
      }
      if (comparison == MinMaxHandler.UNDER_THE_MIN) {
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
      if (comparison == MinMaxHandler.ABOVE_THE_MAX) {
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
}
