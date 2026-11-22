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
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializationContext;
import com.mrivanplays.annotationconfig.yaml.SectionObjectList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "unused"})
@Comment("Generated using AnnotationConfig v%version%")
public class ExampleConfig {

  @Comment("This value can only be between 1 and 3 ( 1 and 3 included )")
  @Min(minInt = 1)
  @Max(maxInt = 3)
  private int foo = 2;

  @Comment("This string cannot be longer than 20 characters ( spaces are included )")
  @Max(maxInt = 20)
  private String bar = "This is some string";

  @ConfigObject private MessagesSection messages = new MessagesSection();

  @Comment("All configurable messages")
  public static final class MessagesSection {

    @Comment("The no permission message")
    @Key("no-permission")
    private String noPermission = "You don't have permission to perform this command";

    @Comment("The no spamming message")
    @Key("no-spamming")
    private String noSpamming = "You can't spam this";

    // getters
  }

  @Ignore private String importantClass = "com.mrivanplays.something.Important"; // this is ignored

  @Comment("This is also going to be serialized as a config object,")
  @Comment("but it is much more controllable rather than @ConfigObject")
  private SomethingToSerialize serialize = new SomethingToSerialize("foo", 1, (byte) 0x2);

  // since 3.0.4 records are supported too!
  public record SomethingToSerialize(String foo, int bar, byte baz) {}

  /**
   * This should be registered before calling the dump method for this annotated config using the
   * SerializerRegistry. This is purely optional to implement, records are by default supported,
   * only implement if you have custom logic.
   */
  public static final class SomethingToSerializeSerializer
      implements FieldTypeSerializer<SomethingToSerialize> {

    @Override
    public @NonNull SomethingToSerialize deserialize(
        @NonNull DataObject data,
        @NonNull SerializationContext<SomethingToSerialize> context,
        @NonNull AnnotationAccessor annotations) {
      return new SomethingToSerialize(
          data.get("foo").getAsString(), data.get("bar").getAsShort(), data.get("baz").getAsByte());
    }

    @Override
    @NonNull
    public DataObject serialize(
        @NonNull SomethingToSerialize value,
        @NonNull SerializationContext<SomethingToSerialize> context,
        @NonNull AnnotationAccessor annotations) {
      DataObject ret = new DataObject();
      ret.put("foo", value.foo());
      ret.put("bar", value.bar());
      ret.put("baz", value.baz());
      return ret;
    }
  }

  @Comment("This cannot have a negative value")
  @Min(minDouble = 0)
  @Key("barxtwo") // you can also apply @Key to regular fields, not just in config objects
  private double baz = 0.2;

  @Comment("AnnotatedConfig can also read & write lists")
  @Key("foo-list")
  private List<String> fooList = Arrays.asList("This is", "a lore", "as an example", "for list");

  @Comment("Lists can be of all primitive types")
  @Key("bar-list")
  private List<Integer> barList = Arrays.asList(1, 2, 3, 4);

  @Comment("And of non primitive types, which also include records")
  private List<Foo> foosList = Arrays.asList(new Foo("baz", 1), new Foo("lorem", 3));

  record Foo(String bar, int baz) {}

  @Comment("Same for maps, but a map can only be Map<String, Object>")
  @Comment("otherwise you will need another object")
  @Key("foo-map")
  private Map<String, Object> fooMap =
      new LinkedHashMap<>() {
        {
          put("foo", 1);
          put("bar", "This is a section value");
          put("baz", 3);
        }
      };

  @Comment("This doesn't have a serializer registered")
  @Comment("so it gets serialized by the default serializer")
  @Key("default-serializer-example")
  private DefaultSerializationExample defaultSerExample =
      new DefaultSerializationExample("bar", 1, 5.6);

  public static final class DefaultSerializationExample {

    private String foo;
    private int bar;
    private double baz;

    public DefaultSerializationExample() {}

    public DefaultSerializationExample(String foo, int bar, double baz) {
      this.foo = foo;
      this.bar = bar;
      this.baz = baz;
    }

    // getters
  }

  @Comment("You can create sections like this too")
  @Key("inside.section.foo")
  private String fooInsideSection = "foo";

  @ConfigObject
  @Key("section-inside-section")
  private SectionInsideSection insideSection = new SectionInsideSection();

  @Comment("This is a section inside a section example")
  public static class SectionInsideSection {

    @Comment("Comments are supported here too!")
    @Key("pp.baz")
    private int baz = 69420;

    @Comment("And here!")
    @Key("pp.foo")
    private double foo = 3.3;

    // getters
  }

  // this is featured in the next example
  public record SectionObject(int aabb, String ccdd) {}

  @Comment("This is a section object list")
  private SectionObjectList<SectionObject> sectionObjectList =
      SectionObjectList.newBuilderForType(SectionObject.class)
          .defaultValue("asdfp", new SectionObject(1, "lorem"))
          .defaultValue("pepepepepe", new SectionObject(2, "ipsum"))
          .build();

  // getters
}
