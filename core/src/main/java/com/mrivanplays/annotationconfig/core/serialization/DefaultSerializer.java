package com.mrivanplays.annotationconfig.core.serialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

class DefaultSerializer implements FieldTypeSerializer<Object> {

  @Override
  public Object deserialize(DataObject data, Field field) {
    PrimitiveSerializers.registerSerializers();
    Object dataRaw = data.getAsObject();
    Class<?> fieldType = field.getType();
    if (!fieldType.isEnum()) {
      if (data.isSingleValue() && isPrimitive(dataRaw)) {
        return forcePrimitive(data, fieldType);
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
      return forcePrimitive(data, fieldType);
    } else {
      if (fieldType.isAssignableFrom(Map.class)) {
        return dataMap;
      }
      Object fieldTypeInstance;
      try {
        Constructor<?> constructor = fieldType.getDeclaredConstructor();
        constructor.setAccessible(true);
        fieldTypeInstance = constructor.newInstance();
      } catch (InvocationTargetException
          | NoSuchMethodException
          | InstantiationException
          | IllegalAccessException e) {
        try {
          Object[] values = new Object[dataMap.size()];
          int i = 0;
          for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            values[i] = entry.getValue();
            i++;
          }
          Constructor<?>[] constructors = fieldType.getDeclaredConstructors();
          Constructor<?> found = null;
          OUT:
          for (Constructor<?> constructor : constructors) {
            Annotation[] annotations = constructor.getDeclaredAnnotations();
            if (annotations.length > 0) {
              for (Annotation annotation : constructor.getDeclaredAnnotations()) {
                if (annotation.annotationType().isAssignableFrom(DeserializeConstructor.class)) {
                  found = constructor;
                  break OUT;
                }
              }
            }
          }
          if (found == null) {
            throw new IllegalArgumentException(
                "Cannot find constructor for "
                    + fieldType.getName()
                    + " ; you need to have a empty constructor or a constructor, annotated with @DeserializeConstructor.");
          }
          found.setAccessible(true);
          return found.newInstance(values);
        } catch (IllegalAccessException ia) {
          throw new IllegalArgumentException("Constructor became inaccessible");
        } catch (InvocationTargetException | InstantiationException e1) {
          throw new RuntimeException(e1);
        }
      }
      SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
      for (Field desField : fieldTypeInstance.getClass().getDeclaredFields()) {
        desField.setAccessible(true);
        Object val = dataMap.get(desField.getName());
        if (val == null) {
          continue;
        }
        Optional<FieldTypeSerializer<?>> serializerOpt =
            serializerRegistry.getSerializer(desField.getType());
        if (serializerOpt.isPresent()) {
          FieldTypeSerializer serializer = serializerOpt.get();
          try {
            desField.set(fieldTypeInstance, serializer.deserialize(new DataObject(val), field));
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("A field became inaccessible");
          }
        } else {
          try {
            desField.set(fieldTypeInstance, deserialize(new DataObject(val), field));
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("A field became inaccessible");
          }
        }
      }

      return fieldTypeInstance;
    }
  }

  @Override
  public DataObject serialize(Object value, Field field) {
    if (isPrimitive(value)) {
      return new DataObject(value);
    }
    Class<?> fieldType = field.getType();
    if (fieldType.isEnum()) {
      try {
        Method nameMethod = value.getClass().getSuperclass().getDeclaredMethod("name");
        nameMethod.setAccessible(true);
        return new DataObject(((String) nameMethod.invoke(value)).toLowerCase(Locale.ROOT));
      } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
        return new DataObject(value.toString());
      }
    }
    DataObject object = new DataObject();
    SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;
    for (Field desField : value.getClass().getDeclaredFields()) {
      desField.setAccessible(true);
      try {
        Object def = desField.get(value);
        if (def == null) {
          continue;
        }
        Optional<FieldTypeSerializer<?>> serializerOpt =
            serializerRegistry.getSerializer(desField.getType());
        if (serializerOpt.isPresent()) {
          FieldTypeSerializer serializer = serializerOpt.get();
          object.putAll(desField.getName(), serializer.serialize(def, desField));
        } else {
          object.putAll(desField.getName(), serialize(def, desField));
        }
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException("Field became inaccessible.");
      }
    }
    return object;
  }

  private Object forcePrimitive(DataObject dataObject, Class<?> fieldType) {
    Function<Object, ?> mapper = PrimitiveSerializers.getMapper(fieldType);
    if (mapper != null) {
      return mapper.apply(dataObject.getAsObject());
    }
    return dataObject.getAsObject();
  }

  private boolean isPrimitive(Object value) {
    Class<?> valClass = value.getClass();
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
        || value instanceof List
        || value instanceof Map;
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
