package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.ValueWriter;
import com.mrivanplays.annotationconfig.core.annotations.Key;
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
import java.util.TreeMap;

public final class AnnotatedConfigResolver {

  static {
    if (!PrimitiveSerializersRegistrar.hasRegistered()) {
      PrimitiveSerializersRegistrar.register();
    }
  }

  public static Map<AnnotationHolder, List<AnnotationType>> resolveAnnotations(
      Object annotatedClass, boolean reverseFields) {
    Map<AnnotationHolder, List<AnnotationType>> annotationData = new TreeMap<>();
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
        if (AnnotationType.RETRIEVE.is(anno.annotationType())) {
          continue;
        }
      }
      AnnotationHolder holder = new AnnotationHolder(field, order);
      if (field.getDeclaredAnnotations().length == 0) {
        annotationData.put(holder, Collections.emptyList());
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
      Map<AnnotationHolder, List<AnnotationType>> map,
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
      Map<AnnotationHolder, List<AnnotationType>> map,
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
    for (Map.Entry<AnnotationHolder, List<AnnotationType>> entry : map.entrySet()) {
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
            try {
              SerializedObject serialized = serializer.serialize(defaultsToValueObject, field);
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
            } catch (Exception e) {
              e.printStackTrace();
              continue;
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

  public static void setFields(
      Object annotatedConfig,
      Map<String, Object> values,
      Map<AnnotationHolder, List<AnnotationType>> map,
      String commentChar,
      ValueWriter valueWriter,
      File file,
      boolean shouldGenerateNonExistentFields,
      boolean reverseFields,
      boolean isSection,
      String sectionBaseName) {
    for (Map.Entry<AnnotationHolder, List<AnnotationType>> entry : map.entrySet()) {
      AnnotationHolder holder = entry.getKey();
      if (holder.isClass()) {
        continue;
      }
      Field field = holder.getField();
      field.setAccessible(true);
      String keyName = field.getName();
      boolean configObject = false;
      Object section = null;
      for (AnnotationType type : entry.getValue()) {
        if (type.is(AnnotationType.KEY)) {
          keyName = field.getDeclaredAnnotation(Key.class).value();
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
          if (configObject) {
            // config sections/objects we're not going to generate
            continue;
          }
          try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
            for (AnnotationType type : entry.getValue()) {
              handleComments(type, field, null, commentChar, writer);
            }
            Object def = field.get(annotatedConfig);
            if (isSection) {
              Map<String, Object> toWrite = new HashMap<>();
              Map<String, Object> val = new HashMap<>();
              values.put(keyName, def);
              toWrite.put(sectionBaseName, val);
              valueWriter.write(keyName, toWrite, writer, true);
            } else {
              writer.append('\n');
              valueWriter.write(keyName, def, writer, false);
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
      try {
        field.set(annotatedConfig, serializer.deserialize(new ConfigDataObject(value), field));
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException(
            "Could not set a field's value ; field not accessible anymore");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static void handleComments(
      AnnotationType type, Field field, Class<?> aClass, String commentChar, PrintWriter writer) {
    if (type.is(AnnotationType.COMMENT)) {
      writer.println(commentChar + getAnnotation(field, aClass, Comment.class).value());
    }
    if (type.is(AnnotationType.COMMENTS)) {
      for (Comment comment : getAnnotation(field, aClass, Comments.class).value()) {
        writer.println(commentChar + comment.value());
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
      Map<AnnotationHolder, List<AnnotationType>> map) {
    if (map.containsKey(holder)) {
      List<AnnotationType> data = map.get(holder);
      data.add(putData);
      map.replace(holder, data);
    } else {
      List<AnnotationType> data = new ArrayList<>();
      data.add(putData);
      map.put(holder, data);
    }
  }
}
