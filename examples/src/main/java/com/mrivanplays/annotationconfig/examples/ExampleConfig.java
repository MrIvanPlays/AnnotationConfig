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

package com.mrivanplays.annotationconfig.examples;

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.yaml.SectionObjectList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/** An example config, written to {@code examples/application-config.yaml}. */
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "unused"})
@Comment("Generated using AnnotationConfig v%version%")
@NullMarked
public class ExampleConfig {

  @Comment("This value can only be between 1 and 3 (1 and 3 included)")
  @Min(minInt = 1)
  @Max(maxInt = 3)
  private int foo = 2;

  @Comment("This string cannot be longer than 20 characters (spaces are included)")
  @Max(maxInt = 20)
  private String bar = "This is some string";

  @ConfigObject private MessagesSection messages = new MessagesSection();
  @Ignore private String importantClass = "com.mrivanplays.something.Important"; // this is ignored

  @Comment("This is also going to be serialized as a config object,")
  @Comment("but it is much more controllable than @ConfigObject")
  private SomethingToSerialize serialize = new SomethingToSerialize("foo", 1, (byte) 0x2);

  @Comment("This cannot have a negative value")
  @Min(minDouble = 0)
  @Key("barxtwo") // you can also apply @Key to regular fields, not just in config objects
  private double baz = 0.2;

  @Comment("AnnotationConfig can also read & write lists")
  @Key("foo-list")
  private List<String> fooList = List.of("This is", "a lore", "as an example", "for list");

  @Comment("Lists can be of all primitive types")
  @Key("bar-list")
  private List<Integer> barList = List.of(1, 2, 3, 4);

  @Comment("And of non primitive types, which also include records")
  private List<Foo> foosList = List.of(new Foo("baz", 1), new Foo("lorem", 3));

  @Comment("Same for maps, but a map can only be Map<String, Object>")
  @Comment("otherwise you will need another object")
  @Key("foo-map")
  private Map<String, Object> fooMap = defaultFooMap();

  @Comment("This doesn't have a serializer registered")
  @Comment("so it gets serialized by the default serializer")
  @Key("default-serializer-example")
  private DefaultSerializationExample defaultSerExample =
      new DefaultSerializationExample("bar", 1, 5.6);

  @Comment("You can create sections like this too")
  @Key("inside.section.foo")
  private String fooInsideSection = "foo";

  @ConfigObject
  @Key("section-inside-section")
  private SectionInsideSection insideSection = new SectionInsideSection();

  @Comment("This is a section object list")
  private SectionObjectList<SectionObject> sectionObjectList =
      SectionObjectList.newBuilderForType(SectionObject.class)
          .defaultValue("asdfp", new SectionObject(1, "lorem"))
          .defaultValue("pepepepepe", new SectionObject(2, "ipsum"))
          .build();

  @Comment("The 'type' key chooses the implementation of this section")
  @Polymorphic({
    @Polymorphic.Subtype(name = "sqlite", type = SqliteStorage.class),
    @Polymorphic.Subtype(name = "mysql", type = MysqlStorage.class)
  })
  private Storage storage = new SqliteStorage();

  @Comment("@Polymorphic on a sealed type applies wherever the type is used, lists included")
  private List<Reward> rewards = List.of(new MoneyReward(100), new ItemReward("diamond", 3));

  private static Map<String, Object> defaultFooMap() {
    // a LinkedHashMap keeps the order of the entries in the file
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("foo", 1);
    map.put("bar", "This is a section value");
    map.put("baz", 3);
    return map;
  }

  /** A config object: its fields are options of their own, in a section. */
  @Comment("All configurable messages")
  public static final class MessagesSection {

    @Comment("The no permission message")
    @Key("no-permission")
    private String noPermission = "You don't have permission to perform this command";

    @Comment("The no spamming message")
    @Key("no-spamming")
    private String noSpamming = "You can't spam this";

    // getters would go here
  }

  /** Records are supported too, since 3.1.0. */
  public record SomethingToSerialize(String foo, int bar, byte baz) {}

  /**
   * This should be registered before calling the dump method for this annotated config using the
   * SerializerRegistry. This is purely optional to implement, records are by default supported,
   * only implement if you have custom logic.
   */
  public static final class SomethingToSerializeSerializer
      implements FieldTypeSerializer<SomethingToSerialize> {

    private static DataObject value(DataObject data, String key) {
      DataObject value = data.get(key);
      if (value == null) {
        throw new IllegalArgumentException("Missing '" + key + "' in " + data);
      }
      return value;
    }

    @Override
    public SomethingToSerialize deserialize(
        DataObject data,
        SerializationContext<SomethingToSerialize> context,
        AnnotationAccessor annotations) {
      return new SomethingToSerialize(
          value(data, "foo").getAsString(),
          value(data, "bar").getAsInt(),
          value(data, "baz").getAsByte());
    }

    @Override
    public DataObject serialize(
        SomethingToSerialize value,
        SerializationContext<SomethingToSerialize> context,
        AnnotationAccessor annotations) {
      DataObject ret = new DataObject();
      ret.put("foo", value.foo());
      ret.put("bar", value.bar());
      ret.put("baz", value.baz());
      return ret;
    }
  }

  record Foo(String bar, int baz) {}

  /**
   * A class without a serializer of its own, read and written by the default serializer. It
   * needs an empty constructor.
   */
  public static final class DefaultSerializationExample {

    private String foo;
    private int bar;
    private double baz;

    public DefaultSerializationExample() {}

    /** Creates the default value of the option. */
    public DefaultSerializationExample(String foo, int bar, double baz) {
      this.foo = foo;
      this.bar = bar;
      this.baz = baz;
    }

    // getters would go here
  }

  /** A config object whose dotted keys make sections inside its section. */
  @Comment("This is a section inside a section example")
  public static class SectionInsideSection {

    @Comment("Comments are supported here too!")
    @Key("pp.baz")
    private int baz = 69420;

    @Comment("And here!")
    @Key("pp.foo")
    private double foo = 3.3;

    // getters would go here
  }

  /** An element of the section object list; see {@link ExampleLoadConfig} for its serializer. */
  public record SectionObject(int aabb, String ccdd) {}

  /** Where the application stores its data; the config chooses the implementation. */
  public interface Storage {}

  /** A storage in a file. */
  public static final class SqliteStorage implements Storage {

    @Comment("The database file")
    private String file = "data.db";

    // getters would go here
  }

  /** A storage on a database server. */
  public static final class MysqlStorage implements Storage {

    private String host = "localhost";
    private int port = 3306;

    // getters would go here
  }

  /**
   * A reward. Its permitted subclasses are its implementations, so {@link Polymorphic} doesn't list
   * them; the 'kind' key names them.
   */
  @Polymorphic(discriminator = "kind")
  public sealed interface Reward permits MoneyReward, ItemReward {}

  /** Named "MoneyReward" in the config, after its simple class name. */
  public record MoneyReward(int amount) implements Reward {}

  /** Named "item" in the config: the name stays the same if the class is renamed or obfuscated. */
  @Polymorphic.Name("item")
  public record ItemReward(String item, int count) implements Reward {}

  // getters would go here
}
