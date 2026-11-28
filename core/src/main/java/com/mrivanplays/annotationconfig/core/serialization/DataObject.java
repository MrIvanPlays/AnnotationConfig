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

import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.utils.MapUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A class, which stores read from config values or serialized values in a developer friendly way.
 *
 * @since 2.0.0
 * @author Ivan Pekov
 */
@SuppressWarnings("unchecked")
@NullMarked
public final class DataObject {

  // the map an immutable data object was created from. Only read: wrapping every value of a big
  // map, then every value of its maps, was the main cost of loading a config, and most of it is
  // only read once through getAsMap().
  private final @Nullable Map<?, ?> source;
  private final @Nullable Object data;
  private final boolean immutable;
  // the entries of a map; for an immutable data object created from a map, made from source on
  // first use (see entries()). Volatile, as that can happen on any thread.
  private volatile @Nullable Map<String, DataObject> serialize;

  /**
   * Constructs an empty data object: a map without entries, to {@link #put(String, String) put}
   * values into. It is {@link #isEmpty() empty} and not a {@link #isSingleValue() single value}.
   */
  public DataObject() {
    // it used to hold no map until a value was put, so it was a single value (null) and not empty
    this.serialize = new LinkedHashMap<>();
    this.source = null;
    this.data = null;
    this.immutable = false;
  }

  /**
   * Constructs a data object, which holds the specified data. If the data is of map type, it gets
   * converted, so its values are accessible.
   *
   * @param data the data which is stored
   */
  public DataObject(@Nullable Object data) {
    this(data, false);
  }

  /**
   * Constructs a data object, which holds the specified data. If the data is of map type, it gets
   * converted, so its values are accessible.
   *
   * <p>If {@code immutable} is {@code true}, the created data object and the data objects it holds
   * can't be modified. An immutable data object reads a map it holds when it is first accessed,
   * without copying it first, so the map should not be changed afterwards.
   *
   * @param data the data which is stored
   * @param immutable whether immutable
   */
  public DataObject(@Nullable Object data, boolean immutable) {
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
  public DataObject(@Nullable Map<String, Object> data) {
    this(data, false);
  }

  /**
   * Constructs a data object, which holds the specified map data.
   *
   * <p>If {@code immutable} is {@code true}, the created data object and the data objects it holds
   * can't be modified. An immutable data object reads the map when it is first accessed, without
   * copying it first, so the map should not be changed afterwards.
   *
   * @param data the data which is stored
   * @param immutable whether immutable
   */
  public DataObject(@Nullable Map<String, Object> data, boolean immutable) {
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

  /**
   * Creates a new {@link DataObjectBuilder}.
   *
   * @return new builder
   */
  public static DataObjectBuilder builder() {
    return new DataObjectBuilder();
  }

  /** Wraps every value of {@code map}; keys that are not strings are turned into their text. */
  private static Map<String, DataObject> wrap(Map<?, ?> map, boolean immutable) {
    return MapUtils.copyWithStringKeys(map, value -> new DataObject(value, immutable));
  }

  /**
   * Removes {@code value}, which {@code keyResolver} read for {@code key}, from {@code values}:
   * from the key {@code key} itself if the resolver reads a dotted key as one key and the value is
   * stored that way, and otherwise from the sections the parts of the key name. Sections left empty
   * are removed.
   *
   * @throws IllegalArgumentException if the value is in neither place, as for a key resolver that
   *     stores values another way
   */
  @SuppressWarnings("unchecked")
  private static void removeRead(
      String key, Object value, Map<String, Object> values, KeyResolver keyResolver) {
    if (values.get(key) == value && readsWholeKey(key, keyResolver)) {
      values.remove(key);
      return;
    }
    // the maps from the top down to the one that holds the last part, and the parts of the key
    List<Map<String, Object>> maps = new ArrayList<>();
    List<String> parts = new ArrayList<>();
    Map<String, Object> current = values;
    int start = 0;
    int end = key.indexOf('.');
    while (end != -1) {
      String part = key.substring(start, end);
      if (!(current.get(part) instanceof Map<?, ?> next)) {
        throw cannotRemove(key);
      }
      maps.add(current);
      parts.add(part);
      current = (Map<String, Object>) next;
      start = end + 1;
      end = key.indexOf('.', start);
    }
    String last = key.substring(start);
    if (current.get(last) != value) {
      throw cannotRemove(key);
    }
    current.remove(last);
    // the sections on the way held the key, so the ones that are empty now were emptied by it
    for (int i = maps.size() - 1; i >= 0 && current.isEmpty(); i--) {
      current = maps.get(i);
      current.remove(parts.get(i));
    }
  }

  /** Whether {@code keyResolver} reads {@code key} as one key of the top level. */
  private static boolean readsWholeKey(String key, KeyResolver keyResolver) {
    if (key.indexOf('.') == -1) {
      return true;
    }
    Object probe = new Object();
    Map<String, Object> probed = new HashMap<>();
    probed.put(key, probe);
    try {
      return keyResolver.unbox(key, probed) == probe;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static IllegalArgumentException cannotRemove(String key) {
    return new IllegalArgumentException(
        "The key resolver read '"
            + key
            + "' from neither the key itself nor the sections its parts name, so it can't be"
            + " moved");
  }

  /**
   * {@code true}, {@code yes} and {@code on}, in any case, are {@code true}; anything else is
   * {@code false}, as {@code Boolean.parseBoolean} reads it. YAML already reads {@code yes} and
   * {@code on} as booleans; this lets the other formats read them too.
   */
  static boolean parseBoolean(String text) {
    return text.equalsIgnoreCase("true")
        || text.equalsIgnoreCase("yes")
        || text.equalsIgnoreCase("on");
  }

  /**
   * The entries of the map this data object holds, or {@code null} for a single value. An immutable
   * data object wraps its map here, on first use: the values are wrapped lazily too, so this costs
   * one map of this level, not a copy of everything below it.
   */
  private @Nullable Map<String, DataObject> entries() {
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
    if (entries != null) {
      return entries.isEmpty();
    }
    Map<?, ?> source = this.source;
    return source == null || source.isEmpty();
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
  public @Nullable DataObject get(String key) {
    Objects.requireNonNull(key, "key");
    Map<String, DataObject> entries = entries();
    if (entries == null) {
      return null;
    }
    return entries.get(key);
  }

  /**
   * Binds the specified value to the specified key.
   *
   * @param key the key you want the value to be bound to
   * @param value the value you want bound
   */
  public void put(String key, String value) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
    mutableEntries("put").put(key, new DataObject(value));
  }

  /**
   * Binds the specified value to the specified key.
   *
   * @param key the key you want the value to be bound to
   * @param value the value you want bound
   */
  public void put(String key, boolean value) {
    Objects.requireNonNull(key, "key");
    mutableEntries("put").put(key, new DataObject(value));
  }

  /**
   * Binds the specified value to the specified key.
   *
   * @param key the key you want the value to be bound to
   * @param value the value you want bound
   */
  public void put(String key, Number value) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(value, "value");
    mutableEntries("put").put(key, new DataObject(value));
  }

  /**
   * Removes the value of the specified key.
   *
   * @param key the key you want the value of removed
   * @return the value removed or null if no value was present
   */
  public @Nullable DataObject remove(String key) {
    Objects.requireNonNull(key, "key");
    return mutableEntries("remove").remove(key);
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
    mutableEntries("putAll").put(key, object);
  }

  /**
   * Moves the value of the key {@code from} to the key {@code to}, as a migration of a config
   * renames an option. If {@code to} has a value already, it is kept and the value of {@code from}
   * is dropped. The moved value comes after the other keys.
   *
   * <p>Like {@link #get(String)}, this works on the keys of this data object only: a dotted key
   * such as {@code server.port} is one key in {@code .properties}, but a key {@code port} in the
   * data object of the key {@code server} in YAML, JSON and TOML.
   *
   * @param from the key whose value to move, not empty
   * @param to the key to move the value to, not empty
   * @return whether {@code from} had a value
   * @throws UnsupportedOperationException if this data object is immutable
   * @throws IllegalArgumentException if {@code from} or {@code to} is empty, checked first, or if
   *     this data object holds a single value
   * @since 3.1.0
   */
  public boolean renameKey(String from, String to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (from.isEmpty() || to.isEmpty()) {
      throw new IllegalArgumentException(
          "The key '" + (from.isEmpty() ? "from" : "to") + "' of renameKey is empty");
    }
    Map<String, DataObject> entries = mutableEntries("renameKey");
    if (from.equals(to)) {
      return entries.containsKey(from);
    }
    // never null for a key that is present: a null value is held by a data object too
    DataObject value = entries.remove(from);
    if (value == null) {
      return false;
    }
    entries.putIfAbsent(to, value);
    return true;
  }

  /**
   * Moves the value of the key {@code from} to the key {@code to}, both read and written the way
   * {@code keyResolver} reads and writes the keys of options, so that a migration of a config can
   * move an option between sections in any format. Pass the key resolver the values were read with:
   * in a {@link com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategy
   * MigrationStrategy}, the one it gets. It is {@link KeyResolver#DEFAULT} for {@code .properties},
   * which keeps {@code server.port} as one key, and {@link
   * com.mrivanplays.annotationconfig.core.resolver.key.DottedResolver DottedResolver} for YAML and
   * JSON, which read it as the key {@code port} of the section {@code server}.
   *
   * <p>As with {@link #renameKey(String, String)}, if {@code to} has a value already, it is kept
   * and the value of {@code from} is dropped. A section that the move leaves empty is removed. A
   * key that holds {@code null} counts as missing, as key resolvers read it the same way, and so
   * does a key whose section is a value ({@code server: 5} for {@code server.port}).
   *
   * <p>The values are copied, moved and wrapped again: data objects got from this one before are no
   * longer part of it once a value was moved.
   *
   * @param from the key whose value to move, not empty
   * @param to the key to move the value to, not empty
   * @param keyResolver the key resolver that reads and writes the keys
   * @return whether {@code from} had a value
   * @throws UnsupportedOperationException if this data object is immutable
   * @throws IllegalArgumentException if {@code from} or {@code to} is empty, checked first, if this
   *     data object holds a single value, or if {@code keyResolver} can't write {@code to}, e.g. as
   *     a part of it is a value and not a section; nothing is changed then
   * @since 3.1.0
   */
  public boolean renameDottedKey(String from, String to, KeyResolver keyResolver) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    Objects.requireNonNull(keyResolver, "keyResolver");
    if (from.isEmpty() || to.isEmpty()) {
      throw new IllegalArgumentException(
          "The key '" + (from.isEmpty() ? "from" : "to") + "' of renameDottedKey is empty");
    }
    // first, so that an immutable data object fails before anything is read
    final Map<String, DataObject> entries = mutableEntries("renameDottedKey");
    // a copy: if writing the new key fails, this data object is left as it was
    Map<String, Object> values = getAsMap();
    Object value;
    try {
      value = keyResolver.unbox(from, values);
    } catch (IllegalArgumentException e) {
      // DottedResolver fails when a part of the key is a value: then there is nothing to move
      return false;
    }
    if (value == null) {
      return false;
    }
    if (from.equals(to)) {
      return true;
    }
    removeRead(from, value, values, keyResolver);
    // a value under the new key is kept, as by renameKey
    if (keyResolver.unbox(to, values) == null) {
      keyResolver.boxTo(to, value, values);
    }
    entries.clear();
    entries.putAll(wrap(values, false));
    return true;
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
      Map<?, ?> source = this.source;
      if (source == null) {
        return new LinkedHashMap<>();
      }
      // copied straight from the source, without wrapping it first
      return MapUtils.deepCopyWithStringKeys(source);
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
   * The entries of this data object for reading only, without the deep copy of {@link #getAsMap()}:
   * the values that are maps are the maps themselves, not copies. An empty map for a single value.
   * Callers must not modify the returned map or the maps in it.
   */
  Map<String, Object> mapView() {
    Map<?, ?> source = this.source;
    if (this.serialize == null && source != null) {
      // a key that isn't a string (e.g. a YAML key read as a number) is looked up by its text, as
      // getAsMap() does
      return MapUtils.withStringKeys(source);
    }
    return getAsMap();
  }

  /**
   * Returns the held value by this data object. If it holds a map, the returned value is null.
   *
   * @return held object
   */
  public @Nullable Object getAsObject() {
    return data;
  }

  /**
   * Returns the held value by this data object. If it holds a map, the returned value is null.
   *
   * @param serializeBigIntegerAndBigDecimal whether to serialize the value to a primitive if it is
   *     a {@link BigInteger} or {@link BigDecimal}
   * @return held object
   */
  public @Nullable Object getAsObject(boolean serializeBigIntegerAndBigDecimal) {
    if (serializeBigIntegerAndBigDecimal) {
      if (data instanceof BigInteger integer) {
        return integer.intValueExact();
      }
      if (data instanceof BigDecimal decimal) {
        return decimal.doubleValue();
      }
    }
    return data;
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
  public <T> @Nullable T getAs(Class<T> type) {
    Object data = this.data;
    if (data == null) {
      return null;
    }
    if (!type.isInstance(data)) {
      throw new IllegalArgumentException(
          "Cannot assign " + data.getClass().getName() + " data type to " + type.getName());
    }
    return type.cast(data);
  }

  /**
   * Returns the held value by this data object as a {@link String}.
   *
   * @return held string
   * @see #getAsObject()
   */
  public String getAsString() {
    return String.valueOf(data);
  }

  /**
   * Returns the held value by this data object as an {@code int}, read as an {@code int} field
   * reads it: a decimal, such as a {@code 1.0} read by YAML, is cut at its decimal point.
   *
   * @return held int
   * @throws NumberFormatException if the held value is no number
   * @see #getAsObject()
   */
  public int getAsInt() {
    return (Integer) number(int.class);
  }

  /**
   * The held value as a field of {@code type} reads it. The getters used to parse its text, so a
   * {@code 1.0} that a field read as {@code 1} failed in {@link #getAsInt()}.
   */
  private Object number(Class<?> type) {
    Object data = this.data;
    Object number = data == null ? null : DefaultSerializer.readScalar(data, type);
    if (number == null) {
      throw new NumberFormatException("Expected a number, got: " + data);
    }
    return number;
  }

  /**
   * Returns the held value by this data object as a {@link Boolean}. {@code true}, {@code yes} and
   * {@code on} are {@code true}, in any case, as YAML 1.1 reads them; anything else is {@code
   * false}.
   *
   * @return held boolean
   * @see #getAsObject()
   */
  public boolean getAsBoolean() {
    if (data instanceof Boolean bool) {
      return bool;
    }
    return parseBoolean(getAsString());
  }

  /**
   * Returns the held value by this data object as a {@link Byte}, read as a {@code byte} field
   * reads it: a decimal is cut at its decimal point.
   *
   * @return held byte
   * @throws NumberFormatException if the held value is no number or out of range
   * @see #getAsObject()
   */
  public byte getAsByte() {
    return (Byte) number(byte.class);
  }

  /**
   * Returns the held value by this data object as a {@code char}: a {@link Character} as it is, and
   * a one character string, which is how text formats store characters, as its character.
   *
   * @return held char
   * @throws IllegalArgumentException if the held value is neither
   * @see #getAsObject()
   */
  public char getAsChar() {
    if (data instanceof Character character) {
      return character;
    }
    String text = getAsString();
    if (text.length() != 1) {
      throw new IllegalArgumentException("Expected a single character, got: " + text);
    }
    return text.charAt(0);
  }

  /**
   * Returns the held value by this data object as a {@link Double}, read as a {@code double} field
   * reads it.
   *
   * @return held double
   * @throws NumberFormatException if the held value is no number
   * @see #getAsObject()
   */
  public double getAsDouble() {
    return (Double) number(double.class);
  }

  /**
   * Returns the held value by this data object as a {@link Float}, read as a {@code float} field
   * reads it.
   *
   * @return held float
   * @throws NumberFormatException if the held value is no number
   * @see #getAsObject()
   */
  public float getAsFloat() {
    return (Float) number(float.class);
  }

  /**
   * Returns the held value by this data object as a {@link Long}, read as a {@code long} field
   * reads it: a decimal is cut at its decimal point.
   *
   * @return held long
   * @throws NumberFormatException if the held value is no number or out of range
   * @see #getAsObject()
   */
  public long getAsLong() {
    return (Long) number(long.class);
  }

  /**
   * Returns the held value by this data object as a {@link Short}, read as a {@code short} field
   * reads it: a decimal is cut at its decimal point.
   *
   * @return held short
   * @throws NumberFormatException if the held value is no number or out of range
   * @see #getAsObject()
   */
  public short getAsShort() {
    return (Short) number(short.class);
  }

  /**
   * Returns the held value by this data object as a {@link BigDecimal}.
   *
   * @return held big decimal
   * @see #getAsObject()
   */
  public BigDecimal getAsBigDecimal() {
    if (data instanceof BigDecimal decimal) {
      return decimal;
    }
    return new BigDecimal(getAsString());
  }

  /**
   * Returns the held value by this data object as a {@link BigInteger}, read as a {@code
   * BigInteger} field reads it: a decimal is cut at its decimal point.
   *
   * @return held big integer
   * @throws NumberFormatException if the held value is no number
   * @see #getAsObject()
   */
  public BigInteger getAsBigInteger() {
    return (BigInteger) number(BigInteger.class);
  }

  /**
   * Returns the held value by this data object as a {@link List} of {@link Object}. If the held
   * value isn't a list, this method will return null.
   *
   * @return list or null
   */
  public @Nullable List<@Nullable Object> getAsList() {
    return data instanceof List<?> list ? (List<@Nullable Object>) list : null;
  }

  /**
   * Returns the held value by this data object as a {@link List} of the specified class type. If
   * the held value isn't a list, this method will return null. Any values which aren't instances of
   * the specified value class, {@code null} included, are skipped.
   *
   * @param valueClass the value class you want the list entries to be cast to
   * @param <T> type needed
   * @return list or null
   */
  public <T> @Nullable List<T> getList(Class<T> valueClass) {
    List<@Nullable Object> base = getAsList();
    if (base == null) {
      return null;
    }
    List<T> ret = new ArrayList<>(base.size());
    for (Object val : base) {
      if (valueClass.isInstance(val)) {
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
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DataObject other) || other.isSingleValue() != this.isSingleValue()) {
      return false;
    }
    if (this.isSingleValue()) {
      return Objects.equals(other.data, this.data);
    }
    return Objects.equals(other.entries(), this.entries());
  }

  @Override
  public int hashCode() {
    Map<String, DataObject> entries = entries();
    int result = entries != null ? entries.hashCode() : 0;
    result = 31 * result + (data != null ? data.hashCode() : 0);
    return result;
  }

  /**
   * The entries to change, created for an empty data object.
   *
   * @throws UnsupportedOperationException if this data object is immutable
   * @throws IllegalArgumentException if it holds a single value
   */
  private Map<String, DataObject> mutableEntries(String action) {
    if (immutable) {
      throw new UnsupportedOperationException("Cannot " + action + " in an immutable DataObject");
    }
    if (this.data != null) {
      throw new IllegalArgumentException(
          "Cannot " + action + " in a DataObject which is single value");
    }
    Map<String, DataObject> entries = this.serialize;
    if (entries == null) {
      entries = new LinkedHashMap<>();
      this.serialize = entries;
    }
    return entries;
  }
}
