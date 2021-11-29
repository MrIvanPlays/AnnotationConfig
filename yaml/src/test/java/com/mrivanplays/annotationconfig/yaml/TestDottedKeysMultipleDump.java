package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDottedKeysMultipleDump {

  @Comment("Generated using AnnotationConfig v2.1.0-SNAPSHOT")
  private static final class Subject {

    @ConfigObject private BarSection bar = new BarSection();

    @Comment("This is a test comment on top of a section")
    public static class BarSection {

      @Comment("Foo comment")
      @Key("baz.foo")
      private String foo = "bar";

      @Comment("Bar comment")
      @Key("baz.bar")
      private String bar = "foo";
    }

    @Comment("Foo comment")
    @Key("kappa.baz.foo")
    private String foo = "bar";

    @Comment("Bar comment")
    @Key("kappa.baz.bar")
    private String bar1 = "foo";

    @Comment("This is a control field")
    @Key("control-field")
    private int controlField = -1;
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter dumpTo = new StringWriter();
    YamlConfig.getConfigResolver().dump(config, dumpTo);

    String expected =
        "# Generated using AnnotationConfig v2.1.0-SNAPSHOT\n"
            + "\n"
            + "# This is a test comment on top of a section\n"
            + "bar:\n"
            + "  baz:\n"
            + "    # Foo comment\n"
            + "    foo: \"bar\"\n"
            + "    # Bar comment\n"
            + "    bar: \"foo\"\n"
            + "\n"
            + "# This is a control field\n"
            + "control-field: -1\n"
            + "\n"
            + "kappa:\n"
            + "  baz:\n"
            + "    # Foo comment\n"
            + "    foo: \"bar\"\n"
            + "    # Bar comment\n"
            + "    bar: \"foo\"\n"
            + "\n";

    Assertions.assertEquals(expected, dumpTo.toString());
  }
}
