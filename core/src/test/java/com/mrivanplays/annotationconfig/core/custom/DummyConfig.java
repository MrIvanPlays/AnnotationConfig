package com.mrivanplays.annotationconfig.core.custom;

public class DummyConfig {
  @DummyAnnotation private String foo = "Hello mama";

  public String getFoo() {
    return foo;
  }

  public void setFoo(String newFoo) {
    this.foo = newFoo;
  }
}
