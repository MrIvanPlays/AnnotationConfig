package com.mrivanplays.annotationconfig.toml;

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import java.lang.reflect.Field;
import java.util.Date;

/** Field resolver, which is resolving dates. */
public class DateResolver implements FieldTypeSerializer<Date> {

  /** {@inheritDoc} */
  @Override
  public Date deserialize(DataObject data, Field field) {
    // yes. that's right.
    // that's what toml does, that's what we will do too.
    return (Date) data.getAsObject();
  }

  /** {@inheritDoc} */
  @Override
  public DataObject serialize(Date value, Field field) {
    return new DataObject(value);
  }
}
