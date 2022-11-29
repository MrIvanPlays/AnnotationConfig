package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestListPropertyDeserialization {

  public static class Subject {

    public List<String> foo;
  }

  @Test
  public void testListProperty() {
    ConfigResolver resolver = YamlConfig.getConfigResolver();
    Subject subject = new Subject();

    StringReader input = new StringReader("foo: \"aabb\"");
    resolver.load(subject, input);

    Assertions.assertNotNull(subject.foo);
    Assertions.assertEquals(1, subject.foo.size());
    Assertions.assertEquals("aabb", subject.foo.get(0));
  }
}
