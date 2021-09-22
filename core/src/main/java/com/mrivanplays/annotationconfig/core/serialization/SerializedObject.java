package com.mrivanplays.annotationconfig.core.serialization;

import java.util.List;
import java.util.Map;

/**
 * Represents a serialized object, which is config dump friendly.
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
public final class SerializedObject {

  /**
   * Creates a {@link SerializedObject} of type map.
   *
   * @param serializedMap the map value that we need to serialize
   * @return a serialized object
   */
  public static SerializedObject map(Map<String, Object> serializedMap) {
    return new SerializedObject(serializedMap);
  }

  /**
   * Creates a {@link SerializedObject} of type list.
   *
   * @param serializedList the list value that we need to serialize
   * @return a serialized object
   */
  public static SerializedObject list(List<Object> serializedList) {
    return new SerializedObject(serializedList);
  }

  /**
   * Creates a {@link SerializedObject} of any primitive type. Please do not use this with maps or
   * lists, use the appropriate methods above, or else you might break (de)serialization.
   *
   * @param serializedObject the primitive value that we need to serialize
   * @return a serialized object
   */
  public static SerializedObject object(Object serializedObject) {
    return new SerializedObject(serializedObject);
  }

  private final PresentValue presentValue;

  private Map<String, Object> serializedMap;
  private List<Object> serializedList;
  private Object serializedObject;

  private SerializedObject(Map<String, Object> serializedMap) {
    this.serializedMap = serializedMap;
    this.presentValue = PresentValue.MAP;
  }

  private SerializedObject(List<Object> serializedList) {
    this.serializedList = serializedList;
    this.presentValue = PresentValue.LIST;
  }

  private SerializedObject(Object serializedObject) {
    this.serializedObject = serializedObject;
    this.presentValue = PresentValue.OBJECT;
  }

  /**
   * Returns the present value in this {@link SerializedObject}
   *
   * @return present value
   */
  public PresentValue getPresentValue() {
    return presentValue;
  }

  public Map<String, Object> getSerializedMap() {
    return serializedMap;
  }

  public List<Object> getSerializedList() {
    return serializedList;
  }

  public Object getSerializedObject() {
    return serializedObject;
  }

  /**
   * Represents an enum, containing the value types a {@link SerializedObject} can contain by their
   * names.
   */
  public enum PresentValue {
    MAP,
    LIST,
    OBJECT
  }
}
