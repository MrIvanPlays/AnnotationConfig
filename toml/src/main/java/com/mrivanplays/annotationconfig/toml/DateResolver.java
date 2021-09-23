package com.mrivanplays.annotationconfig.toml;

import com.mrivanplays.annotationconfig.core.serialization.ConfigDataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializedObject;
import java.lang.reflect.Field;
import java.util.Date;

/** Field resolver, which is resolving dates. */
public class DateResolver implements FieldTypeSerializer<Date> {

  /** {@inheritDoc} */
  @Override
  public Date deserialize(ConfigDataObject data, Field field) {
    // yes. that's right.
    // that's what toml does, that's what we will do too.
    return (Date) data.getRawData();
  }

  /** {@inheritDoc} */
  @Override
  public SerializedObject serialize(Date value, Field field) {
    return SerializedObject.object(value);
  }
}
