package com.mrivanplays.annotationconfig.core.serialization;

import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

class DefaultSerializer implements FieldTypeSerializer<Object> {

  @Override
  public Object deserialize(DataObject data, SerializationContext<Object> context) {
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
    if (data.isSingleValue() && dataRaw instanceof List) {
      List<Object> read = (List<Object>) dataRaw;
      if (read.isEmpty()) {
        return new ArrayList<>();
      } else {
        List<Object> ret = new ArrayList<>();
        SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
        for (Object o : read) {
          if (isPrimitive(o, false)) {
            ret.add(o);
          } else {
            Class<?> neededType =
                (Class<?>)
                    ((ParameterizedType) context.getGenericType()).getActualTypeArguments()[0];
            Optional<FieldTypeSerializer<?>> serializerOpt =
                serializerRegistry.getSerializer(neededType);
            if (serializerOpt.isPresent()) {
              FieldTypeSerializer serializer = serializerOpt.get();
              ret.add(
                  serializer.deserialize(
                      new DataObject(o),
                      SerializationContext.of(
                          null, null, neededType, neededType, context.getAnnotatedConfig())));
            } else {
              ret.add(
                  deserialize(
                      new DataObject(o),
                      SerializationContext.of(
                          null, null, neededType, neededType, context.getAnnotatedConfig())));
            }
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
      SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
      for (Field desField : fieldTypeInstance.getClass().getDeclaredFields()) {
        desField.setAccessible(true);
        Object val = dataMap.get(desField.getName());
        if (val == null) {
          continue;
        }
        Optional<FieldTypeSerializer<?>> serializerOpt =
            serializerRegistry.getSerializer(desField.getGenericType());
        if (serializerOpt.isPresent()) {
          FieldTypeSerializer serializer = serializerOpt.get();
          try {
            desField.set(
                fieldTypeInstance,
                serializer.deserialize(
                    new DataObject(val, true),
                    SerializationContext.fromField(desField, fieldTypeInstance)));
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("A field became inaccessible");
          }
        } else {
          try {
            desField.set(
                fieldTypeInstance,
                deserialize(
                    new DataObject(val, true),
                    SerializationContext.fromField(desField, fieldTypeInstance)));
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("A field became inaccessible");
          }
        }
      }

      return fieldTypeInstance;
    }
  }

  @Override
  public DataObject serialize(Object value, SerializationContext<Object> context) {
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
        List<Object> toSerialize = new ArrayList<>();
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
            Class<?> neededType =
                (Class<?>)
                    ((ParameterizedType) context.getGenericType()).getActualTypeArguments()[0];
            Optional<FieldTypeSerializer<?>> serializerOpt =
                serializerRegistry.getSerializer(neededType);
            if (serializerOpt.isPresent()) {
              FieldTypeSerializer serializer = serializerOpt.get();
              DataObject serialized =
                  serializer.serialize(
                      val,
                      SerializationContext.of(
                          null, val, neededType, neededType, context.getAnnotatedConfig()));
              if (serialized.isSingleValue()) {
                toSerialize.add(serialized.getAsObject());
              } else {
                toSerialize.add(serialized.getAsMap());
              }
            } else {
              DataObject serialized =
                  serialize(
                      val,
                      SerializationContext.of(
                          null, val, neededType, neededType, context.getAnnotatedConfig()));
              if (serialized.isSingleValue()) {
                toSerialize.add(serialized.getAsObject());
              } else {
                toSerialize.add(serialized.getAsMap());
              }
            }
          }
        }
        return new DataObject(toSerialize);
      }
    }
    DataObject object = new DataObject();
    for (Field desField : value.getClass().getDeclaredFields()) {
      desField.setAccessible(true);
      if (desField.getDeclaredAnnotation(Ignore.class) != null) {
        continue;
      }
      String key = desField.getName();
      if (desField.getDeclaredAnnotation(Key.class) != null) {
        key = desField.getDeclaredAnnotation(Key.class).value();
      }
      try {
        Object def = desField.get(value);
        if (def == null) {
          continue;
        }
        Optional<FieldTypeSerializer<?>> serializerOpt =
            serializerRegistry.getSerializer(desField.getGenericType());
        if (serializerOpt.isPresent()) {
          FieldTypeSerializer serializer = serializerOpt.get();
          object.putAll(
              key, serializer.serialize(def, SerializationContext.fromField(desField, value)));
        } else {
          object.putAll(key, serialize(def, SerializationContext.fromField(desField, value)));
        }
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
