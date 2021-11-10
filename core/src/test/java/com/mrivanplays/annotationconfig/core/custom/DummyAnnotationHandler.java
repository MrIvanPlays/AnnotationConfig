package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import com.mrivanplays.annotationconfig.core.resolver.options.Option;
import java.lang.reflect.Field;

public class DummyAnnotationHandler implements AnnotationValidator<DummyAnnotation> {

  @Override
  public boolean validate(
      DummyAnnotation annotation, Object value, CustomOptions options, Field field) {
    if (annotation.value()) {
      if (options.has(OptsConstant.DUMMY_OPTION)) {
        Boolean optVal = options.getAs(OptsConstant.DUMMY_OPTION, Boolean.class).get();
        options.put(OptsConstant.DUMMY_OPTION, Option.of(!optVal).markReplaceable());
      }
    }
    if (value instanceof String) {
      return ((String) value).contains("Hello");
    }
    return false;
  }

  @Override
  public Throwable error() {
    return new IllegalArgumentException("Dummy throwable");
  }
}
