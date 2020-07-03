package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.FieldTypeResolver;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Field resolver, resolving yaml lists to type of string lists. */
public class StringListResolver implements FieldTypeResolver {

  /** {@inheritDoc} */
  @Override
  public Object toType(Object value, Field field) throws Exception {
    if (!(value instanceof List<?>)) {
      throw new IllegalArgumentException("Value given is not a list.");
    }
    List<?> list = (List<?>) value;
    List<String> transformed = new ArrayList<>();
    for (Object i : list) {
      if (i instanceof String) {
        transformed.add(String.valueOf(i));
      }
    }
    return transformed;
  }

  /** {@inheritDoc} */
  @Override
  public boolean shouldResolve(Class<?> fieldType) {
    return List.class.isAssignableFrom(fieldType);
  }
}
