package com.mrivanplays.annotationconfig.core.serialization;

import java.util.List;
import java.util.Map;

/** Represents a serialized object, which is config dump friendly. */
public final class SerializedObject {

  public static SerializedObject map(Map<String, Object> serializedMap) {
    return new SerializedObject(serializedMap);
  }

  public static SerializedObject list(List<Object> serializedList) {
    return new SerializedObject(serializedList);
  }

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
