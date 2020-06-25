package com.mrivanplays.annotationconfig.toml;

import com.mrivanplays.annotationconfig.core.FieldTypeResolver;
import java.lang.reflect.Field;
import java.util.Date;

/** Field resolver, which is resolving dates. */
public class DateResolver implements FieldTypeResolver {

  /** {@inheritDoc} */
  @Override
  public Object toType(Object value, Field field) throws Exception {
    // yes. that's right.
    // that's what toml does, that's what we will do too.
    return (Date) value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean shouldResolve(Class<?> fieldType) {
    return Date.class.isAssignableFrom(fieldType);
  }
}
