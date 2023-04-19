package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSectionWithKeyResolver {

  static class Subject {

    @Comment("Foo comment")
    @Key("commands.subcommands.help.foo")
    private String foo = "bar";

    @Comment("Bar comment")
    @Key("commands.subcommands.this.bar")
    private String bar = "aaa";

    @Comment("baz")
    @Key("commands.subcommands.this.baz")
    private int baz = -1;
  }

  @Test
  public void testDump() {
    Subject config = new Subject();
    StringWriter writer = new StringWriter();

    YamlConfig.getConfigResolver().dump(config, writer);

    String expected =
        "commands:\n"
            + "  subcommands:\n"
            + "    help:\n"
            + "      # Foo comment\n"
            + "      foo: \"bar\"\n"
            + "    this:\n"
            + "      # Bar comment\n"
            + "      bar: \"aaa\"\n"
            + "      # baz\n"
            + "      baz: -1\n"
            + "\n";

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  public void testLoad() {
    Subject config = new Subject();
    YamlConfig.getConfigResolver()
        .load(
            config,
            getClass().getClassLoader().getResourceAsStream("section-with-key-resolver.yml"));

    Assertions.assertEquals("pepega", config.foo);
    Assertions.assertEquals("baz", config.bar);
    Assertions.assertEquals(69420, config.baz);
  }
}
