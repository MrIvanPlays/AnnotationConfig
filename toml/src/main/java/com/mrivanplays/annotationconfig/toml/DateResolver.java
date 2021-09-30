package com.mrivanplays.annotationconfig.toml;

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Type serializer, which is serializing and deserializing the java 8+ obsolete class {@link Date}.
 * <br>
 * This is kept and maintained for backwards compatibility, everyone is encouraged to migrate to the
 * new java time api.
 *
 * @since 1.0
 */
public class DateResolver implements FieldTypeSerializer<Date> {

  /** {@inheritDoc} */
  @Override
  public Date deserialize(DataObject data, Field field) {
    DateFormat format = getFormatter();
    try {
      return format.parse(data.getAsString());
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public DataObject serialize(Date value, Field field) {
    DateFormat format = getFormatter();
    return new DataObject(format.format(value));
  }

  private DateFormat getFormatter() {
    TimeZone timeZone = TimeZone.getDefault();
    String format;
    if (timeZone.getID().contains("UTC")) {
      format = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    } else {
      format = "yyyy-MM-dd'T'HH:mm:ssXXX";
    }
    SimpleDateFormat formatter = new SimpleDateFormat(format);
    formatter.setTimeZone(timeZone);
    return formatter;
  }
}
