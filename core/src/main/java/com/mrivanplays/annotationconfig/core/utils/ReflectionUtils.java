package com.mrivanplays.annotationconfig.core.utils;

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

  private ReflectionUtils() {
    throw new IllegalArgumentException("Instantiation of utility-type class.");
  }

  private enum PrimitiveNameTypes {
    BYTE(byte.class, "byte", "java.lang.Byte") {
      @Override
      public Object[] castToArray(Object obj) {
        byte[] b = (byte[]) obj;
        Byte[] bArr = new Byte[b.length];
        for (int i = 0; i < b.length; i++) {
          bArr[i] = b[i];
        }
        return bArr;
      }
    },
    DOUBLE(double.class, "double", "java.lang.Double") {
      @Override
      public Object[] castToArray(Object obj) {
        double[] d = (double[]) obj;
        Double[] dArr = new Double[d.length];
        for (int i = 0; i < d.length; i++) {
          dArr[i] = d[i];
        }
        return dArr;
      }
    },
    FLOAT(float.class, "float", "java.lang.Float") {
      @Override
      public Object[] castToArray(Object obj) {
        float[] f = (float[]) obj;
        Float[] fArr = new Float[f.length];
        for (int i = 0; i < f.length; i++) {
          fArr[i] = f[i];
        }
        return fArr;
      }
    },
    INTEGER(int.class, "int", "java.lang.Integer") {
      @Override
      public Object[] castToArray(Object obj) {
        int[] in = (int[]) obj;
        Integer[] iArr = new Integer[in.length];
        for (int i = 0; i < in.length; i++) {
          iArr[i] = in[i];
        }
        return iArr;
      }
    },
    SHORT(short.class, "short", "java.lang.Short") {
      @Override
      public Object[] castToArray(Object obj) {
        short[] s = (short[]) obj;
        Short[] sArr = new Short[s.length];
        for (int i = 0; i < s.length; i++) {
          sArr[i] = s[i];
        }
        return sArr;
      }
    },
    LONG(long.class, "long", "java.lang.Long") {
      @Override
      public Object[] castToArray(Object obj) {
        long[] l = (long[]) obj;
        Long[] lArr = new Long[l.length];
        for (int i = 0; i < l.length; i++) {
          lArr[i] = l[i];
        }
        return lArr;
      }
    },
    CHAR(char.class, "char", "java.lang.Character") {
      @Override
      public Object[] castToArray(Object obj) {
        char[] c = (char[]) obj;
        Character[] cArr = new Character[c.length];
        for (int i = 0; i < c.length; i++) {
          cArr[i] = c[i];
        }
        return cArr;
      }
    },
    BOOLEAN(boolean.class, "boolean", "java.lang.Boolean") {
      @Override
      public Object[] castToArray(Object obj) {
        boolean[] bo = (boolean[]) obj;
        Boolean[] boArr = new Boolean[bo.length];
        for (int i = 0; i < bo.length; i++) {
          boArr[i] = bo[i];
        }
        return boArr;
      }
    },
    STRING(String.class, "String", "java.lang.String") {
      @Override
      public Object[] castToArray(Object obj) {
        return (String[]) obj;
      }
    },
    BIG_INTEGER(BigInteger.class, "BigInteger", "java.math.BigInteger") {
      @Override
      public Object[] castToArray(Object obj) {
        // transform BigIntegers to long arrays
        BigInteger[] bigIntegerArray = (BigInteger[]) obj;
        Long[] bigIntRet = new Long[bigIntegerArray.length];
        for (int i = 0; i < bigIntegerArray.length; i++) {
          bigIntRet[i] = bigIntegerArray[i].longValueExact();
        }
        return bigIntRet;
      }
    },
    BIG_DECIMAL(BigDecimal.class, "BigDecimal", "java.math.BigDecimal") {
      @Override
      public Object[] castToArray(Object obj) {
        // transform BigDecimals to double arrays
        BigDecimal[] bigDecimalArray = (BigDecimal[]) obj;
        Double[] bigDecRet = new Double[bigDecimalArray.length];
        for (int i = 0; i < bigDecimalArray.length; i++) {
          bigDecRet[i] = bigDecimalArray[i].doubleValue();
        }
        return bigDecRet;
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

    PrimitiveNameTypes(Class<?> primitiveType, String... names) {
      this.primitiveType = primitiveType;
      this.names = names;
    }

    public abstract Object[] castToArray(Object array);
  }
}
