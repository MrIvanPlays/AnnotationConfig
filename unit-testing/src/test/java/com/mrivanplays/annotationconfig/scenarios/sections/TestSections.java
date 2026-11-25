/*
 * Copyright (c) Ivan Pekov 2026
 * Copyright (c) Contributors 2026
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.mrivanplays.annotationconfig.scenarios.sections;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Sections in every format that has them: config objects, sections in sections, dotted keys, which
 * are nested in every format, sections of parent classes, and the fields the compiler adds.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_SECTIONS)
class TestSections extends Scenario {

  TestSections(Format format) {
    super(format, "sections");
  }

  private static Settings findParents() {
    return new Settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true);
  }

  private static int occurrences(String text, String part) {
    return text.split(Pattern.quote(part), -1).length - 1;
  }

  // <editor-fold desc="Config objects">

  @Test
  void configObjectsAreNestedSections() {
    final String written = assertDump("config-objects", new Sections());
    Sections read = new Sections();
    read.name = null;
    read.second.host = null;
    read.second.limits.players = 0;
    loadText(read, written);
    Assertions.assertEquals("Ivan", read.name);
    Assertions.assertEquals("localhost", read.second.host);
    Assertions.assertEquals(20, read.second.limits.players);
  }

  @Test
  void configObjectsLoad() {
    Sections config = load(new Sections(), "config-objects-read");
    Assertions.assertEquals("Pekov", config.name);
    Assertions.assertEquals("example.org", config.server.host);
    Assertions.assertEquals(25577, config.server.port);
    Assertions.assertEquals(50, config.server.limits.players);
  }

  @Test
  void sectionsInSectionsAreNested() {
    assertDump("child-sections", new ChildSections());
    ChildSections config = load(new ChildSections(), "child-sections-read");
    Assertions.assertEquals("foo", config.foo.barSect.bar);
    Assertions.assertEquals("fff", config.foo.control);
  }

  @Test
  void missingOptionsOfASectionAreAdded(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("config." + this.format.extension());
    this.resolver.dump(new OlderSection(), file);
    NewerSection config = new NewerSection();
    // set, as TOML keeps it off by default
    this.resolver.load(config, file, generateNewOptions());
    // the value in the file wins over the changed default, the missing option gets its default
    Assertions.assertEquals("foo", config.test.foo);
    Assertions.assertEquals("bar", config.test.bar);
    assertOutput("missing-section-options", Files.readString(file));
  }

  @Test
  void aNullSectionIsNamedInTheError() {
    IllegalArgumentException e =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> this.resolver.dump(new NullSection(), new StringWriter()));
    Assertions.assertTrue(e.getMessage().contains("'section'"), e.getMessage());
  }

  // </editor-fold>

  // <editor-fold desc="Dotted keys">

  @Test
  void dottedKeysAreNestedSections() {
    assertDump("dotted-keys", new DottedKeys());
    DottedKeys config = load(new DottedKeys(), "dotted-keys-read");
    Assertions.assertEquals("pepega", config.foo);
    Assertions.assertEquals("baz", config.bar);
    Assertions.assertEquals(69420, config.baz);
  }

  @Test
  void dottedKeysSharingTheirParentsShareTheirSection() {
    // only "y" was written before
    final String written = assertDump("deep-dotted-keys", new DeepDottedKeys());
    DeepDottedKeys read = new DeepDottedKeys();
    read.x = 0;
    read.y = 0;
    loadText(read, written);
    Assertions.assertEquals(1, read.x);
    Assertions.assertEquals(2, read.y);
    DeepDottedKeys config = load(new DeepDottedKeys(), "deep-dotted-keys-read");
    Assertions.assertEquals(10, config.x);
    Assertions.assertEquals(20, config.y);
  }

  @Test
  void dottedKeysInAndBesideConfigObjects() {
    final String written = assertDump("dotted-keys-and-sections", new DottedKeysAndSections());
    DottedKeysAndSections read = new DottedKeysAndSections();
    read.bar.foo = null;
    read.foo = null;
    read.controlField = 0;
    loadText(read, written);
    Assertions.assertEquals("bar", read.bar.foo);
    Assertions.assertEquals("bar", read.foo);
    Assertions.assertEquals(-1, read.controlField);
  }

  @Test
  void aDottedKeyJoinsTheConfigObjectWithItsFirstPart() {
    final String written = assertDump("dotted-key-joins-section", new JoiningDottedKeys());
    JoiningDottedKeys read = new JoiningDottedKeys();
    read.ip = null;
    read.port = 0;
    read.max = 0;
    read.server.players = 0;
    loadText(read, written);
    Assertions.assertEquals("localhost", read.ip);
    Assertions.assertEquals(25565, read.port);
    Assertions.assertEquals(100, read.max);
    Assertions.assertEquals(20, read.server.players);
  }

  // </editor-fold>

  // <editor-fold desc="Parent classes">

  @Test
  void aSectionKeepsTheCommentsOfItsParentClass() {
    final String written = dump(new WithSection(), findParents());
    assertOutput("parent-class-section", written);
    if (this.format.has(Format.Feature.COMMENTS)) {
      // the parent's comment used to be replaced by a second copy of the child's
      Assertions.assertEquals(1, occurrences(written, "child comment"), written);
      Assertions.assertEquals(1, occurrences(written, "parent comment"), written);
    }
    WithSection read = new WithSection();
    read.section.parentValue = 0;
    loadText(read, written, findParents());
    Assertions.assertEquals(1, read.section.parentValue);
  }

  @Test
  void localClassesWithParentFields() {
    class LocalBase {

      public int a = 1;
    }

    class Local extends LocalBase {

      public int b = 2;
    }

    // local classes have no canonical name, which used to throw a NullPointerException
    String written = dump(new Local(), findParents());
    Local read = new Local();
    read.a = 0;
    read.b = 0;
    loadText(read, written, findParents());
    Assertions.assertEquals(1, read.a);
    Assertions.assertEquals(2, read.b);
  }

  // </editor-fold>

  // <editor-fold desc="Synthetic fields">

  @Test
  void theOuterInstanceOfAnInnerSectionIsNoOption() {
    assertDump("inner-section", new InnerSectionConfig());
    InnerSectionConfig config = new InnerSectionConfig();
    InnerSectionConfig.Inner inner = config.inner;
    load(config, "inner-section-read");
    Assertions.assertSame(inner, config.inner);
    Assertions.assertEquals("read too", config.inner.b);
    Assertions.assertEquals("read", config.inner.outer());
  }

  @Test
  void theAssertionFlagIsNoOptionWithStaticFields() {
    assertOutput(
        "assertion-flag",
        dump(new AssertConfig(), new Settings().put(ACDefaultSettings.FIND_STATIC_FIELDS, true)));
  }

  // </editor-fold>

  // <editor-fold desc="Config objects: subjects">

  @Comment("Class comment")
  static class Sections {

    @Comment("Section comment")
    @ConfigObject
    public Server server = new Server();

    @ConfigObject public Server second = new Server();

    @Comment("Plain field comment")
    public String name = "Ivan";
  }

  @Comment("Server class comment")
  public static class Server {

    @Comment("ip comment")
    @Key("ip")
    public String host = "localhost";

    public int port = 25565;

    @ConfigObject public Limits limits = new Limits();
  }

  public static class Limits {

    @Comment("players comment")
    public int players = 20;
  }

  @Comment("Subject comment")
  static class ChildSections {

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

  @Comment("Comments should also be kept!")
  static class OlderSection {

    @Comment("This is test subject comment")
    @ConfigObject
    private Test1 test = new Test1();

    static class Test1 {

      private String foo = "foo";
    }
  }

  // a newer version of OlderSection: the default of foo changed and the option bar was added
  @Comment("Comments should also be kept!")
  static class NewerSection {

    @Comment("This is test subject comment")
    @ConfigObject
    private Test2 test = new Test2();

    static class Test2 {

      private String foo = "baz";
      private String bar = "bar";
    }
  }

  public static class NullSection {

    @ConfigObject public Limits section = null;
  }

  // </editor-fold>

  // <editor-fold desc="Dotted keys: subjects">

  static class DottedKeys {

    @Comment("Foo comment")
    @Key("commands.subcommands.help.foo")
    public String foo = "bar";

    @Comment("Bar comment")
    @Key("commands.subcommands.this.bar")
    public String bar = "aaa";

    @Comment("baz")
    @Key("commands.subcommands.this.baz")
    public int baz = -1;
  }

  static class DeepDottedKeys {

    @Key("a.b.c.d.x")
    int x = 1;

    @Key("a.b.c.d.y")
    int y = 2;
  }

  @Comment("Generated using AnnotationConfig")
  static class DottedKeysAndSections {

    @ConfigObject private BarSection bar = new BarSection();

    @Comment("Foo comment")
    @Key("kappa.baz.foo")
    private String foo = "bar";

    @Comment("Bar comment")
    @Key("kappa.baz.bar")
    private String bar1 = "foo";

    @Comment("This is a control field")
    @Key("control-field")
    private int controlField = -1;

    @Comment("This is a test comment on top of a section")
    static class BarSection {

      @Comment("Foo comment")
      @Key("baz.foo")
      private String foo = "bar";

      @Comment("Bar comment")
      @Key("baz.bar")
      private String bar = "foo";
    }
  }

  static class JoiningDottedKeys {

    public String name = "Ivan";

    @Comment("ip comment")
    @Key("server.ip")
    public String ip = "localhost";

    @ConfigObject public Limits server = new Limits();

    @Key("other.port")
    public int port = 25565;

    @Key("limits.players.max")
    public int max = 100;
  }

  // </editor-fold>

  // <editor-fold desc="Parent classes: subjects">

  @Comment("parent comment")
  public static class SectionParent {

    public int parentValue = 1;
  }

  @Comment("child comment")
  public static class ChildOfParent extends SectionParent {

    public int childValue = 2;
  }

  public static class WithSection {

    @ConfigObject public ChildOfParent section = new ChildOfParent();
  }

  // </editor-fold>

  // <editor-fold desc="Synthetic fields: subjects">

  static class InnerSectionConfig {

    String a = "y";

    @ConfigObject Inner inner = new Inner();

    // an inner class that uses its outer instance has the synthetic field this$0
    class Inner {

      String b = "x";

      String outer() {
        return a;
      }
    }
  }

  static class AssertConfig {

    String c = "z";

    // an assert statement adds the synthetic field $assertionsDisabled
    void check() {
      assert c != null;
    }
  }

  // </editor-fold>
}
