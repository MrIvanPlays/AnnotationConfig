package com.mrivanplays.annotationconfig.core.serialization;

/**
 * Represents a data object, which we have received from reading the configuration. It gives you
 * utility methods to more easily deserialize your data.
 *
 * @since 2.0.0
 * @author MrIvanPlays
 */
public final class ConfigDataObject {

  private final Object data;

  public ConfigDataObject(Object data) {
    this.data = data;
  }

  /**
   * Returns the raw data we received from reading the configuration
   *
   * @return raw data
   */
  public Object getRawData() {
    return data;
  }

  /**
   * Returns the data we received as a {@link String}
   *
   * @return data, represented as string
   */
  public String getAsString() {
    return String.valueOf(data);
  }

  /**
   * Returns the data we received as a {@link Boolean}
   *
   * @return data, represented as boolean
   */
  public boolean getAsBoolean() {
    return Boolean.parseBoolean(getAsString());
  }

  /**
   * Returns the data we received as a {@link Integer}
   *
   * @return data, represented as integer
   */
  public int getAsInt() {
    return Integer.parseInt(getAsString());
  }

  /**
   * Returns the data we received as a {@link Byte}
   *
   * @return data, represented as byte
   */
  public byte getAsByte() {
    return Byte.parseByte(getAsString());
  }

  /**
   * Returns the data we received as a {@link Double}
   *
   * @return data, represented as double
   */
  public double getAsDouble() {
    return Double.parseDouble(getAsString());
  }

  /**
   * Returns the data we received as a {@link Float}
   *
   * @return data, represented as float
   */
  public float getAsFloat() {
    return Float.parseFloat(getAsString());
  }
}
