package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.ValidationResponse;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.lang.reflect.Field;

public class DummyAnnotationHandler implements AnnotationValidator<DummyAnnotation> {

  @Override
  public ValidationResponse validate(
      DummyAnnotation annotation, Object value, Settings settings, Field field) {
    if (annotation.value()) {
      if (settings.has(OptsConstant.DUMMY_DEFAULT)) {
        boolean optVal = settings.get(OptsConstant.DUMMY_DEFAULT).get();
        settings.put(OptsConstant.DUMMY_DEFAULT, !optVal);
      }
    }
    IllegalArgumentException error = new IllegalArgumentException("Dummy throwable");
    if (value instanceof String) {
      return ValidationResponse.fromBooleanValue(((String) value).contains("Hello"), error);
    }
    return ValidationResponse.fail(error);
  }
}
