package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.Multiline;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comments;
import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.annotations.custom.ValidationResponse;
import com.mrivanplays.annotationconfig.core.annotations.type.AnnotationType;
import com.mrivanplays.annotationconfig.core.internal.MinMaxHandler.NumberResult;
import com.mrivanplays.annotationconfig.core.internal.MinMaxHandler.State;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
      KeyResolver keyResolver,
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
            keyResolver,
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
      KeyResolver keyResolver,
      boolean reverseFields) {
    try (PrintWriter writer = new PrintWriter(writerFeed)) {
      toWriter(
          annotatedConfig,
          writer,
          map,
          commentChar,
          valueWriter,
          options,
          keyResolver,
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
      KeyResolver keyResolver,
      boolean reverseFields)
      throws IOException {
    WriteData parentData = new WriteData();
    for (Map.Entry<AnnotationHolder, Set<AnnotationType>> entry : map.entrySet()) {
      AnnotationHolder holder = entry.getKey();
      if (holder.isClass()) {
        Class<?> theClass = annotatedConfig.getClass();
        for (AnnotationType type : entry.getValue()) {
          List<String> comments = getComments(type, null, theClass);
          if (!comments.isEmpty()) {
            for (String comment : comments) {
              writer.println(commentChar + comment);
            }
          }
        }
        writer.append('\n');
        continue;
      }
      if (entry.getValue().size() > 1) {
        if (entry.getValue().contains(AnnotationType.RAW_CONFIG)) {
          throw new IllegalArgumentException(
              "Found illegal annotation placement ; @RawConfig on a field with other annotations except @RawConfig.");
        }
      } else if (entry.getValue().size() == 1
          && entry.getValue().contains(AnnotationType.RAW_CONFIG)) {
        continue;
      }
      try {
        WriteData current = getWriteData(annotatedConfig, entry, keyResolver, reverseFields);
        for (Map.Entry<String, Object> childDataWrite : current.getToWrite().entrySet()) {
          String childDataKey = childDataWrite.getKey();
          Object childDataValue = childDataWrite.getValue();
          combineMapToData(parentData, childDataKey, childDataValue);
        }
        parentData.getFieldComments().putAll(current.getFieldComments());
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("A field became inaccessible");
      }
    }
    valueWriter.write(parentData.getToWrite(), parentData.getFieldComments(), writer, options);
  }

  private static class WriteData {

    private List<String> classComments = new ArrayList<>();
    private Map<String, Object> toWrite = new HashMap<>();
    private Map<String, List<String>> fieldComments = new HashMap<>();

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
      Object annotatedConfig,
      Map.Entry<AnnotationHolder, Set<AnnotationType>> entry,
      KeyResolver keyResolver,
      boolean reverseFields)
      throws IllegalAccessException {
    AnnotationHolder holder = entry.getKey();
    WriteData ret = new WriteData();
    if (holder.isClass()) {
      List<String> comments = Collections.emptyList();
      for (AnnotationType type : entry.getValue()) {
        if (comments.isEmpty()) {
          comments = getComments(type, null, annotatedConfig.getClass());
        }
      }
      ret.getClassComments().addAll(comments);
      return ret;
    }
    Field field = entry.getKey().getField();
    field.setAccessible(true);
    String keyName = field.getName();
    List<String> comments = Collections.emptyList();
    boolean configObject = false;
    Character multilineCharacter = null;
    for (AnnotationType type : entry.getValue()) {
      if (comments.isEmpty()) {
        comments = getComments(type, field, null);
      }
      if (type.is(AnnotationType.KEY)) {
        keyName = field.getDeclaredAnnotation(Key.class).value();
      }
      if (type.is(AnnotationType.CONFIG_OBJECT)) {
        configObject = true;
      }
      if (type.is(AnnotationType.MULTILINE)) {
        multilineCharacter = field.getDeclaredAnnotation(Multiline.class).value();
      }
    }
    if (configObject) {
      Object cfgObject = field.get(annotatedConfig);
      Map<AnnotationHolder, Set<AnnotationType>> annotations =
          resolveAnnotations(cfgObject, reverseFields);

      WriteData combinedData = new WriteData();
      for (Map.Entry<AnnotationHolder, Set<AnnotationType>> e1 : annotations.entrySet()) {
        WriteData childData = getWriteData(cfgObject, e1, keyResolver, reverseFields);
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
    Optional<FieldTypeSerializer<?>> serializerOpt =
        SerializerRegistry.INSTANCE.getSerializer(field.getGenericType());
    FieldTypeSerializer serializer;
    if (serializerOpt.isPresent()) {
      serializer = serializerOpt.get();
    } else {
      serializer = SerializerRegistry.INSTANCE.getDefaultSerializer();
    }
    DataObject serialized = serializer.serialize(defaultsToValueObject, field);
    if (serialized == null) {
      throw new NullPointerException(
          "Expected DataObject, got null ; Field: "
              + field.getName()
              + " ; Field type: "
              + field.getType().getName());
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
            foundWriteAnnotation = type;
            Optional<FieldTypeSerializer<?>> newSerializerOpt =
                SerializerRegistry.INSTANCE.getSerializer(val.getClass());
            FieldTypeSerializer newSerializer;
            if (newSerializerOpt.isPresent()) {
              newSerializer = newSerializerOpt.get();
            } else {
              newSerializer = SerializerRegistry.INSTANCE.getDefaultSerializer();
            }
            DataObject newSerialized = newSerializer.serialize(val, field);
            if (newSerialized == null) {
              throw new NullPointerException(
                  "Expected DataObject, but got null ; Field: "
                      + field.getName()
                      + " ; Field type: "
                      + val.getClass().getName());
            }
            if (newSerialized.isSingleValue()) {
              defaultsToValueObject = newSerialized.getAsObject();
            } else {
              defaultsToValueObject = newSerialized.getAsMap();
            }
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
    Map<String, Object> dummyValues = new HashMap<>();
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
      Map<AnnotationHolder, Set<AnnotationType>> map,
      NullReadHandleOption nullReadHandler,
      CustomOptions options,
      KeyResolver keyResolver,
      boolean reverseFields) {
    boolean missingOptions = false;
    for (Map.Entry<AnnotationHolder, Set<AnnotationType>> entry : map.entrySet()) {
      AnnotationHolder holder = entry.getKey();
      if (holder.isClass()) {
        continue;
      }
      Field field = holder.getField();
      field.setAccessible(true);
      Set<AnnotationType> annotationTypes = entry.getValue();
      if (annotationTypes.size() > 1 && annotationTypes.contains(AnnotationType.RAW_CONFIG)) {
        throw new IllegalArgumentException(
            "Found illegal annotation placement ; @RawConfig on a field with other annotations except @RawConfig.");
      } else if (annotationTypes.size() == 1
          && annotationTypes.contains(AnnotationType.RAW_CONFIG)) {
        if (!field.getType().isAssignableFrom(DataObject.class)) {
          throw new IllegalArgumentException("@RawConfig on a field which is not DataObject");
        }
        try {
          field.set(annotatedConfig, new DataObject(values, true));
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException(
              "Could not set a field's value ; field not accessible anymore");
        }
        continue;
      }
      String keyName = field.getName();
      boolean configObject = false;
      Object section = null;
      NumberResult min = NumberResult.stateOnly(State.START);
      NumberResult max = NumberResult.stateOnly(State.START);
      for (AnnotationType type : annotationTypes) {
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
        boolean thMissing =
            setFields(
                section,
                (Map<String, Object>) value,
                resolveAnnotations(section, reverseFields),
                nullReadHandler,
                options,
                keyResolver,
                reverseFields);
        if (thMissing && !missingOptions) {
          missingOptions = true;
        }
        continue;
      }
      Type fieldType = field.getGenericType();
      Optional<FieldTypeSerializer<?>> serializerOpt =
          SerializerRegistry.INSTANCE.getSerializer(fieldType);
      FieldTypeSerializer<?> serializer;
      if (!serializerOpt.isPresent()) {
        serializer = SerializerRegistry.INSTANCE.getDefaultSerializer();
      } else {
        serializer = serializerOpt.get();
      }
      Object deserialized = serializer.deserialize(new DataObject(value, true), field);
      if (deserialized == null && !thisMissingOption) {
        if (nullReadHandler == NullReadHandleOption.USE_DEFAULT_VALUE) {
          continue;
        }
      } else if (deserialized == null) {
        continue;
      }
      if (deserialized instanceof Number) {
        Number comparable = (Number) deserialized;
        State comparison = MinMaxHandler.compare(min, max, comparable);
        handleComparison(comparison, keyName, comparable, fieldType, min, max, Number.class);
      } else if (deserialized instanceof String) {
        String comparable = (String) deserialized;
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
            ValidationResponse validatorResponse =
                validator.validate(field.getAnnotation(type), deserialized, options, field);
            if (validatorResponse.throwError() != null) {
              error = validatorResponse.throwError();
              break;
            }
            if (validatorResponse.shouldFailSilently()) {
              failed = validatorResponse.shouldFailSilently();
              break;
            }
            if (validatorResponse.onSuccess() != null) {
              validatorResponse.onSuccess().run();
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
            "Could not set a field's value ; field not accessible anymore");
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
