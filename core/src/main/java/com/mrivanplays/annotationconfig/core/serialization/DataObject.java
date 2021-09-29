package com.mrivanplays.annotationconfig.core.serialization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A class, which stores read from config values or serialized values in a developer friendly way.
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
public final class DataObject {

  private Map<String, DataObject> serialize;
  private final Object data;

  /** Constructs a empty data object */
  public DataObject() {
    this.serialize = null;
    this.data = null;
  }

  /**
   * Constructs a data object, which holds the specified data. If the data is of map type, it gets
   * converted, so it's values are accessible
   *
   * @param data the data which is stored
   */
  public DataObject(Object data) {
    if (data instanceof Map) {
      Map<Object, Object> map = (Map<Object, Object>) data;
      this.serialize = new LinkedHashMap<>();
      for (Map.Entry<Object, Object> entry : map.entrySet()) {
        this.serialize.put(String.valueOf(entry.getKey()), new DataObject(entry.getValue()));
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
    this.data = null;
    this.serialize = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      this.serialize.put(entry.getKey(), new DataObject(entry.getValue()));
    }
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
   * Returns whether the specified key has a value in this data object.
   *
   * @param key the key you want to check for a value
   * @return boolean value
   */
  public boolean has(String key) {
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
        ret.put(key, value.getAsObject());
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
    List<T> ret = new ArrayList<>();
    for (Object val : base) {
      if (val.getClass().isAssignableFrom(valueClass)) {
        ret.add(valueClass.cast(val));
      }
    }
    return ret;
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
}
