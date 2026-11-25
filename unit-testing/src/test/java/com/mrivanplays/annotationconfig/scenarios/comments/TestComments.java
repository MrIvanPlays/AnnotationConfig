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

package com.mrivanplays.annotationconfig.scenarios.comments;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** Where comments are written, in every format with comments and sections. */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.WITH_COMMENTED_SECTIONS)
class TestComments extends Scenario {

  TestComments(Format format) {
    super(format, "comments");
  }

  @Test
  void commentsAreAboveTheirKeys() {
    Commented config = new Commented();
    assertDump("commented", config);
    config.name = "Pekov";
    config.server.port = 1;
    config.server.limits.players = 2;
    config.items = List.of(new Item("b", 3), new Item("c", 4));

    // the comments don't get in the way of reading
    Commented read = roundTrip(config, new Commented());
    Assertions.assertEquals("Pekov", read.name);
    Assertions.assertEquals("localhost", read.server.host);
    Assertions.assertEquals(1, read.server.port);
    Assertions.assertEquals(2, read.server.limits.players);
    Assertions.assertEquals(2, read.items.size());
    Assertions.assertEquals("c", read.items.get(1).name);
    Assertions.assertEquals(4, read.items.get(1).amount);
  }

  @Test
  void aSectionWithoutCommentGetsItsClassComment() {
    assertDump("section-class-comment", new SectionCommentHolder());
  }

  @Test
  void sectionsWithKeysOfTheSameName() {
    assertDump("same-keys-in-sections", new SameKeys());
  }

  @Test
  void aCommentBelongsToExactlyOneKey() {
    // "port" has no comment and is a prefix of "portal", so it used to get portal's comment
    assertDump("similar-keys", new SimilarKeys());
  }

  // <editor-fold desc="subjects">

  @Comment("Class comment")
  static class Commented {

    @Comment("The name")
    public String name = "Ivan";

    @Comment("Server settings")
    @ConfigObject
    public Server server = new Server();

    @Comment("first line\nsecond line")
    public int multi = 1;

    public List<Item> items = List.of(new Item("a", 1));
  }

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

  public static class Item {

    public String name;
    public int amount;

    public Item() {}

    Item(String name, int amount) {
      this.name = name;
      this.amount = amount;
    }
  }

  static class SectionCommentHolder {

    @ConfigObject public CommentedSection section = new CommentedSection();
  }

  @Comment("Section without its own comment")
  public static class CommentedSection {

    public int value = 1;
  }

  @Comment("Class comment")
  static class SameKeys {

    @Comment("sectionOne comment")
    @ConfigObject
    public Section1 sectionOne = new Section1();

    @Comment("sectionTwo comment")
    @ConfigObject
    public Section2 sectionTwo = new Section2();

    @Comment("foo outside config object")
    public String foo = "aa";

    public static class Section1 {

      @Comment("foo inside sectionOne")
      public String foo = "bb";
    }

    public static class Section2 {

      @Comment("foo inside sectionTwo")
      public String foo = "cc";
    }
  }

  static class SimilarKeys {

    @Comment("portal comment")
    public int portal = 1;

    public int port = 2;
  }

  // </editor-fold>
}
