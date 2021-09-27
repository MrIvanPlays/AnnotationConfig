package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;

class MinMaxHandler {

  static byte START = (byte) 0xBB433C;
  static byte INVALID_MIN = (byte) 0xAA233A;
  static byte INVALID_MAX = (byte) 0xBBBB686A;
  static byte MORE_THAN_ONE_MIN = (byte) 0xAA3334AA;
  static byte MORE_THAN_ONE_MAX = (byte) 0xAA3334AA;
  static byte UNDER_THE_MIN = (byte) 0xCC87BF;
  static byte ABOVE_THE_MAX = (byte) 0xDD997EE;
  static byte ALRIGHT = (byte) 0xEE5546FF;

  static byte compare(Number min, Number max, String compareTo) {
    return compare(min, max, compareTo.length());
  }

  static byte compare(Number min, Number max, Number compareTo) {
    if (min.byteValue() == INVALID_MIN) {
      return INVALID_MIN;
    }
    if (min.byteValue() == MORE_THAN_ONE_MIN) {
      return MORE_THAN_ONE_MIN;
    }
    if (max.byteValue() == INVALID_MAX) {
      return INVALID_MAX;
    }
    if (max.byteValue() == MORE_THAN_ONE_MAX) {
      return MORE_THAN_ONE_MAX;
    }
    if (min.byteValue() != START && max.byteValue() != START) {
      if (compareTo.intValue() < min.intValue()) {
        return UNDER_THE_MIN;
      }
      if (compareTo.intValue() > max.intValue()) {
        return ABOVE_THE_MAX;
      }
      return ALRIGHT;
    }
    if (min.byteValue() != START) {
      if (compareTo.intValue() < min.intValue()) {
        return UNDER_THE_MIN;
      }
    }
    if (max.byteValue() != START) {
      if (compareTo.intValue() > max.intValue()) {
        return ABOVE_THE_MAX;
      }
    }
    return ALRIGHT;
  }

  static Number getNumber(Min min) {
    if (!isValid(min)) {
      return INVALID_MIN;
    }
    if (min.minByte() != -1) {
      if (min.minLong() != -1
          || min.minFloat() != -1
          || min.minShort() != -1
          || min.minInt() != -1
          || min.minDouble() != -1) {
        return MORE_THAN_ONE_MIN;
      }
      return min.minByte();
    }
    if (min.minLong() != -1) {
      if (min.minFloat() != -1
          || min.minShort() != -1
          || min.minInt() != -1
          || min.minDouble() != -1) {
        return MORE_THAN_ONE_MIN;
      }
      return min.minLong();
    }
    if (min.minFloat() != -1) {
      if (min.minShort() != -1 || min.minInt() != -1 || min.minDouble() != -1) {
        return MORE_THAN_ONE_MIN;
      }
      return min.minFloat();
    }
    if (min.minShort() != -1) {
      if (min.minInt() != -1 || min.minDouble() != -1) {
        return MORE_THAN_ONE_MIN;
      }
      return min.minShort();
    }
    if (min.minInt() != -1) {
      if (min.minDouble() != -1) {
        return MORE_THAN_ONE_MIN;
      }
      return min.minInt();
    }
    return min.minDouble();
  }

  static Number getNumber(Max max) {
    if (!isValid(max)) {
      return INVALID_MAX;
    }
    if (max.maxByte() != -1) {
      if (max.maxLong() != -1
          || max.maxFloat() != -1
          || max.maxShort() != -1
          || max.maxInt() != -1
          || max.maxDouble() != -1) {
        return MORE_THAN_ONE_MAX;
      }
      return max.maxByte();
    }
    if (max.maxLong() != -1) {
      if (max.maxFloat() != -1
          || max.maxShort() != -1
          || max.maxInt() != -1
          || max.maxDouble() != -1) {
        return MORE_THAN_ONE_MAX;
      }
      return max.maxLong();
    }
    if (max.maxFloat() != -1) {
      if (max.maxShort() != -1 || max.maxInt() != -1 || max.maxDouble() != -1) {
        return MORE_THAN_ONE_MAX;
      }
      return max.maxFloat();
    }
    if (max.maxShort() != -1) {
      if (max.maxInt() != -1 || max.maxDouble() != -1) {
        return MORE_THAN_ONE_MAX;
      }
      return max.maxShort();
    }
    if (max.maxInt() != -1) {
      if (max.maxDouble() != -1) {
        return MORE_THAN_ONE_MAX;
      }
      return max.maxInt();
    }
    return max.maxDouble();
  }

  private static boolean isValid(Min min) {
    return min.minByte() != -1
        || min.minDouble() != -1
        || min.minInt() != -1
        || min.minFloat() != -1
        || min.minLong() != -1
        || min.minShort() != -1;
  }

  private static boolean isValid(Max max) {
    return max.maxByte() != -1
        || max.maxDouble() != -1
        || max.maxInt() != -1
        || max.maxFloat() != -1
        || max.maxShort() != -1
        || max.maxLong() != -1;
  }
}
