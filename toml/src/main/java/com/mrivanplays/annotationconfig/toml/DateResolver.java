package com.mrivanplays.annotationconfig.toml;

import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import java.text.DateFormat;
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

  private static final DateFormat formatter = getFormatter();

  /** {@inheritDoc} */
  @Override
  public Date deserialize(
      DataObject data, SerializationContext<Date> context, AnnotationAccessor annotations) {
    printWarning();
    String input = data.getAsString();
    if (input.indexOf('T') != -1) {
      String[] dateTimePart = input.split("T");
      String date = dateTimePart[0];
      String time = dateTimePart[1];
      if (!date.contains("-")) {
        throw new IllegalArgumentException("Invalid date!");
      }
      String[] dateParts = date.split("-");
      int year = Integer.parseInt(dateParts[0]);
      int month = Integer.parseInt(dateParts[1]);
      int day = Integer.parseInt(dateParts[2]);
      if (!time.contains(":")) {
        throw new IllegalArgumentException("Invalid time!");
      }
      String[] timeSplit = time.split(":");
      int hour = Integer.parseInt(timeSplit[0]);
      int minute = Integer.parseInt(timeSplit[1]);
      int second = 0;
      if (timeSplit.length >= 3) {
        String secondPart = timeSplit[2];
        if (secondPart.contains("+") || secondPart.contains("-")) {
          // fuck timezones. DO NOT USE DATE FOR GOD'S SAKE
          if (secondPart.indexOf('+') != -1) {
            secondPart = secondPart.substring(0, secondPart.indexOf('+'));
          }
          if (secondPart.indexOf('-') != -1) {
            secondPart = secondPart.substring(0, secondPart.indexOf('-'));
          }
        }
        second = Integer.parseInt(secondPart);
      }
      return new Date(year, month, day, hour, minute, second);
    } else {
      // normal date
      if (!input.contains("-")) {
        throw new IllegalArgumentException("Invalid date!");
      }
      String[] dateParts = input.split("-");
      int year = Integer.parseInt(dateParts[0]);
      int month = Integer.parseInt(dateParts[1]);
      int day = Integer.parseInt(dateParts[2]);
      return new Date(year, month, day);
    }
  }

  /** {@inheritDoc} */
  @Override
  public DataObject serialize(
      Date value, SerializationContext<Date> context, AnnotationAccessor annotations) {
    printWarning();
    return new DataObject(formatter.format(value));
  }

  private void printWarning() {
    System.err.println(
        "[AnnotationConfig] WARNING: Stop using Date for dates. Heck, its 2022, we have java 17, and java 8 in 2014 implemented a new time api. USE THAT");
    System.err.println(
        "[AnnotationConfig] WARNING: Date serializer is kept and maintained for backwards compatibility, migrate to the new java time API.");
  }

  private static DateFormat getFormatter() {
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
