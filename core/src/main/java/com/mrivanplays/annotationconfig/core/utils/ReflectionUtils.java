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
    switch (nameType) {
      case BYTE:
        byte[] b = (byte[]) obj;
        Byte[] bArr = new Byte[b.length];
        for (int i = 0; i < b.length; i++) {
          bArr[i] = b[i];
        }
        return bArr;
      case DOUBLE:
        double[] d = (double[]) obj;
        Double[] dArr = new Double[d.length];
        for (int i = 0; i < d.length; i++) {
          dArr[i] = d[i];
        }
        return dArr;
      case FLOAT:
        float[] f = (float[]) obj;
        Float[] fArr = new Float[f.length];
        for (int i = 0; i < f.length; i++) {
          fArr[i] = f[i];
        }
        return fArr;
      case INTEGER:
        int[] in = (int[]) obj;
        Integer[] iArr = new Integer[in.length];
        for (int i = 0; i < in.length; i++) {
          iArr[i] = in[i];
        }
        return iArr;
      case SHORT:
        short[] s = (short[]) obj;
        Short[] sArr = new Short[s.length];
        for (int i = 0; i < s.length; i++) {
          sArr[i] = s[i];
        }
        return sArr;
      case LONG:
        long[] l = (long[]) obj;
        Long[] lArr = new Long[l.length];
        for (int i = 0; i < l.length; i++) {
          lArr[i] = l[i];
        }
        return lArr;
      case CHAR:
        char[] c = (char[]) obj;
        Character[] cArr = new Character[c.length];
        for (int i = 0; i < c.length; i++) {
          cArr[i] = c[i];
        }
        return cArr;
      case BOOLEAN:
        boolean[] bo = (boolean[]) obj;
        Boolean[] boArr = new Boolean[bo.length];
        for (int i = 0; i < bo.length; i++) {
          boArr[i] = bo[i];
        }
        return boArr;
      case STRING:
        return (String[]) obj;
      case BIG_INTEGER:
        // transform BigIntegers to long arrays
        BigInteger[] bigIntegerArray = (BigInteger[]) obj;
        Long[] bigIntRet = new Long[bigIntegerArray.length];
        for (int i = 0; i < bigIntegerArray.length; i++) {
          bigIntRet[i] = bigIntegerArray[i].longValueExact();
        }
        return bigIntRet;
      case BIG_DECIMAL:
        // transform BigDecimals to double arrays
        BigDecimal[] bigDecimalArray = (BigDecimal[]) obj;
        Double[] bigDecRet = new Double[bigDecimalArray.length];
        for (int i = 0; i < bigDecimalArray.length; i++) {
          bigDecRet[i] = bigDecimalArray[i].doubleValue();
        }
        return bigDecRet;
      default:
        throw new IllegalArgumentException("Something went wrong with casting Object to an array");
    }
  }

  private ReflectionUtils() {
    throw new IllegalArgumentException("Instantiation of utility-type class.");
  }

  private enum PrimitiveNameTypes {
    BYTE(byte.class, "byte", "java.lang.Byte"),
    DOUBLE(double.class, "double", "java.lang.Double"),
    FLOAT(float.class, "float", "java.lang.Float"),
    INTEGER(int.class, "int", "java.lang.Integer"),
    SHORT(short.class, "short", "java.lang.Short"),
    LONG(long.class, "long", "java.lang.Long"),
    CHAR(char.class, "char", "java.lang.Character"),
    BOOLEAN(boolean.class, "boolean", "java.lang.Boolean"),
    STRING(String.class, "String", "java.lang.String"),
    BIG_INTEGER(BigInteger.class, "BigInteger", "java.math.BigInteger"),
    BIG_DECIMAL(BigDecimal.class, "BigDecimal", "java.math.BigDecimal");

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
  }
}
