package com.mrivanplays.annotationconfig.core.custom;

public class DummyConfig {
  @DummyAnnotation private String foo = "Hello mama";

  @DummyAnnotation2 private String bar = "asd";

  public String getFoo() {
    return foo;
  }

  public void setFoo(String newFoo) {
    this.foo = newFoo;
  }

  public String getBar() {
    return bar;
  }
}
