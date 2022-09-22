package com.mrivanplays.annotationconfig.core.utils;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Utilities to help with reflection stuff
 *
 * @author MrIvanPlays
 * @since 3.0.0
 */
public final class ReflectionUtils {

  public static boolean isPrimitive(String typeName) {
    return PrimitiveNameTypes.getNameType(typeName) != null;
  }

  public static Class<?> getPrimitiveClass(String typeName) {
    PrimitiveNameTypes nameType = PrimitiveNameTypes.getNameType(typeName);
    if (nameType == null) {
      throw new IllegalArgumentException("Not primitive");
    }
    return nameType.primitiveType;
  }

  public static Object[] castToArray(Object obj) {
    if (!obj.getClass().isArray()) {
      throw new IllegalArgumentException("Not array");
    }
    Class<?> clazz = obj.getClass();
    String typeName = clazz.getTypeName();
    typeName = typeName.substring(0, typeName.length() - 2);
    PrimitiveNameTypes nameType = PrimitiveNameTypes.getNameType(typeName);
    if (nameType == null) {
      throw new IllegalArgumentException("Not primitive");
    }
    return nameType.castToArray(obj);
  }

  public static Object castArrayToType(Class<?> type, Object obj) {
    if (!obj.getClass().isArray()) {
      throw new IllegalArgumentException("Not array");
    }
    String name = constructTypeName(type.getName());
    PrimitiveNameTypes nameType = PrimitiveNameTypes.getNameType(name);
    if (nameType == null) {
      throw new IllegalArgumentException("Not primitive");
    }
    Object[] array = nameType.castToArray(obj);
    Object ret = Array.newInstance(nameType.primitiveType, array.length);
    for (int i = 0; i < array.length; i++) {
      Array.set(ret, i, nameType.resolveToPrimitiveType(array[i]));
    }
    return ret;
  }

  private static String constructTypeName(String name) {
    if (name.startsWith("[L")) {
      name = name.substring(2);
    }
    if (name.endsWith(";")) {
      name = name.substring(0, name.length() - 1);
    }
    return name;
  }

  private ReflectionUtils() {
    throw new IllegalArgumentException("Instantiation of utility-type class.");
  }

  private enum PrimitiveNameTypes {
    BYTE(byte.class, Byte.class, "[B", "byte", "java.lang.Byte") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          byte[] b = (byte[]) obj;
          Byte[] bArr = new Byte[b.length];
          for (int i = 0; i < b.length; i++) {
            bArr[i] = b[i];
          }
          return bArr;
        } catch (ClassCastException e) {
          Object[] b = (Object[]) obj;
          Byte[] bArr = new Byte[b.length];
          for (int i = 0; i < b.length; i++) {
            bArr[i] = Byte.valueOf(String.valueOf(b[i]));
          }
          return bArr;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return Byte.valueOf(String.valueOf(obj)).byteValue();
      }
    },
    DOUBLE(double.class, Double.class, "[D", "double", "java.lang.Double") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          double[] d = (double[]) obj;
          Double[] dArr = new Double[d.length];
          for (int i = 0; i < d.length; i++) {
            dArr[i] = d[i];
          }
          return dArr;
        } catch (ClassCastException e) {
          Object[] d = (Object[]) obj;
          Double[] dArr = new Double[d.length];
          for (int i = 0; i < d.length; i++) {
            dArr[i] = Double.valueOf(String.valueOf(d[i]));
          }
          return dArr;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return Double.valueOf(String.valueOf(obj)).doubleValue();
      }
    },
    FLOAT(float.class, Float.class, "[F", "float", "java.lang.Float") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          float[] f = (float[]) obj;
          Float[] fArr = new Float[f.length];
          for (int i = 0; i < f.length; i++) {
            fArr[i] = f[i];
          }
          return fArr;
        } catch (ClassCastException e) {
          Object[] f = (Object[]) obj;
          Float[] fArr = new Float[f.length];
          for (int i = 0; i < f.length; i++) {
            fArr[i] = Float.valueOf(String.valueOf(f[i]));
          }
          return fArr;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return Float.valueOf(String.valueOf(obj)).floatValue();
      }
    },
    INTEGER(int.class, Integer.class, "[I", "int", "java.lang.Integer") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          int[] in = (int[]) obj;
          Integer[] iArr = new Integer[in.length];
          for (int i = 0; i < in.length; i++) {
            iArr[i] = in[i];
          }
          return iArr;
        } catch (ClassCastException e) {
          Object[] array = (Object[]) obj;
          Integer[] iArr = new Integer[array.length];
          for (int i = 0; i < array.length; i++) {
            iArr[i] = Integer.valueOf(String.valueOf(array[i]));
          }
          return iArr;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return (int) Integer.valueOf(String.valueOf(obj)).intValue();
      }
    },
    SHORT(short.class, Short.class, "[S", "short", "java.lang.Short") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          short[] s = (short[]) obj;
          Short[] sArr = new Short[s.length];
          for (int i = 0; i < s.length; i++) {
            sArr[i] = s[i];
          }
          return sArr;
        } catch (ClassCastException e) {
          Object[] array = (Object[]) obj;
          Short[] sArr = new Short[array.length];
          for (int i = 0; i < array.length; i++) {
            sArr[i] = Short.valueOf(String.valueOf(array[i]));
          }
          return sArr;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return Short.valueOf(String.valueOf(obj)).shortValue();
      }
    },
    LONG(long.class, Long.class, "[J", "long", "java.lang.Long") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          long[] l = (long[]) obj;
          Long[] lArr = new Long[l.length];
          for (int i = 0; i < l.length; i++) {
            lArr[i] = l[i];
          }
          return lArr;
        } catch (ClassCastException e) {
          Object[] l = (Object[]) obj;
          Long[] lArr = new Long[l.length];
          for (int i = 0; i < l.length; i++) {
            lArr[i] = Long.valueOf(String.valueOf(l[i]));
          }
          return lArr;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return Long.valueOf(String.valueOf(obj)).longValue();
      }
    },
    CHAR(char.class, Character.class, "[C", "char", "java.lang.Character") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          char[] c = (char[]) obj;
          Character[] cArr = new Character[c.length];
          for (int i = 0; i < c.length; i++) {
            cArr[i] = c[i];
          }
          return cArr;
        } catch (ClassCastException e) {
          Object[] c = (Object[]) obj;
          Character[] cArr = new Character[c.length];
          for (int i = 0; i < c.length; i++) {
            Object val = c[i];
            if (val instanceof String) {
              String valStr = (String) val;
              if (valStr.length() != 1) {
                throw new IllegalArgumentException(
                    "Invalid input; expected character got String: " + valStr);
              }
              cArr[i] = valStr.charAt(0);
            } else {
              cArr[i] = (char) c[i];
            }
          }
          return cArr;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        if (obj instanceof String) {
          String stringVal = String.valueOf(obj);
          if (stringVal.length() != 1) {
            throw new IllegalArgumentException(
                "Invalid input ; expected character got String: " + stringVal);
          }
          return stringVal.charAt(0);
        }
        return (char) obj;
      }
    },
    BOOLEAN(boolean.class, Boolean.class, "[Z", "boolean", "java.lang.Boolean") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          boolean[] bo = (boolean[]) obj;
          Boolean[] boArr = new Boolean[bo.length];
          for (int i = 0; i < bo.length; i++) {
            boArr[i] = bo[i];
          }
          return boArr;
        } catch (ClassCastException e) {
          Object[] bo = (Object[]) obj;
          Boolean[] boArr = new Boolean[bo.length];
          for (int i = 0; i < bo.length; i++) {
            boArr[i] = Boolean.valueOf(String.valueOf(bo[i]));
          }
          return boArr;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return String.valueOf(obj).equalsIgnoreCase("true");
      }
    },
    STRING(String.class, String.class, "String", "java.lang.String") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          return (String[]) obj;
        } catch (ClassCastException e) {
          Object[] array = (Object[]) obj;
          String[] ret = new String[array.length];
          for (int i = 0; i < ret.length; i++) {
            ret[i] = String.valueOf(array[i]);
          }
          return ret;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return String.valueOf(obj);
      }
    },
    BIG_INTEGER(BigInteger.class, BigInteger.class, "BigInteger", "java.math.BigInteger") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          // transform BigIntegers to long arrays
          BigInteger[] bigIntegerArray = (BigInteger[]) obj;
          Long[] bigIntRet = new Long[bigIntegerArray.length];
          for (int i = 0; i < bigIntegerArray.length; i++) {
            bigIntRet[i] = bigIntegerArray[i].longValueExact();
          }
          return bigIntRet;
        } catch (ClassCastException e) {
          Object[] array = (Object[]) obj;
          BigInteger[] ret = new BigInteger[array.length];
          for (int i = 0; i < array.length; i++) {
            ret[i] = new BigInteger(String.valueOf(array[i]));
          }
          return ret;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return new BigInteger(String.valueOf(obj));
      }
    },
    BIG_DECIMAL(BigDecimal.class, BigDecimal.class, "BigDecimal", "java.math.BigDecimal") {
      @Override
      public Object[] castToArray(Object obj) {
        try {
          // transform BigDecimals to double arrays
          BigDecimal[] bigDecimalArray = (BigDecimal[]) obj;
          Double[] bigDecRet = new Double[bigDecimalArray.length];
          for (int i = 0; i < bigDecimalArray.length; i++) {
            bigDecRet[i] = bigDecimalArray[i].doubleValue();
          }
          return bigDecRet;
        } catch (ClassCastException e) {
          Object[] array = (Object[]) obj;
          BigDecimal[] ret = new BigDecimal[array.length];
          for (int i = 0; i < ret.length; i++) {
            ret[i] = new BigDecimal(String.valueOf(array[i]));
          }
          return ret;
        }
      }

      @Override
      public Object resolveToPrimitiveType(Object obj) {
        return new BigDecimal(String.valueOf(obj));
      }
    };

    public static PrimitiveNameTypes getNameType(String typeName) {
      for (PrimitiveNameTypes val : PrimitiveNameTypes.values()) {
        for (String name : val.names) {
          if (name.equalsIgnoreCase(typeName)) {
            return val;
          }
        }
      }
      return null;
    }

    private final String[] names;
    public final Class<?> primitiveType;

    public final Class<?> objectClass;

    PrimitiveNameTypes(Class<?> primitiveType, Class<?> objectClass, String... names) {
      this.primitiveType = primitiveType;
      this.objectClass = objectClass;
      this.names = names;
    }

    public abstract Object[] castToArray(Object array);

    public abstract Object resolveToPrimitiveType(Object obj);
  }
}
