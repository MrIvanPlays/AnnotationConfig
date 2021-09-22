package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.AnnotationType;
import com.mrivanplays.annotationconfig.core.Comment;
import com.mrivanplays.annotationconfig.core.Comments;
import com.mrivanplays.annotationconfig.core.FieldTypeResolver;
import com.mrivanplays.annotationconfig.core.Key;
import com.mrivanplays.annotationconfig.core.TypeResolver;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class AnnotatedConfigResolver {

  public static Map<AnnotationHolder, List<AnnotationType>> resolveAnnotations(
      Object annotatedClass, boolean reverseFields) {
    Map<AnnotationHolder, List<AnnotationType>> annotationData = new TreeMap<>();
    AnnotationHolder CLASS_ANNOTATION_HOLDER = new AnnotationHolder();
    Class<?> theClass = annotatedClass.getClass();
    for (Annotation annotation : theClass.getAnnotations()) {
      AnnotationType annotationType = AnnotationType.match(annotation.annotationType());
      populate(CLASS_ANNOTATION_HOLDER, annotationType, annotationData);
    }

    List<Field> fields = Arrays.asList(theClass.getDeclaredFields());
    if (reverseFields) {
      Collections.reverse(fields);
    }

    int order = 1;
    for (Field field : fields) {
      if (field.getDeclaredAnnotations().length > 0) {
        AnnotationHolder holder = new AnnotationHolder(field, order);
        for (Annotation annotation : field.getDeclaredAnnotations()) {
          AnnotationType annotationType = AnnotationType.match(annotation.annotationType());
          populate(holder, annotationType, annotationData);
        }
        order++;
      }
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

  public interface ValueWriter {

    void write(String key, Object value, PrintWriter writer, boolean sectionExists)
        throws IOException;
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
        if (entry.getValue().size() == 1 && entry.getValue().get(0).is(AnnotationType.RETRIEVE)) {
          continue;
        }
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
          Object defaultsToValueObject = field.get(annotatedConfig);
          if (defaultsToValueObject == null) {
            throw new IllegalArgumentException(
                "No default value for field '" + field.getName() + "'");
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
      if (entry.getValue().size() == 1 && entry.getValue().get(0).is(AnnotationType.RETRIEVE)) {
        continue;
      }
      Field field = holder.getField();
      field.setAccessible(true);
      Class<? extends FieldTypeResolver> typeResolver = null;
      String keyName = field.getName();
      boolean configObject = false;
      Object section = null;
      for (AnnotationType type : entry.getValue()) {
        if (type.is(AnnotationType.TYPE_RESOLVER)) {
          typeResolver = field.getDeclaredAnnotation(TypeResolver.class).value();
        }
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
      if (typeResolver != null) {
        try {
          FieldTypeResolver resolver = typeResolver.getDeclaredConstructor().newInstance();
          if (!resolver.shouldResolve(fieldType)) {
            throw new IllegalArgumentException(
                "Invalid type resolver found for \"" + field.getName() + "\"");
          }
          Object resolvedValue = resolver.toType(value, field);
          field.set(annotatedConfig, resolvedValue);
        } catch (InstantiationException e) {
          throw new IllegalArgumentException("Could not construct a type resolver");
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException(
              "Could not construct a type resolver/set a field's value; field/constructor not accessible anymore");
        } catch (InvocationTargetException e) {
          throw new IllegalArgumentException(
              "Could not construct a type resolver; constructor rejected execution");
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException(
              "Cannot find a no args constructor for field type resolver \""
                  + field.getName()
                  + "\"");
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        // either I'm really tired or the only way we can check if the type is primitive type is
        // to do this spaghetti
        try {
          if (fieldType.isAssignableFrom(boolean.class)
              || fieldType.isAssignableFrom(Boolean.class)) {
            field.set(annotatedConfig, Boolean.parseBoolean(String.valueOf(value)));
          }
          if (fieldType.isAssignableFrom(String.class)) {
            field.set(annotatedConfig, String.valueOf(value));
          }
          if (fieldType.isAssignableFrom(byte.class) || fieldType.isAssignableFrom(Byte.class)) {
            field.set(annotatedConfig, Byte.valueOf(String.valueOf(value)));
          }
          if (fieldType.isAssignableFrom(int.class) || fieldType.isAssignableFrom(Integer.class)) {
            field.set(annotatedConfig, Integer.valueOf(String.valueOf(value)));
          }
          if (fieldType.isAssignableFrom(double.class)
              || fieldType.isAssignableFrom(Double.class)) {
            field.set(annotatedConfig, Double.valueOf(String.valueOf(value)));
          }
          if (fieldType.isAssignableFrom(float.class) || fieldType.isAssignableFrom(Float.class)) {
            field.set(annotatedConfig, Float.valueOf(String.valueOf(value)));
          }
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException(
              "Could not set field \""
                  + field.getName()
                  + "\" value; field not accessible anymore");
        }
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
