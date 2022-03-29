package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// when a section has a child a section
public class TestSectionChildSections {

  @Comment("Subject comment")
  static class Subject {

    @ConfigObject private Section foo = new Section();

    @Comment("This is top section comment")
    static class Section {

      @Comment("Control field comment")
      private String control = "asd";

      @ConfigObject private ChildSection barSect = new ChildSection();

      @Comment("Child section comment")
      static class ChildSection {

        @Comment("Bar comment")
        private String bar = "aabb";
      }
    }
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();

    YamlConfig.getConfigResolver().dump(config, writer);
    String expected =
        "# Subject comment\n"
            + "\n"
            + "# This is top section comment\n"
            + "foo:\n"
            + "  # Control field comment\n"
            + "  control: \"asd\"\n"
            + "  # Child section comment\n"
            + "  barSect:\n"
            + "    # Bar comment\n"
            + "    bar: \"aabb\"\n"
            + "\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  public void testLoad() {
    Subject config = new Subject();
    YamlConfig.getConfigResolver()
        .load(
            config, getClass().getClassLoader().getResourceAsStream("section-child-sections.yml"));

    Assertions.assertEquals("foo", config.foo.barSect.bar);
    Assertions.assertEquals("fff", config.foo.control);
  }
}
