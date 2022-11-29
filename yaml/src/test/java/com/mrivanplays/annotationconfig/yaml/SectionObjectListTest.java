package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.utils.TypeToken;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SectionObjectListTest {

  public static class SerializedObject {

    private int foo;
    private String baz;

    public SerializedObject(int foo, String baz) {
      this.foo = foo;
      this.baz = baz;
    }

    public int getFoo() {
      return foo;
    }

    public String getBaz() {
      return baz;
    }
  }

  public static class Subject {

    public SectionObjectList<SerializedObject> list =
        SectionObjectList.newBuilderForType(SerializedObject.class)
            .defaultValue("foo", new SerializedObject(1, "lorem"))
            .defaultValue("bar", new SerializedObject(2, "ipsum"))
            .build();
  }

  private static final ConfigResolver resolver = YamlConfig.getConfigResolver();
  private static final SerializerRegistry serializerRegistry = SerializerRegistry.INSTANCE;

  @Test
  void testDump() {
    Subject subject = new Subject();
    Type sectionObjectListType = new TypeToken<SectionObjectList<SerializedObject>>() {}.getType();
    if (!serializerRegistry.hasSerializer(sectionObjectListType)) {
      serializerRegistry.registerSerializer(
          sectionObjectListType, new SectionObjectListSerializer<SerializedObject>());
    }
    StringWriter writer = new StringWriter();

    String expected =
        "list:\n"
            + "  foo:\n"
            + "    foo: 1\n"
            + "    baz: \"lorem\"\n"
            + "  bar:\n"
            + "    foo: 2\n"
            + "    baz: \"ipsum\"\n"
            + "\n";

    resolver.dump(subject, writer);

    Assertions.assertEquals(expected, writer.toString());
  }

  @Test
  void testLoad() {
    Subject subject = new Subject();
    Type sectionObjectListType = new TypeToken<SectionObjectList<SerializedObject>>() {}.getType();
    if (!serializerRegistry.hasSerializer(sectionObjectListType)) {
      serializerRegistry.registerSerializer(
          sectionObjectListType, new SectionObjectListSerializer<SerializedObject>());
    }
    String input =
        "list:\n"
            + "  bar:\n"
            + "    foo: 3\n"
            + "    baz: \"dolor\"\n"
            + "  foo:\n"
            + "    foo: 69\n"
            + "    baz: \"sit\"\n"
            + "\n";

    resolver.load(subject, new StringReader(input));

    Map<String, SerializedObject> serialized = subject.list.getAsMap();

    Assertions.assertEquals(3, serialized.get("bar").getFoo());
    Assertions.assertEquals("dolor", serialized.get("bar").getBaz());
    Assertions.assertEquals(69, serialized.get("foo").getFoo());
    Assertions.assertEquals("sit", serialized.get("foo").getBaz());
  }
}
