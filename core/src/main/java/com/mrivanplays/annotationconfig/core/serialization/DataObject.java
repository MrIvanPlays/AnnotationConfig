package com.mrivanplays.annotationconfig.core.serialization;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A class, which stores read from config values or serialized values in a developer friendly way.
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
public final class DataObject {

  /**
   * Creates a new {@link DataObjectBuilder}.
   *
   * @return new builder
   */
  public static DataObjectBuilder builder() {
    return new DataObjectBuilder();
  }

  private Map<String, DataObject> serialize;
  private final Object data;

  private final boolean immutable;

  /** Constructs a empty data object */
  public DataObject() {
    this.serialize = null;
    this.data = null;
    this.immutable = false;
  }

  /**
   * Constructs a data object, which holds the specified data. If the data is of map type, it gets
   * converted, so it's values are accessible
   *
   * @param data the data which is stored
   */
  public DataObject(Object data) {
    this(data, false);
  }

  /**
   * Constructs a data object, which holds the specified data. If the data is of map type, it gets
   * converted, so it's values are accessible.
   *
   * <p>The {@code immutable} tag if set to {@code true} will make the data held in the created data
   * object and in the created data object delegates immutable/unmodifiable.
   *
   * @param data the data which is stored
   * @param immutable whether immutable
   */
  public DataObject(Object data, boolean immutable) {
    this.immutable = immutable;
    if (data == null) {
      this.data = null;
      this.serialize = null;
      return;
    }
    if (data instanceof Map) {
      Map<Object, Object> map = (Map<Object, Object>) data;
      this.serialize = new LinkedHashMap<>();
      for (Map.Entry<Object, Object> entry : map.entrySet()) {
        this.serialize.put(
            String.valueOf(entry.getKey()), new DataObject(entry.getValue(), immutable));
      }
      this.data = null;
      return;
    }
    this.data = data;
  }

  /**
   * Constructs a data object, which holds the specified map data.
   *
   * @param data the data which is stored
   */
  public DataObject(Map<String, Object> data) {
    this(data, false);
  }

  /**
   * Constructs a data object, which holds the specified map data.
   *
   * <p>The {@code immutable} tag if set to {@code true} will make the data held in the created data
   * object and in the created data object delegates immutable/unmodifiable.
   *
   * @param data the data which is stored
   * @param immutable whether immutable
   */
  public DataObject(Map<String, Object> data, boolean immutable) {
    this.data = null;
    this.serialize = new LinkedHashMap<>();
    this.immutable = immutable;
    if (data != null) {
      for (Map.Entry<String, Object> entry : data.entrySet()) {
        this.serialize.put(entry.getKey(), new DataObject(entry.getValue(), immutable));
      }
    }
  }

  /**
   * Returns whether this data object is empty.
   *
   * @return boolean value
   */
  public boolean isEmpty() {
    if (!isSingleValue()) {
      if (this.serialize != null) {
        return this.serialize.isEmpty();
      } else {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether this data object holds a single value and not a map.
   *
   * @return boolean value
   */
  public boolean isSingleValue() {
    return this.serialize == null && this.data != null;
  }

  /**
   * Returns whether this data object is immutable/unmodifiable.
   *
   * @return boolean value
   */
  public boolean isImmutable() {
    return immutable;
  }

  /**
   * Returns whether the specified key has a value in this data object.
   *
   * @param key the key you want to check for a value
   * @return boolean value
   */
  public boolean has(String key) {
    Objects.requireNonNull(key, "key");
    if (this.serialize == null) {
      return false;
    }
    return this.serialize.containsKey(key);
  }

  /**
   * Returns the {@link DataObject} value held by the specified key. If there isn't a value, {@code
   * null} is returned.
   *
   * @param key the key for which you want the data
   * @return data object or null
   */
  public DataObject get(String key) {
    Objects.requireNonNull(key, "key");
    if (this.serialize == null) {
      return null;
    }
    return this.serialize.get(key);
  }

  /**
   * Binds the specified value to the specified key
   *
   * @param key the key you want the value to be bound to
   * @param value the value you want bound
   */
  public void put(String key, String value) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
    checkImmutable("put");
    checkNonNullData("put");
    this.serialize.put(key, new DataObject(value));
  }

  /**
   * Binds the specified value to the specified key
   *
   * @param key the key you want the value to be bound to
   * @param value the value you want bound
   */
  public void put(String key, boolean value) {
    Objects.requireNonNull(key, "key");
    checkImmutable("put");
    checkNonNullData("put");
    this.serialize.put(key, new DataObject(value));
  }

  /**
   * Binds the specified value to the specified key
   *
   * @param key the key you want the value to be bound to
   * @param value the value you want bound
   */
  public void put(String key, Number value) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
    checkImmutable("put");
    checkNonNullData("put");
    this.serialize.put(key, new DataObject(value));
  }

  /**
   * Removes the value of the specified key.
   *
   * @param key the key you want the value of removed
   * @return the value removed or null if no value was present
   */
  public DataObject remove(String key) {
    Objects.requireNonNull(key, "key");
    checkImmutable("remove");
    checkNonNullData("remove");
    return this.serialize.remove(key);
  }

  /**
   * Binds the specified {@link DataObject} to the specified key.
   *
   * @param key the key you want the value to be bound to
   * @param object the value you want bound
   */
  public void putAll(String key, DataObject object) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(object, "object");
    checkImmutable("putAll");
    checkNonNullData("putAll");
    this.serialize.put(key, object);
  }

  /**
   * Returns this data object as a {@link Map} with key type of {@link String} and a value type of
   * {@link Object}. Modifying the returned map won't have any impact on this data object.
   *
   * @return data as base serialization map
   */
  public Map<String, Object> getAsMap() {
    if (this.serialize == null) {
      return Collections.emptyMap();
    }
    Map<String, Object> ret = new LinkedHashMap<>();
    for (Map.Entry<String, DataObject> entry : this.serialize.entrySet()) {
      String key = entry.getKey();
      DataObject value = entry.getValue();
      if (value.isSingleValue()) {
        ret.put(key, value.getAsObject(true));
      } else {
        ret.put(key, value.getAsMap());
      }
    }
    return ret;
  }

  /**
   * Returns the held value by this data object. If it holds a map, the returned value is null.
   *
   * @return held object
   */
  public Object getAsObject() {
    return data;
  }

  /**
   * Returns the held value by this data objects. If it holds a map, the returned value is null.
   *
   * @param serializeBigIntegerAndBigDecimal whether to serialize the value to a primitive if it is
   *     a {@link BigInteger} or {@link BigDecimal}
   * @return held object
   */
  public Object getAsObject(boolean serializeBigIntegerAndBigDecimal) {
    if (serializeBigIntegerAndBigDecimal) {
      if (data instanceof BigInteger) {
        return ((BigInteger) data).intValueExact();
      } else if (data instanceof BigDecimal) {
        return ((BigDecimal) data).doubleValue();
      } else {
        return data;
      }
    } else {
      return data;
    }
  }

  /**
   * Returns the held value by this data object as the specified {@code type}, if the held data is
   * of that type. If it holds a map, the returned value is null.
   *
   * @param type the type to transform to
   * @param <T> generic
   * @return transformed type or null if data is null
   * @throws IllegalArgumentException if the held data's type is not the specified type
   */
  public <T> T getAs(Class<T> type) {
    if (data != null) {
      if (data.getClass().isAssignableFrom(type)) {
        return type.cast(data);
      } else {
        throw new IllegalArgumentException(
            "Cannot assign " + data.getClass().getName() + " data type to " + type.getName());
      }
    }
    return null;
  }

  /**
   * Returns the held value by this data object as a {@link String}
   *
   * @return held string
   * @see #getAsObject()
   */
  public String getAsString() {
    return String.valueOf(data);
  }

  /**
   * Returns the held value by this data object as a {@link Integer}
   *
   * @return held int
   * @see #getAsObject()
   */
  public int getAsInt() {
    return Integer.parseInt(getAsString());
  }

  /**
   * Returns the held value by this data object as a {@link Boolean}
   *
   * @return held boolean
   * @see #getAsObject()
   */
  public boolean getAsBoolean() {
    return Boolean.parseBoolean(getAsString());
  }

  /**
   * Returns the held value by this data object as a {@link Byte}
   *
   * @return held byte
   * @see #getAsObject()
   */
  public byte getAsByte() {
    return Byte.parseByte(getAsString());
  }

  /**
   * Returns the held value by this data object as a {@link Character}
   *
   * @return held char
   * @see #getAsObject()
   */
  public char getAsChar() {
    return (char) data;
  }

  /**
   * Returns the held value by this data object as a {@link Double}
   *
   * @return held double
   * @see #getAsObject()
   */
  public double getAsDouble() {
    return Double.parseDouble(getAsString());
  }

  /**
   * Returns the held value by this data object as a {@link Float}
   *
   * @return held float
   * @see #getAsObject()
   */
  public float getAsFloat() {
    return Float.parseFloat(getAsString());
  }

  /**
   * Returns the held value by this data object as a {@link Long}
   *
   * @return held long
   * @see #getAsObject()
   */
  public long getAsLong() {
    return Long.parseLong(getAsString());
  }

  /**
   * Returns the held value by this data object as a {@link Short}
   *
   * @return held short
   * @see #getAsObject()
   */
  public short getAsShort() {
    return Short.parseShort(getAsString());
  }

  /**
   * Returns the held value by this data object as a {@link BigDecimal}
   *
   * @return held big decimal
   * @see #getAsObject()
   */
  public BigDecimal getAsBigDecimal() {
    if (getAsObject() instanceof BigDecimal) {
      return (BigDecimal) getAsObject();
    }
    return BigDecimal.valueOf(getAsDouble());
  }

  /**
   * Returns the held value by this data object as a {@link BigInteger}
   *
   * @return held big integer
   * @see #getAsObject()
   */
  public BigInteger getAsBigInteger() {
    if (getAsObject() instanceof BigInteger) {
      return (BigInteger) getAsObject();
    }
    return BigInteger.valueOf(getAsLong());
  }

  /**
   * Returns the held value by this data object as a {@link List} of {@link Object} . If the held
   * value isn't a list, this method will return null.
   *
   * @return list or null
   */
  public List<Object> getAsList() {
    if (!(data instanceof List)) {
      return null;
    }
    return (List<Object>) data;
  }

  /**
   * Returns the held value by this data object as a {@link List} of the specified class type. If
   * the held value isn't a list, this method will return null. Any values which aren't assignable
   * to the specified value class are skipped.
   *
   * @param valueClass the value class you want the list entries to be cast to
   * @param <T> type needed
   * @return list or null
   */
  public <T> List<T> getList(Class<T> valueClass) {
    List<Object> base = getAsList();
    if (base == null) {
      return null;
    }
    List<T> ret = new LinkedList<>();
    for (Object val : base) {
      if (val.getClass().isAssignableFrom(valueClass)) {
        ret.add(valueClass.cast(val));
      }
    }
    return ret;
  }

  @Override
  public String toString() {
    if (isSingleValue()) {
      return "DataObject{value=" + data + ", immutable=" + immutable + "}";
    } else {
      return "DataObject{value=" + serialize + ", immutable=" + immutable + "}";
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataObject other = (DataObject) o;
    if (this == other) {
      return true;
    }
    if (other.isSingleValue() && this.isSingleValue()) {
      return other.data.equals(this.data);
    } else if (!other.isSingleValue() && this.isSingleValue()) {
      return false;
    } else if (other.isSingleValue()) {
      return false;
    } else {
      return other.serialize.equals(this.serialize);
    }
  }

  @Override
  public int hashCode() {
    int result = serialize != null ? serialize.hashCode() : 0;
    result = 31 * result + (data != null ? data.hashCode() : 0);
    return result;
  }

  private void checkNonNullData(String action) {
    if (this.data != null) {
      throw new IllegalArgumentException(
          "Cannot " + action + " in a DataObject which is single value");
    }
    if (this.serialize == null) {
      this.serialize = new LinkedHashMap<>();
    }
  }

  private void checkImmutable(String action) {
    if (immutable) {
      throw new UnsupportedOperationException("Cannot " + action + " in a immutable DataObject");
    }
  }
}
