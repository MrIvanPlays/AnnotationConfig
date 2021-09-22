package com.mrivanplays.annotationconfig.core.serialization;

// todo: javadoc
public final class ConfigDataObject {

  private final Object data;

  public ConfigDataObject(Object data) {
    this.data = data;
  }

  public boolean hasData() {
    return data != null;
  }

  public Object getRawData() {
    return data;
  }

  public String getAsString() {
    return String.valueOf(data);
  }

  public boolean getAsBoolean() {
    return Boolean.parseBoolean(getAsString());
  }

  public int getAsInt() {
    return Integer.parseInt(getAsString());
  }

  public byte getAsByte() {
    return Byte.parseByte(getAsString());
  }

  public double getAsDouble() {
    return Double.parseDouble(getAsString());
  }

  public float getAsFloat() {
    return Float.parseFloat(getAsString());
  }
}
