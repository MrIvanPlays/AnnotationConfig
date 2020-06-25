package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.AnnotationType;
import com.mrivanplays.annotationconfig.core.Comment;
import com.mrivanplays.annotationconfig.core.Comments;
import com.mrivanplays.annotationconfig.core.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.CustomAnnotationRegistry.AnnotationResolver;
import com.mrivanplays.annotationconfig.core.CustomAnnotationRegistry.AnnotationResolverContext;
import com.mrivanplays.annotationconfig.core.CustomAnnotationRegistry.AnnotationWriter;
import com.mrivanplays.annotationconfig.core.CustomAnnotationRegistry.AnnotationWriter.WriteFunction;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class AnnotatedConfigResolver {

  public static List<Map.Entry<AnnotationHolder, List<AnnotationType>>> resolveAnnotations(
      Object annotatedClass, CustomAnnotationRegistry annoRegistry, boolean reverseFields) {
    Map<AnnotationHolder, List<AnnotationType>> annotationData = new HashMap<>();
    AnnotationHolder CLASS_ANNOTATION_HOLDER = new AnnotationHolder();
    Class<?> theClass = annotatedClass.getClass();
    for (Annotation annotation : theClass.getAnnotations()) {
      AnnotationType annotationType =
          AnnotationType.match(annotation.annotationType(), annoRegistry);
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
          AnnotationType annotationType =
              AnnotationType.match(annotation.annotationType(), annoRegistry);
          populate(holder, annotationType, annotationData);
        }
        order++;
      }
    }
    return annotationData.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(Collectors.toCollection(LinkedList::new));
  }

  public static void dump(
      Object annotatedConfig,
      List<Map.Entry<AnnotationHolder, List<AnnotationType>>> map,
      File file,
      String commentChar,
      ValueWriter valueWriter,
      CustomAnnotationRegistry annotationRegistry,
      Class<?> configType,
      boolean reverseFields) {
    try {
      file.createNewFile();
      try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
        toWriter(
            annotatedConfig,
            writer,
            map,
            commentChar,
            annotationRegistry,
            configType,
            valueWriter,
            false,
            null,
            reverseFields);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public interface ValueWriter {

    void write(String key, Object value, PrintWriter writer) throws IOException;

    void writeCustom(Object value, PrintWriter writer, String annoName) throws IOException;
  }

  private static void toWriter(
      Object annotatedConfig,
      PrintWriter writer,
      List<Map.Entry<AnnotationHolder, List<AnnotationType>>> map,
      String commentChar,
      CustomAnnotationRegistry annotationRegistry,
      Class<?> configType,
      ValueWriter valueWriter,
      boolean isSection,
      String sectionKey,
      boolean reverseFields)
      throws IOException {
    Map<String, Object> toWrite = null;
    if (isSection) {
      toWrite = new HashMap<>();
    }
    for (Map.Entry<AnnotationHolder, List<AnnotationType>> entry : map) {
      AnnotationHolder holder = entry.getKey();
      if (holder.isClass()) {
        Class<?> theClass = annotatedConfig.getClass();
        for (AnnotationType type : entry.getValue()) {
          if (type.is(AnnotationType.COMMENT)) {
            writer.println(commentChar + theClass.getDeclaredAnnotation(Comment.class).value());
          }
          if (type.is(AnnotationType.COMMENTS)) {
            for (Comment comment : theClass.getDeclaredAnnotation(Comments.class).value()) {
              writer.println(commentChar + comment.value());
            }
          }
        }
        if (!isSection) {
          writer.append('\n');
        }
      } else {
        Field field = holder.getField();
        field.setAccessible(true);
        List<String> comments = new ArrayList<>();
        String keyName = field.getName();
        AnnotationResolver annoResolver = null;
        Class<? extends Annotation> customAnnotationType = null;
        boolean configObject = false;
        for (AnnotationType type : entry.getValue()) {
          if (type.is(AnnotationType.COMMENT)) {
            comments.add(field.getDeclaredAnnotation(Comment.class).value());
          }
          if (type.is(AnnotationType.COMMENTS)) {
            for (Comment comment : field.getDeclaredAnnotation(Comments.class).value()) {
              comments.add(comment.value());
            }
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
                  resolveAnnotations(section, annotationRegistry, reverseFields),
                  commentChar,
                  annotationRegistry,
                  configType,
                  valueWriter,
                  true,
                  keyName,
                  reverseFields);
            } catch (IllegalAccessException e) {
              throw new IllegalArgumentException(
                  "Could not config object value; field became inaccessible");
            }
          }
          if (AnnotationType.isCustom(type)) {
            if (annoResolver != null) {
              throw new IllegalArgumentException("A field can only have 1 custom annotation.");
            }
            annoResolver = annotationRegistry.registry().get(type);
            customAnnotationType = type.getRawType();
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
            for (String comment : comments) {
              writer.println(commentChar + comment);
            }
          }
          if (annoResolver == null) {
            if (!isSection) {
              valueWriter.write(keyName, defaultsToValueObject, writer);
            } else {
              toWrite.put(keyName, defaultsToValueObject);
            }
          } else {
            Annotation annotation = field.getDeclaredAnnotation(customAnnotationType);
            AnnotationWriter annotationWriter = new AnnotationWriter();
            AnnotationResolverContext context =
                new AnnotationResolverContext(
                    configType, field, annotatedConfig, defaultsToValueObject, keyName, isSection);
            annoResolver.write(annotationWriter, customAnnotationType.cast(annotation), context);
            for (Map.Entry<WriteFunction, Object> writeEntry :
                annotationWriter.toWrite().entrySet()) {
              WriteFunction func = writeEntry.getKey();
              Object written = writeEntry.getValue();
              switch (func) {
                case WRITE:
                  valueWriter.writeCustom(
                      written, writer, annotation.annotationType().getSimpleName());
                  break;
                case APPEND:
                  // but why this check when we have method only for appending character ?
                  // this is here to prevent people who use the non intended for api map
                  // I don't want to deal with abstracting the AnnotationWriter in order for this
                  // not
                  // to happen, but if I still do it then the people who are anti api would still
                  // find
                  // a way to bypass my little techniques. That's why this is here.
                  if (!(written instanceof Character)) {
                    throw new IllegalArgumentException(
                        "Cannot append other than char for config: annotation '"
                            + annotation.annotationType().getSimpleName()
                            + "'");
                  }
                  writer.append((char) written);
              }
            }
          }
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException("lost access to field '" + field.getName() + "'");
        }
      }
    }

    if (isSection) {
      valueWriter.write(sectionKey, toWrite, writer);
    }
  }

  public static void setFields(
      Object annotatedConfig,
      Map<String, Object> values,
      List<Map.Entry<AnnotationHolder, List<AnnotationType>>> map,
      CustomAnnotationRegistry annoRegistry,
      String commentChar,
      ValueWriter valueWriter,
      File file,
      boolean shouldGenerateNonExistentFields,
      boolean reverseFields) {
    for (Map.Entry<AnnotationHolder, List<AnnotationType>> entry : map) {
      AnnotationHolder holder = entry.getKey();
      if (holder.isClass()) {
        continue;
      }
      Field field = holder.getField();
      field.setAccessible(true);
      Class<? extends FieldTypeResolver> typeResolver = null;
      FieldTypeResolver constructedTypeResolver = null;
      String keyName = field.getName();
      boolean configObject = false;
      Object section = null;
      for (AnnotationType type : entry.getValue()) {
        if (AnnotationType.isCustom(type)) {
          if (annoRegistry == null) {
            throw new IllegalArgumentException(
                "Could not resolve custom annotation type '"
                    + type.getRawType().getSimpleName()
                    + "' ; registry not available.");
          }
          AnnotationResolver<? extends Annotation> resolver = annoRegistry.registry().get(type);
          if (resolver == null) {
            throw new IllegalArgumentException(
                "Could not resolve custom annotation type '"
                    + type.getRawType().getSimpleName()
                    + "' ; unregistered.");
          }
          if (constructedTypeResolver != null) {
            throw new IllegalArgumentException("A field can only have 1 custom annotation");
          }
          constructedTypeResolver = resolver.typeResolver().get();
        }
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
              if (type.is(AnnotationType.COMMENT)) {
                writer.println(commentChar + field.getDeclaredAnnotation(Comment.class).value());
              }
              if (type.is(AnnotationType.COMMENTS)) {
                for (Comment comment : field.getDeclaredAnnotation(Comments.class).value()) {
                  writer.println(commentChar + comment.value());
                }
              }
            }
            Object def = field.get(annotatedConfig);
            valueWriter.write(keyName, def, writer);
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
            resolveAnnotations(section, annoRegistry, reverseFields),
            annoRegistry,
            commentChar,
            valueWriter,
            file,
            shouldGenerateNonExistentFields,
            reverseFields);
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
          throw new IllegalArgumentException(
              "Could not resolve to type argument \"" + field.getName() + "\": " + e.getMessage(),
              e);
        }
      } else {
        if (constructedTypeResolver != null) {
          try {
            if (!constructedTypeResolver.shouldResolve(fieldType)) {
              throw new IllegalArgumentException(
                  "Invalid type resolver found for \"" + field.getName() + "\"");
            }
            Object resolvedValue = constructedTypeResolver.toType(value, field);
            field.setAccessible(true);
            field.set(annotatedConfig, resolvedValue);
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                "Could not set field \""
                    + field.getName()
                    + "\" value; field not accessible anymore");
          } catch (Exception e) {
            throw new IllegalArgumentException(
                "Could not resolve to type argument \"" + field.getName() + "\": " + e.getMessage(),
                e);
          }
        } else {
          // either I'm really tired or the only way we can check if the type is primitive type is
          // to do this spaghetti
          try {
            field.setAccessible(true);
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
            if (fieldType.isAssignableFrom(int.class)
                || fieldType.isAssignableFrom(Integer.class)) {
              field.set(annotatedConfig, Integer.valueOf(String.valueOf(value)));
            }
            if (fieldType.isAssignableFrom(double.class)
                || fieldType.isAssignableFrom(Double.class)) {
              field.set(annotatedConfig, Double.valueOf(String.valueOf(value)));
            }
            if (fieldType.isAssignableFrom(float.class)
                || fieldType.isAssignableFrom(Float.class)) {
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
