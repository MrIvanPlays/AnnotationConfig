package com.mrivanplays.annotationconfig.core.serialization;

import com.mrivanplays.annotationconfig.core.utils.AnnotationUtils;
import com.mrivanplays.annotationconfig.core.utils.ReflectionUtils;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "rawtypes", "FieldMayBeFinal"})
class DefaultSerializer implements FieldTypeSerializer<Object> {

  static final DefaultSerializer INSTANCE = new DefaultSerializer();

  private DefaultSerializer() {}

  @Override
  public Object deserialize(
      DataObject data, SerializationContext<Object> context, AnnotationAccessor annotations) {
    PrimitiveSerializers.registerSerializers();
    Class<?> fieldType = context.getClassType();
    Object dataRaw = data.getAsObject();
    if (data.isSingleValue() && dataRaw == null) {
      return null;
    }
    if (!fieldType.isEnum()) {
      if (data.isSingleValue()
          && isPrimitive(dataRaw)
          && dataRaw.getClass().isAssignableFrom(fieldType)) {
        return forcePrimitive(data.getAsObject(), fieldType);
      }

      if (data.isSingleValue() && isPrimitiveClass(fieldType)) {
        return forcePrimitive(data.getAsObject(), fieldType);
      }
    }
    if (fieldType.isAssignableFrom(DataObject.class)) {
      return data;
    }
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    if (data.isSingleValue() && fieldType.isArray()) {
      String typeName = fieldType.getTypeName();
      typeName = typeName.substring(0, typeName.length() - 2);
      if (dataRaw instanceof List) {
        List<Object> read = (List<Object>) dataRaw;
        if (read.isEmpty()) {
          return Array.newInstance(fieldType, 0);
        }
        // snakeyaml returns a list instead of an array for all types
        Class<?> type;
        if (ReflectionUtils.isPrimitive(typeName)) {
          type = ReflectionUtils.getPrimitiveClass(typeName);
        } else {
          try {
            type = Class.forName(typeName);
          } catch (ClassNotFoundException e) {
            throw new RuntimeException("Couldn't get class of non primitive array", e);
          }
        }
        Object arr = Array.newInstance(type, read.size());
        FieldTypeSerializer serializer = serializerRegistry.getSerializer(type).orElse(this);
        SerializationContext serializationContext =
            SerializationContext.of(type, type, context.getAnnotatedConfig());
        for (int i = 0; i < read.size(); i++) {
          Object o = read.get(i);
          if (isPrimitive(o, false)) {
            Array.set(arr, i, forcePrimitive(o, type));
          } else {
            Object deserialized =
                serializer.deserialize(
                    new DataObject(o, true), serializationContext, AnnotationAccessor.EMPTY);
            Array.set(arr, i, deserialized);
          }
        }
        return arr;
      } else {
        if (ReflectionUtils.isPrimitive(typeName)) {
          return dataRaw;
        }
        // this is (technically) not possible to be reached by the default serializer, but it's
        // better to have the belt than not to.
        try {
          Class<?> type = Class.forName(typeName);
          List<Object> list = new LinkedList<>();
          FieldTypeSerializer serializer = serializerRegistry.getSerializer(type).orElse(this);
          SerializationContext serializationContext =
              SerializationContext.of(type, type, context.getAnnotatedConfig());
          for (Object o : (Object[]) dataRaw) {
            Object deserialized =
                serializer.deserialize(
                    new DataObject(o, true), serializationContext, AnnotationAccessor.EMPTY);
            list.add(deserialized);
          }
          return list.toArray();
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Couldn't get class of non primitive array", e);
        }
      }
    }
    if (data.isSingleValue() && dataRaw instanceof List) {
      List<Object> read = (List<Object>) dataRaw;
      if (read.isEmpty()) {
        return new LinkedList<>();
      } else {
        List<Object> ret = new LinkedList<>();
        Class<?> neededType =
            (Class<?>) ((ParameterizedType) context.getGenericType()).getActualTypeArguments()[0];
        FieldTypeSerializer serializer = serializerRegistry.getSerializer(neededType).orElse(this);
        SerializationContext serializationContext =
            SerializationContext.of(neededType, neededType, context.getAnnotatedConfig());
        for (Object o : read) {
          if (isPrimitive(o, false)) {
            ret.add(o);
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
      Class<?> listValueNeeded =
          (Class<?>) ((ParameterizedType) context.getGenericType()).getActualTypeArguments()[0];
      if (isPrimitiveClass(listValueNeeded)) {
        return Collections.singletonList(forcePrimitive(data.getAsObject(), listValueNeeded));
      } else {
        throw new IllegalArgumentException(
            "Found a list value asking for a non primitive type with a received "
                + data
                + " to deserialize!");
      }
    }
    Map<String, Object> dataMap = data.getAsMap();
    if (dataMap.isEmpty()) {
      if (fieldType.isEnum()) {
        String dataString = data.getAsString();
        try {
          Method valueOfMethod = fieldType.getDeclaredMethod("valueOf", String.class);
          valueOfMethod.setAccessible(true);
          return valueOfMethod.invoke(null, dataString.toUpperCase(Locale.ROOT));
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
          // ignored
        }
      }
      if (data.getAsObject() == null) {
        return null;
      }
      return forcePrimitive(data.getAsObject(), fieldType);
    } else {
      if (fieldType.isAssignableFrom(Map.class)) {
        return dataMap;
      }
      Class<?> neededInstanceAllocation = fieldType;
      if (fieldType.isAssignableFrom(List.class)) {
        neededInstanceAllocation =
            (Class<?>) ((ParameterizedType) context.getGenericType()).getActualTypeArguments()[0];
      }
      Object fieldTypeInstance;
      try {
        fieldTypeInstance = getUnsafeInstance().allocateInstance(neededInstanceAllocation);
      } catch (InstantiationException e) {
        throw new RuntimeException("Cannot instantiate " + fieldType.getName() + " ; ", e);
      }
      for (Field desField : fieldTypeInstance.getClass().getDeclaredFields()) {
        desField.setAccessible(true);
        if (AnnotationUtils.isIgnored(desField)) {
          continue;
        }
        Object val = dataMap.get(AnnotationUtils.getKey(desField));
        if (val == null) {
          continue;
        }
        FieldTypeSerializer serializer =
            serializerRegistry.getSerializer(desField.getGenericType()).orElse(this);
        try {
          desField.set(
              fieldTypeInstance,
              serializer.deserialize(
                  new DataObject(val, true),
                  SerializationContext.fromField(desField, fieldTypeInstance),
                  AnnotationAccessor.createFromField(desField)));
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException("A field became inaccessible");
        }
      }

      return fieldTypeInstance;
    }
  }

  @Override
  public DataObject serialize(
      Object value, SerializationContext<Object> context, AnnotationAccessor annotations) {
    if (isPrimitive(value)) {
      return new DataObject(value);
    }
    if (value instanceof DataObject) {
      return (DataObject) value;
    }
    Class<?> fieldType = context.getClassType();
    if (fieldType.isEnum()) {
      try {
        Method nameMethod = value.getClass().getSuperclass().getDeclaredMethod("name");
        nameMethod.setAccessible(true);
        return new DataObject(((String) nameMethod.invoke(value)).toLowerCase(Locale.ROOT));
      } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
        return new DataObject(value.toString());
      }
    }
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    if (value instanceof List) {
      List<Object> values = (List<Object>) value;
      if (values.isEmpty()) {
        return new DataObject(values);
      } else {
        List<Object> toSerialize = new LinkedList<>();
        Class<?> neededType =
            (Class<?>) ((ParameterizedType) context.getGenericType()).getActualTypeArguments()[0];
        FieldTypeSerializer serializer = serializerRegistry.getSerializer(neededType).orElse(this);
        SerializationContext serializationContext =
            SerializationContext.of(neededType, neededType, context.getAnnotatedConfig());
        for (Object val : values) {
          if (isPrimitive(val)) {
            if (val instanceof BigDecimal) {
              toSerialize.add(((BigDecimal) val).doubleValue());
            } else if (val instanceof BigInteger) {
              toSerialize.add(((BigInteger) val).intValueExact());
            } else {
              toSerialize.add(val);
            }
          } else {
            DataObject serialized =
                serializer.serialize(val, serializationContext, AnnotationAccessor.EMPTY);
            if (serialized == null) {
              throw new NullPointerException(
                  "Expected DataObject, but got null ; Serialized type: " + neededType.getName());
            }
            if (serialized.isEmpty()) {
              continue;
            }
            if (serialized.isSingleValue()) {
              toSerialize.add(serialized.getAsObject(true));
            } else {
              toSerialize.add(serialized.getAsMap());
            }
          }
        }
        return new DataObject(toSerialize);
      }
    }
    if (fieldType.isArray()) {
      String typeName = fieldType.getTypeName();
      typeName = typeName.substring(0, typeName.length() - 2);
      if (ReflectionUtils.isPrimitive(typeName)) {
        return new DataObject(value);
      } else {
        try {
          Class<?> type = Class.forName(typeName);
          FieldTypeSerializer serializer = serializerRegistry.getSerializer(type).orElse(this);
          SerializationContext serializationContext =
              SerializationContext.of(type, type, context.getAnnotatedConfig());
          List<Object> list = new LinkedList<>();
          for (Object toSerialize : (Object[]) value) {
            if (toSerialize == null) {
              continue;
            }
            DataObject serialized =
                serializer.serialize(toSerialize, serializationContext, AnnotationAccessor.EMPTY);
            if (serialized == null) {
              throw new NullPointerException(
                  "Expected DataObject, but got null ; Serialized type: " + type.getName());
            }
            if (serialized.isEmpty()) {
              continue;
            }
            if (serialized.isSingleValue()) {
              list.add(serialized.getAsObject(true));
            } else {
              list.add(serialized.getAsMap());
            }
          }
          return new DataObject(list);
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Couldn't get class of non primitive array", e);
        }
      }
    }
    DataObject object = new DataObject();
    for (Field desField : value.getClass().getDeclaredFields()) {
      desField.setAccessible(true);
      if (AnnotationUtils.isIgnored(desField)) {
        continue;
      }
      String key = AnnotationUtils.getKey(desField);
      try {
        Object def = desField.get(value);
        if (def == null) {
          continue;
        }
        FieldTypeSerializer serializer =
            serializerRegistry.getSerializer(desField.getGenericType()).orElse(this);
        DataObject serialized =
            serializer.serialize(
                def,
                SerializationContext.of(
                    desField.getName(),
                    def,
                    desField.getType(),
                    desField.getGenericType(),
                    context.getAnnotatedConfig()),
                AnnotationAccessor.createFromField(desField));
        if (serialized == null) {
          throw new NullPointerException(
              "Expected DataObject, but got null ; Field: "
                  + desField.getName()
                  + " ; Field type: "
                  + desField.getClass().getName());
        }
        if (serialized.isEmpty()) {
          continue;
        }
        object.putAll(key, serialized);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Field became inaccessible.");
      }
    }
    return object;
  }

  private sun.misc.Unsafe getUnsafeInstance() {
    try {
      Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      return (sun.misc.Unsafe) field.get(null);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      // ignored
      return null;
    }
  }

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

  private boolean isPrimitive(Object value, boolean checkMap) {
    Class<?> valClass = value.getClass();
    return isPrimitiveClass(valClass) || (checkMap && value instanceof Map);
  }

  private static final class PrimitiveSerializers {

    private static Map<Class<?>, Function<Object, ?>> serializers = new HashMap<>();
    private static boolean registered = false;

    public static void registerSerializers() {
      if (registered) {
        return;
      }

      Function<Object, Byte> byteSerializer = (o) -> Byte.parseByte(String.valueOf(o));
      registerSerializer(byte.class, byteSerializer);
      registerSerializer(Byte.class, byteSerializer);

      Function<Object, Double> doubleSerializer = (o) -> Double.parseDouble(String.valueOf(o));
      registerSerializer(double.class, doubleSerializer);
      registerSerializer(Double.class, doubleSerializer);

      Function<Object, Float> floatSerializer = (o) -> Float.parseFloat(String.valueOf(o));
      registerSerializer(float.class, floatSerializer);
      registerSerializer(Float.class, floatSerializer);

      Function<Object, Integer> intSerializer = (o) -> Integer.parseInt(String.valueOf(o));
      registerSerializer(Integer.class, intSerializer);
      registerSerializer(int.class, intSerializer);

      Function<Object, Short> shortSerializer = (o) -> Short.parseShort(String.valueOf(o));
      registerSerializer(Short.class, shortSerializer);
      registerSerializer(short.class, shortSerializer);

      Function<Object, Long> longSerializer = (o) -> Long.parseLong(String.valueOf(o));
      registerSerializer(Long.class, longSerializer);
      registerSerializer(long.class, longSerializer);

      Function<Object, Character> charSerializer = (o) -> (char) o;
      registerSerializer(char.class, charSerializer);
      registerSerializer(Character.class, charSerializer);

      Function<Object, Boolean> boolSerializer = (o) -> Boolean.parseBoolean(String.valueOf(o));
      registerSerializer(boolean.class, boolSerializer);
      registerSerializer(Boolean.class, boolSerializer);

      Function<Object, BigDecimal> bigDecimalSerializer =
          (o) -> BigDecimal.valueOf(Double.parseDouble(String.valueOf(o)));
      registerSerializer(BigDecimal.class, bigDecimalSerializer);

      Function<Object, BigInteger> bigIntegerSerializer =
          (o) -> BigInteger.valueOf(Long.parseLong(String.valueOf(o)));
      registerSerializer(BigInteger.class, bigIntegerSerializer);

      registered = true;
    }

    public static Function<Object, ?> getMapper(Class<?> aClass) {
      return serializers.get(aClass);
    }

    private static <T> void registerSerializer(Class<T> val, Function<Object, T> mapper) {
      serializers.put(val, mapper);
    }
  }
}
