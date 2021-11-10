package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.resolver.options.Option;

public class OptsConstant {

  public static final String DUMMY_OPTION = "opts:dummy";
  public static final Option<Boolean> DUMMY_DEFAULT = Option.of(false).markReplaceable();
}
