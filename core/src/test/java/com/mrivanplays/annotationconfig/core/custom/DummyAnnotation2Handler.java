package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.ValidationResponse;
import com.mrivanplays.annotationconfig.core.resolver.options.CustomOptions;
import java.lang.reflect.Field;

public class DummyAnnotation2Handler implements AnnotationValidator<DummyAnnotation2> {

  @Override
  public ValidationResponse validate(
      DummyAnnotation2 annotation, Object value, CustomOptions options, Field field) {
    return ValidationResponse.success(
        () -> System.out.println("If you are seeing this, then you're a little sneaky boi."));
  }

  @Override
  public Object writeValue(Object value) {
    return "Lorem ipsum dolor sit amet.";
  }
}
