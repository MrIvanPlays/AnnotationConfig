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

import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullUnmarked;

/**
 * A class, which stores read from config values or serialized values in a developer friendly way.
 *
 * @since 2.0.0
 * @author Ivan Pekov
 */
@SuppressWarnings({"unchecked", "unused"})
@NullUnmarked
public final class DataObject {

  /**
   * Creates a new {@link DataObjectBuilder}.
   *
   * @return new builder
   */
  public static DataObjectBuilder builder() {
    return new DataObjectBuilder();
  }

  // the entries of a map; for an immutable data object created from a map, made from source on
  // first use (see entries()). Volatile, as that can happen on any thread.
  private volatile Map<String, DataObject> serialize;
  // the map an immutable data object was created from. Only read: wrapping every value of a big
  // map, then every value of its maps, was the main cost of loading a config, and most of it is
  // only read once through getAsMap().
  private final Map<?, ?> source;
  private final Object data;

  private final boolean immutable;

  /** Constructs an empty data object */
  public DataObject() {
    this.serialize = null;
    this.source = null;
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
   * object and in the created data object delegates immutable/unmodifiable. An immutable data
   * object reads a map it holds when it is first accessed, without copying it first, so the map
   * should not be changed afterwards.
   *
   * @param data the data which is stored
   * @param immutable whether immutable
   */
  public DataObject(Object data, boolean immutable) {
    this.immutable = immutable;
    if (data == null) {
      this.data = null;
      this.serialize = null;
      this.source = null;
      return;
    }
    if (data instanceof Map<?, ?> map) {
      this.data = null;
      if (immutable) {
        this.serialize = null;
        this.source = map;
      } else {
        this.serialize = wrap(map, false);
        this.source = null;
      }
      return;
    }
    this.data = data;
    this.serialize = null;
    this.source = null;
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
   * object and in the created data object delegates immutable/unmodifiable. An immutable data
   * object reads the map when it is first accessed, without copying it first, so the map should
   * not be changed afterwards.
   *
   * @param data the data which is stored
   * @param immutable whether immutable
   */
  public DataObject(Map<String, Object> data, boolean immutable) {
    this.data = null;
    this.immutable = immutable;
    if (data != null && immutable) {
      this.serialize = null;
      this.source = data;
    } else {
      this.serialize = data == null ? new LinkedHashMap<>() : wrap(data, immutable);
      this.source = null;
    }
  }

  /** Wraps every value of {@code map}; keys that are not strings are turned into their text. */
  private static Map<String, DataObject> wrap(Map<?, ?> map, boolean immutable) {
    return MapUtils.copyWithStringKeys(map, value -> new DataObject(value, immutable));
  }

  /**
   * The entries of the map this data object holds, or {@code null} for a single value. An
   * immutable data object wraps its map here, on first use: the values are wrapped lazily too, so
   * this costs one map of this level, not a copy of everything below it.
   */
  private Map<String, DataObject> entries() {
    Map<String, DataObject> entries = this.serialize;
    if (entries == null && this.source != null) {
      // two threads may both wrap the map; the results are equal, and either is kept
      entries = wrap(this.source, true);
      this.serialize = entries;
    }
    return entries;
  }

  /**
   * Returns whether this data object is empty.
   *
   * @return boolean value
   */
  public boolean isEmpty() {
    if (isSingleValue()) {
      return false;
    }
    Map<String, DataObject> entries = this.serialize;
    return entries != null ? entries.isEmpty() : this.source.isEmpty();
  }

  /**
   * Returns whether this data object holds a single value and not a map.
   *
   * @return boolean value
   */
  public boolean isSingleValue() {
    return this.serialize == null && this.source == null;
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
    Map<String, DataObject> entries = entries();
    if (entries == null) {
      return false;
    }
    return entries.containsKey(key);
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
    Map<String, DataObject> entries = entries();
    if (entries == null) {
      return null;
    }
    return entries.get(key);
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
   * <p>Values are returned as they are held, like {@link #getAsObject()} returns them: a {@link
   * BigDecimal} or {@link BigInteger} is not turned into a {@code double} or {@code int}.
   *
   * @return data as base serialization map
   */
  public Map<String, Object> getAsMap() {
    Map<String, DataObject> entries = this.serialize;
    if (entries == null) {
      if (this.source == null) {
        return Collections.emptyMap();
      }
      // copied straight from the source, without wrapping it first
      return MapUtils.deepCopyWithStringKeys(this.source);
    }
    Map<String, Object> ret = LinkedHashMap.newLinkedHashMap(entries.size());
    for (Map.Entry<String, DataObject> entry : entries.entrySet()) {
      String key = entry.getKey();
      DataObject value = entry.getValue();
      if (value.isSingleValue()) {
        // getAsObject(true) lost precision and threw for integers beyond the int range
        ret.put(key, value.getAsObject());
      } else {
        ret.put(key, value.getAsMap());
      }
    }
    return ret;
  }

  /**
   * The entries of this data object for reading only, without the deep copy of {@link
   * #getAsMap()}: the values that are maps are the maps themselves, not copies. An empty map for a
   * single value. Callers must not modify the returned map or the maps in it.
   */
  Map<String, Object> mapView() {
    if (this.serialize == null && this.source != null) {
      // a key that isn't a string (e.g. a YAML key read as a number) is looked up by its text, as
      // getAsMap() does
      return MapUtils.withStringKeys(this.source);
    }
    return getAsMap();
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
      return "DataObject{value=" + entries() + ", immutable=" + immutable + "}";
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
      return other.entries().equals(this.entries());
    }
  }

  @Override
  public int hashCode() {
    Map<String, DataObject> entries = entries();
    int result = entries != null ? entries.hashCode() : 0;
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
