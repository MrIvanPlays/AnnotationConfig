package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ComplexTestComments {

  @Comment("Class comment")
  static class Subject {

    @Comment("foo outside config object")
    public String foo = "aa";

    @Comment("sectionOne comment")
    @ConfigObject
    public Section1 sectionOne = new Section1();

    public static class Section1 {

      @Comment("foo inside sectionOne")
      public String foo = "bb";
    }

    @Comment("sectionTwo comment")
    @ConfigObject
    public Section2 sectionTwo = new Section2();

    public static class Section2 {

      @Comment("foo inside sectionTwo")
      public String foo = "cc";
    }
  }

  @Test
  public void testComplexComments() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();

    YamlConfig.getConfigResolver().dump(config, writer);

    String expected =
        "# Class comment\n"
            + "\n"
            + "# sectionOne comment\n"
            + "sectionOne:\n"
            + "  # foo inside sectionOne\n"
            + "  foo: \"bb\"\n"
            + "\n"
            + "# sectionTwo comment\n"
            + "sectionTwo:\n"
            + "  # foo inside sectionTwo\n"
            + "  foo: \"cc\"\n"
            + "\n"
            + "# foo outside config object\n"
            + "foo: \"aa\"\n\n";

    Assertions.assertEquals(expected, writer.toString());
  }
}
