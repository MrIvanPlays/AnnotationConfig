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

package com.mrivanplays.annotationconfig.core.serialization;

import com.mrivanplays.annotationconfig.core.internal.ClassInfo;
import com.mrivanplays.annotationconfig.core.internal.FieldInfo;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.NullReadHandleOption;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @hidden
 */
@SuppressWarnings({"unchecked", "rawtypes"})
final class DefaultSerializer implements FieldTypeSerializer<Object> {

  static final DefaultSerializer INSTANCE = new DefaultSerializer();

  private DefaultSerializer() {}

  @Override
  public Object deserialize(
      @NonNull DataObject data,
      @NonNull SerializationContext<Object> context,
      @NonNull AnnotationAccessor annotations) {
    Class<?> fieldType = context.getClassType();
    Object dataRaw = data.getAsObject();
    if (data.isSingleValue() && dataRaw == null) {
      return null;
    }
    if (!fieldType.isEnum()) {
      if (data.isSingleValue()
          && isPrimitive(dataRaw)
          && dataRaw.getClass().isAssignableFrom(fieldType)) {
        return forcePrimitive(dataRaw, fieldType);
      }

      if (data.isSingleValue() && isPrimitiveClass(fieldType)) {
        return forcePrimitive(dataRaw, fieldType);
      }
    }
    if (fieldType.isAssignableFrom(DataObject.class)) {
      return data;
    }
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    if (data.isSingleValue() && fieldType.isArray()) {
      Class<?> type = fieldType.getComponentType();
      if (dataRaw.getClass().isArray() && isPrimitiveArray(fieldType)) {
        return castArray(fieldType, dataRaw);
      }
      // snakeyaml (and others) return a list instead of an array for all types
      List<?> read = asList(dataRaw);
      Object arr = Array.newInstance(type, read.size());
      if (read.isEmpty()) {
        return arr;
      }
      FieldTypeSerializer serializer = serializerRegistry.getSerializer(type).orElse(this);
      SerializationContext serializationContext =
          SerializationContext.of(type, type, context.getAnnotatedConfig(), context.getSettings());
      for (int i = 0; i < read.size(); i++) {
        Object o = read.get(i);
        if (o != null && isPrimitive(o, false) && isPrimitiveClass(type)) {
          Array.set(arr, i, forcePrimitive(o, type));
        } else {
          Object deserialized =
              serializer.deserialize(
                  new DataObject(o, true), serializationContext, AnnotationAccessor.EMPTY);
          Array.set(arr, i, deserialized);
        }
      }
      return arr;
    }
    if (data.isSingleValue() && dataRaw instanceof List) {
      List<Object> read = (List<Object>) dataRaw;
      if (read.isEmpty()) {
        return new ArrayList<>();
      } else {
        List<Object> ret = new ArrayList<>(read.size());
        Class<?> neededType = listElementType(context);
        FieldTypeSerializer serializer = serializerRegistry.getSerializer(neededType).orElse(this);
        SerializationContext serializationContext =
            SerializationContext.of(
                neededType, neededType, context.getAnnotatedConfig(), context.getSettings());
        for (Object o : read) {
          if (o == null) {
            ret.add(null);
          } else if (isPrimitive(o, false) && isPrimitiveClass(neededType)) {
            // e.g. snakeyaml reads small numbers as Integer even for a List<Long>
            ret.add(forcePrimitive(o, neededType));
          } else {
            ret.add(
                serializer.deserialize(
                    new DataObject(o, true), serializationContext, AnnotationAccessor.EMPTY));
          }
        }
        return ret;
      }
    }
    // deserialize asked value list from a received value not a list as a singleton list
    if (data.isSingleValue() && fieldType.isAssignableFrom(List.class)) {
      Class<?> listValueNeeded = listElementType(context);
      if (isPrimitiveClass(listValueNeeded)) {
        return Collections.singletonList(forcePrimitive(dataRaw, listValueNeeded));
      } else {
        throw new IllegalArgumentException(
            "Found a list value asking for a non primitive type with a received "
                + data
                + " to deserialize!");
      }
    }
    // read only: the maps in it are the read maps themselves, not copies
    Map<String, Object> dataMap = data.mapView();
    if (dataMap.isEmpty()) {
      if (fieldType.isEnum()) {
        return deserializeEnum(fieldType, data.getAsString());
      }
      Object fieldTypeInstance = this.tryGetFieldTypeInstance(fieldType, context);
      List<FieldInfo> fields =
          ClassInfo.of(fieldTypeInstance.getClass()).dataFields(findStaticFields(context));
      if (fields.size() != 1) {
        throw new IllegalArgumentException(
            "Can't deserialize a "
                + data
                + " to a "
                + fieldType.getName()
                + " because "
                + fieldType.getName()
                + " does not have exactly 1 field");
      }
      FieldInfo info = fields.getFirst();
      Field desField = info.ensureAccessible();
      if (info.ignored()) {
        throw new IllegalArgumentException("Can't deserialize a " + data + " to empty class!");
      }
      FieldTypeSerializer serializer =
          serializerRegistry.getSerializer(info.genericType()).orElse(this);
      try {
        desField.set(
            fieldTypeInstance,
            serializer.deserialize(
                data,
                SerializationContext.fromField(desField, fieldTypeInstance, context.getSettings()),
                info.accessor()));
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("A field became inaccessible");
      }
      return fieldTypeInstance;
    } else {
      if (fieldType.isAssignableFrom(Map.class)) {
        // the field gets its own copy, as it did before dataMap was a view
        return data.getAsMap();
      }
      Class<?> neededInstanceAllocation = fieldType;
      if (fieldType.isAssignableFrom(List.class)) {
        neededInstanceAllocation = listElementType(context);
      }
      if (neededInstanceAllocation.isRecord()) {

        /*
         * CONTRIBUTOR NOTE: DO NOT TOUCH THIS SECTION AT ALL
         * THIS IS PURE HELL AND TORTURE OF REFLECTION UNRAVELLED
         * and, probably a big hack, but it works
         * IF IT WORKS, DO NOT, FOR ANY CIRCUMSTANCE WHATSOEVER, TOUCH
         */

        // the constructor, its parameter names and the components are looked up once per record
        // class; the components are the record's fields that are neither static nor synthetic
        ClassInfo.RecordInfo record = ClassInfo.of(neededInstanceAllocation).record();
        // try to not set with fields and directly use a ctor
        Constructor<?> ctor = record.constructor();
        if (!record.accessible()) {
          throw new RuntimeException(
              "ctor of record is not accessible '" + neededInstanceAllocation.getName() + "'");
        }
        Map<String, Object> deserialized = new HashMap<>();
        for (FieldInfo component : record.components()) {
          Field f = component.ensureAccessible();
          Object val = dataMap.get(component.key());
          if ((component.ignored() || component.rawConfig())
              && NAGGED_RECORDS.add(neededInstanceAllocation)) {
            System.err.println(
                "[AnnotationConfig] Warning: unsupported annotation on record ( NAG AUTHORS OF ) "
                    + neededInstanceAllocation.getCanonicalName());
            System.err.println(
                "[AnnotationConfig] @Ignore and @RawConfig are not supported on record components.");
            System.err.println("[AnnotationConfig] The annotation will currently be ignored.");
            System.err.println(
                "[AnnotationConfig] This may change to an exception in a future release.");
            System.err.println("[AnnotationConfig] Component: " + f.getName());
          }
          if (val == null) {
            NullReadHandleOption nullReadHandle =
                context.getSettings().get(ACDefaultSettings.NULL_READ_HANDLER).orElseThrow();
            if (nullReadHandle == NullReadHandleOption.SET_NULL) {
              // actually put a null value otherwise the record won't be constructed properly
              deserialized.put(f.getName(), null);
            } else {
              if (context.getDefaultValue().isPresent()) {
                Object defValue = context.getDefaultValue().get();
                try {
                  deserialized.put(f.getName(), f.get(defValue));
                } catch (IllegalAccessException e) {
                  throw new IllegalArgumentException("A field became inaccessible", e);
                }
              } else {
                if (context.getAnnotatedConfig() != null && context.getName().isPresent()) {
                  // also a field the config's class inherits; no such field: nothing is put
                  Field parentField =
                      tryFindField(context.getAnnotatedConfig(), context.getName().get());
                  if (parentField != null) {
                    try {
                      parentField.setAccessible(true);
                      Object defValue = parentField.get(context.getAnnotatedConfig());
                      if (defValue != null) {
                        deserialized.put(f.getName(), f.get(defValue));
                      } else {
                        // at least we _tried_ though
                        deserialized.put(f.getName(), null);
                      }
                    } catch (IllegalAccessException e) {
                      deserialized.put(f.getName(), null);
                      new RuntimeException("Field became inaccessible, will be set to null", e)
                          .printStackTrace();
                    }
                  }
                } else {
                  // again, set null (unfortunately)
                  deserialized.put(f.getName(), null);
                }
              }
            }
            continue;
          }
          FieldTypeSerializer serializer =
              serializerRegistry.getSerializer(component.genericType()).orElse(this);
          try {
            deserialized.put(
                f.getName(),
                serializer.deserialize(
                    new DataObject(val, true),
                    SerializationContext.fromField(f, null, context.getSettings()),
                    component.accessor()));
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("A field became inaccessible", e);
          }
        }
        try {
          // sort values by the order they occur
          String[] parameterNames = record.parameterNames();
          Object[] ctorValues = new Object[parameterNames.length];
          for (int i = 0; i < parameterNames.length; i++) {
            ctorValues[i] = deserialized.get(parameterNames[i]);
          }

          return ctor.newInstance(ctorValues);
        } catch (InvocationTargetException e) {
          throw new RuntimeException("ctor invoke threw exception", e);
        } catch (InstantiationException e) {
          throw new RuntimeException("could not instantiate '" + fieldType.getName() + "'", e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException("ctor became inaccessible '" + fieldType.getName() + "'", e);
        }
      } else {
        Object fieldTypeInstance = this.tryGetFieldTypeInstance(fieldType, context);
        for (FieldInfo info :
            ClassInfo.of(fieldTypeInstance.getClass()).dataFields(findStaticFields(context))) {
          if (info.ignored()) {
            continue;
          }
          Object val = dataMap.get(info.key());
          if (val == null) {
            continue;
          }
          Field desField = info.ensureAccessible();
          FieldTypeSerializer serializer =
              serializerRegistry.getSerializer(info.genericType()).orElse(this);
          try {
            desField.set(
                fieldTypeInstance,
                serializer.deserialize(
                    new DataObject(val, true),
                    SerializationContext.fromField(
                        desField, fieldTypeInstance, context.getSettings()),
                    info.accessor()));
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("A field became inaccessible", e);
          }
        }

        return fieldTypeInstance;
      }
    }
  }

  private Object tryGetFieldTypeInstance(Class<?> fieldType, SerializationContext<Object> context) {
    Object fieldTypeInstance;
    if (context.getDefaultValue().isPresent()) {
      fieldTypeInstance = context.getDefaultValue().get();
    } else {
      try {
        if (context.getAnnotatedConfig() != null && context.getName().isPresent()) {
          Field orig = tryFindField(context.getAnnotatedConfig(), context.getName().get());
          if (orig == null) {
            return this.createFieldTypeInstance(fieldType);
          }
          orig.setAccessible(true);
          fieldTypeInstance = orig.get(context.getAnnotatedConfig());
          if (fieldTypeInstance == null) {
            return this.createFieldTypeInstance(fieldType);
          }
        } else {
          fieldTypeInstance = this.createFieldTypeInstance(fieldType);
        }
      } catch (IllegalAccessException e) {
        fieldTypeInstance = this.createFieldTypeInstance(fieldType);
      }
    }
    return fieldTypeInstance;
  }

  /**
   * The field {@code fieldName} of {@code annotatedConfig}'s class or of a class it extends, as
   * {@code FIND_PARENT_FIELDS} finds fields; {@code null} if there is none.
   */
  private static @Nullable Field tryFindField(Object annotatedConfig, String fieldName) {
    for (Class<?> type = annotatedConfig.getClass();
        type != null && type != Object.class;
        type = type.getSuperclass()) {
      for (FieldInfo info : ClassInfo.of(type).fields()) {
        if (info.name().equals(fieldName)) {
          return info.field();
        }
      }
    }
    return null;
  }

  private static boolean findStaticFields(SerializationContext<?> context) {
    return context.getSettings().get(ACDefaultSettings.FIND_STATIC_FIELDS).orElse(false);
  }

  // records whose unsupported annotations have been reported, so it happens once per record
  private static final Set<Class<?>> NAGGED_RECORDS = ConcurrentHashMap.newKeySet();

  private Object createFieldTypeInstance(Class<?> fieldType) {
    Object fieldTypeInstance;
    try {
      Constructor<?> constructor = fieldType.getConstructor();
      if (!constructor.trySetAccessible()) {
        throw new RuntimeException("Could not set constructor accessible; " + fieldType.getName());
      }
      fieldTypeInstance = constructor.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException("Cannot instantiate " + fieldType.getName() + " ; ", e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Cannot invoke constructor of " + fieldType.getName() + " ; ", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(
          "Constructor of " + fieldType.getName() + " became inaccessible ; ", e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(
          "Type " + fieldType.getName() + " does not have an empty constructor ; ", e);
    }
    return fieldTypeInstance;
  }

  @Override
  @NonNull
  public DataObject serialize(
      @NonNull Object value,
      @NonNull SerializationContext<Object> context,
      @NonNull AnnotationAccessor annotations) {
    if (isPrimitive(value)) {
      return new DataObject(value);
    }
    if (value instanceof DataObject) {
      return (DataObject) value;
    }
    Class<?> fieldType = context.getClassType();
    if (fieldType.isEnum()) {
      // name() rather than toString(): toString may be overridden, and constants with a body are
      // subclasses of the enum, which broke the old reflective lookup of name()
      return new DataObject(
          value instanceof Enum<?> constant
              ? constant.name().toLowerCase(Locale.ROOT)
              : value.toString());
    }
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    if (value instanceof List) {
      List<Object> values = (List<Object>) value;
      if (values.isEmpty()) {
        return new DataObject(values);
      } else {
        List<Object> toSerialize = new ArrayList<>(values.size());
        Class<?> neededType = listElementType(context);
        FieldTypeSerializer serializer = serializerRegistry.getSerializer(neededType).orElse(this);
        SerializationContext serializationContext =
            SerializationContext.of(
                neededType, neededType, context.getAnnotatedConfig(), context.getSettings());
        for (Object val : values) {
          if (isPrimitive(val)) {
            // BigDecimal and BigInteger are kept as they are: every writer writes them exactly
            toSerialize.add(val);
          } else {
            callParentSerializer(
                toSerialize, neededType, serializer, serializationContext, val, annotations);
          }
        }
        return new DataObject(toSerialize);
      }
    }
    if (fieldType.isArray()) {
      if (isPrimitiveArray(fieldType)) {
        return new DataObject(value);
      }
      Class<?> type = fieldType.getComponentType();
      FieldTypeSerializer serializer = serializerRegistry.getSerializer(type).orElse(this);
      SerializationContext serializationContext =
          SerializationContext.of(type, type, context.getAnnotatedConfig(), context.getSettings());
      Object[] array = (Object[]) value;
      List<Object> list = new ArrayList<>(array.length);
      for (Object toSerialize : array) {
        if (toSerialize != null) {
          callParentSerializer(
              list, type, serializer, serializationContext, toSerialize, annotations);
        }
      }
      return new DataObject(list);
    }
    // static fields only with FIND_STATIC_FIELDS, and never synthetic ones: a class with one field
    // and a constant is written as that one field's value, as a class with only the field is
    List<FieldInfo> fields = ClassInfo.of(value.getClass()).dataFields(findStaticFields(context));
    if (fields.isEmpty()) {
      throw new IllegalArgumentException("Can't serialize object with no fields.");
    }
    if (fields.size() == 1) {
      FieldInfo info = fields.getFirst();
      Field desField = info.ensureAccessible();
      if (info.ignored()) {
        throw new IllegalArgumentException("Can't serialize object with no fields.");
      }
      try {
        Object def = desField.get(value);
        if (def == null) {
          throw new IllegalArgumentException("Can't serialize object with no fields.");
        }
        FieldTypeSerializer serializer =
            serializerRegistry.getSerializer(info.genericType()).orElse(this);
        return serializer.serialize(
            def,
            SerializationContext.of(
                desField.getName(),
                def,
                desField.getType(),
                info.genericType(),
                context.getAnnotatedConfig(),
                context.getSettings()),
            info.accessor());
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Field became inaccessible.", e);
      }
    }
    DataObject object = new DataObject();
    for (FieldInfo info : fields) {
      if (info.ignored()) {
        continue;
      }
      Field desField = info.ensureAccessible();
      String key = info.key();
      try {
        Object def = desField.get(value);
        if (def == null) {
          continue;
        }
        FieldTypeSerializer serializer =
            serializerRegistry.getSerializer(info.genericType()).orElse(this);
        DataObject serialized =
            serializer.serialize(
                def,
                SerializationContext.of(
                    desField.getName(),
                    def,
                    desField.getType(),
                    info.genericType(),
                    context.getAnnotatedConfig(),
                    context.getSettings()),
                info.accessor());
        if (serialized == null) {
          throw new NullPointerException(
              "Expected DataObject, but got null ; Field: "
                  + desField.getName()
                  + " ; Field type: "
                  + desField.getType().getName());
        }
        if (serialized.isEmpty()) {
          continue;
        }
        object.putAll(key, serialized);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Field became inaccessible.", e);
      }
    }
    return object;
  }

  private void callParentSerializer(
      List<Object> toSerialize,
      Class<?> neededType,
      FieldTypeSerializer serializer,
      SerializationContext serializationContext,
      Object val,
      AnnotationAccessor annotations) {
    DataObject serialized = serializer.serialize(val, serializationContext, annotations);
    if (serialized == null) {
      throw new NullPointerException(
          "Expected DataObject, but got null ; Serialized type: " + neededType.getName());
    }
    if (serialized.isEmpty()) {
      return;
    }
    if (serialized.isSingleValue()) {
      toSerialize.add(serialized.getAsObject());
    } else {
      toSerialize.add(serialized.getAsMap());
    }
  }

  /** isPrimitiveClass depends only on the class, so it is computed once per class. */
  private static final ClassValue<Boolean> PRIMITIVE_CLASS =
      new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> valClass) {
          return valClass.isPrimitive()
              || valClass.isAssignableFrom(String.class)
              || valClass.isAssignableFrom(Boolean.class)
              || valClass.isAssignableFrom(Byte.class)
              || valClass.isAssignableFrom(Character.class)
              || valClass.isAssignableFrom(Double.class)
              || valClass.isAssignableFrom(Float.class)
              || valClass.isAssignableFrom(Integer.class)
              || valClass.isAssignableFrom(Short.class)
              || valClass.isAssignableFrom(Long.class)
              || valClass.isAssignableFrom(BigDecimal.class)
              || valClass.isAssignableFrom(BigInteger.class);
        }
      };

  private Object forcePrimitive(Object val, Class<?> fieldType) {
    Function<Object, ?> mapper = PrimitiveSerializers.getMapper(fieldType);
    if (mapper != null) {
      return mapper.apply(val);
    }
    return val;
  }

  private boolean isPrimitive(Object value) {
    return isPrimitive(value, true);
  }

  private boolean isPrimitiveClass(Class<?> valClass) {
    return PRIMITIVE_CLASS.get(valClass);
  }

  private boolean isPrimitive(Object value, boolean checkMap) {
    Class<?> valClass = value.getClass();
    return isPrimitiveClass(valClass) || (checkMap && value instanceof Map);
  }

  private static Object deserializeEnum(Class<?> enumType, String value) {
    Object[] constants = enumType.getEnumConstants();
    for (Object constant : constants) {
      // serialized lower case; case-insensitive so that non upper-case constants work as well
      if (((Enum<?>) constant).name().equalsIgnoreCase(value)) {
        return constant;
      }
    }
    throw new IllegalArgumentException(
        "Unknown value '"
            + value
            + "' for enum "
            + enumType.getName()
            + "; expected one of "
            + Arrays.toString(constants));
  }

  /** Element class of a {@code List<X>} field; {@code Object} when the list is raw. */
  private static Class<?> listElementType(SerializationContext<?> context) {
    if (context.getGenericType() instanceof ParameterizedType parameterized) {
      return rawClass(parameterized.getActualTypeArguments()[0]);
    }
    return Object.class;
  }

  // getActualTypeArguments() is not always a Class: List<List<String>>, List<? extends Foo>, ...
  private static Class<?> rawClass(Type type) {
    if (type instanceof Class<?> clazz) {
      return clazz;
    }
    if (type instanceof ParameterizedType parameterized) {
      return rawClass(parameterized.getRawType());
    }
    if (type instanceof WildcardType wildcard) {
      return rawClass(wildcard.getUpperBounds()[0]);
    }
    if (type instanceof TypeVariable<?> variable) {
      return rawClass(variable.getBounds()[0]);
    }
    if (type instanceof GenericArrayType array) {
      return rawClass(array.getGenericComponentType()).arrayType();
    }
    return Object.class;
  }

  /** A list or array read from a config as a list; any other single value as a 1 element list. */
  private static List<?> asList(Object source) {
    if (source instanceof List<?> list) {
      return list;
    }
    if (source instanceof Object[] array) {
      return Arrays.asList(array);
    }
    if (source.getClass().isArray()) {
      int length = Array.getLength(source);
      List<Object> list = new ArrayList<>(length);
      for (int i = 0; i < length; i++) {
        list.add(Array.get(source, i));
      }
      return list;
    }
    return Collections.singletonList(source);
  }

  private boolean isPrimitiveArray(Class<?> type) {
    return type.isArray() && PrimitiveSerializers.getMapper(type.getComponentType()) != null;
  }

  private Object castArray(Class<?> arrayType, Object source) {
    if (!arrayType.isArray() || !source.getClass().isArray()) {
      throw new IllegalArgumentException("Not an array");
    }
    Class<?> component = arrayType.getComponentType();
    Function<Object, ?> mapper = PrimitiveSerializers.getMapper(component); // once per array
    if (mapper == null) {
      mapper = Function.identity();
    }
    int length = Array.getLength(source);
    Object result = Array.newInstance(component, length);
    Object[] objects = source instanceof Object[] array ? array : null; // no reflection needed
    if (component.isPrimitive()) {
      for (int i = 0; i < length; i++) {
        Object converted = mapper.apply(objects != null ? objects[i] : Array.get(source, i));
        if (converted == null) {
          throw new IllegalArgumentException(
              "No value for element " + i + " of " + component.getName() + " array");
        }
        Array.set(result, i, converted);
      }
    } else {
      Object[] out = (Object[]) result;
      for (int i = 0; i < length; i++) {
        out[i] = mapper.apply(objects != null ? objects[i] : Array.get(source, i));
      }
    }
    return result;
  }

  private static final class PrimitiveSerializers {

    // only written during class initialisation, which the JVM makes thread safe
    private static final Map<Class<?>, Function<Object, ?>> serializers = new HashMap<>();

    static {
      // not validateO: that reads "" as no value, which suits numbers but makes an empty string
      // impossible to configure
      registerSerializer(String.class, o -> o == null ? null : String.valueOf(o));

      Function<Object, Byte> byteSerializer = stripDot(Byte.class, Byte::parseByte);
      registerSerializer(byte.class, byteSerializer);
      registerSerializer(Byte.class, byteSerializer);

      // Gson and Jackson's TOML parser read decimals as BigDecimal. Its doubleValue() and
      // floatValue() are correctly rounded (since Java 21), so they give what parsing its text
      // gives, without building the text. Other numbers, e.g. a Float, still go through their
      // text, so that 3.11f is read as 3.11 and not as 3.109999895095825.
      Function<Object, Double> parseDouble = validateO(Double.class, Double::parseDouble);
      Function<Object, Double> doubleSerializer =
          o -> o instanceof BigDecimal decimal ? decimal.doubleValue() : parseDouble.apply(o);
      registerSerializer(double.class, doubleSerializer);
      registerSerializer(Double.class, doubleSerializer);

      Function<Object, Float> parseFloat = validateO(Float.class, Float::parseFloat);
      Function<Object, Float> floatSerializer =
          o -> o instanceof BigDecimal decimal ? decimal.floatValue() : parseFloat.apply(o);
      registerSerializer(float.class, floatSerializer);
      registerSerializer(Float.class, floatSerializer);

      Function<Object, Integer> intSerializer = stripDot(Integer.class, Integer::parseInt);
      registerSerializer(Integer.class, intSerializer);
      registerSerializer(int.class, intSerializer);

      Function<Object, Short> shortSerializer = stripDot(Short.class, Short::parseShort);
      registerSerializer(Short.class, shortSerializer);
      registerSerializer(short.class, shortSerializer);

      Function<Object, Long> longSerializer = stripDot(Long.class, Long::parseLong);
      registerSerializer(Long.class, longSerializer);
      registerSerializer(long.class, longSerializer);

      // text formats store chars as one character strings
      Function<Object, Character> charSerializer =
          o -> {
            if (o == null) {
              return null;
            }
            if (o instanceof Character c) {
              return c;
            }
            String text = String.valueOf(o);
            if (text.length() != 1) {
              throw new IllegalArgumentException(
                  "Invalid input; expected character got String: " + text);
            }
            return text.charAt(0);
          };
      registerSerializer(char.class, charSerializer);
      registerSerializer(Character.class, charSerializer);

      Function<Object, Boolean> boolSerializer = validateO(Boolean.class, Boolean::parseBoolean);
      registerSerializer(boolean.class, boolSerializer);
      registerSerializer(Boolean.class, boolSerializer);

      // parsed directly, going through double / long lost precision and overflowed past 2^63
      registerSerializer(BigDecimal.class, validateO(BigDecimal.class, BigDecimal::new));
      registerSerializer(BigInteger.class, stripDot(BigInteger.class, BigInteger::new));
    }

    private static <T> Function<Object, T> validateO(Function<String, T> cont) {
      return o -> {
        if (o == null) {
          return null; // String.valueOf(null) is "null", which would then be parsed
        }
        String strVal = String.valueOf(o);
        if (strVal.isEmpty()) {
          return null;
        }
        return cont.apply(strVal);
      };
    }

    private static <T> Function<Object, T> validateO(Class<T> type, Function<String, T> cont) {
      Function<Object, T> parse = validateO(cont);
      return o -> type.isInstance(o) ? type.cast(o) : parse.apply(o);
    }

    private static <T> Function<Object, T> stripDot(Class<T> type, Function<String, T> finish) {
      return validateO(
          type,
          s -> {
            // a whole number can arrive in decimal form, e.g. "5.0" from a double, or with an
            // exponent ("2.0E9"), which double uses from 10^7 on; cutting that at the '.' gave 2
            if (s.indexOf('e') != -1 || s.indexOf('E') != -1) {
              return finish.apply(new BigDecimal(s).toBigInteger().toString());
            }
            int dot = s.indexOf('.');
            return finish.apply(dot == -1 ? s : s.substring(0, dot));
          });
    }

    public static Function<Object, ?> getMapper(Class<?> aClass) {
      return serializers.get(aClass);
    }

    private static <T> void registerSerializer(Class<T> val, Function<Object, T> mapper) {
      serializers.put(val, mapper);
    }
  }
}
