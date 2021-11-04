package com.mrivanplays.annotationconfig.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mrivanplays.annotationconfig.core.serialization.AdvancedEnumSerializer;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import org.junit.jupiter.api.Test;

public class AdvancedEnumSerializerTest {

  enum Values {
    ACCEPT_ALL_TRUE,
    ACCEPT_ALL_FALSE,
    ACCEPT_ALL_BOTH,
    DENY_ALL_FALSE,
    DENY_ALL_TRUE,
    DENY_ALL_BOTH,
    TRUE,
    FALSE,
    THA_COOL
  }

  private static final AdvancedEnumSerializer<Values> serializer =
      AdvancedEnumSerializer.forEnum(Values.class);

  private static Values deserialize(String input) {
    // it is safe to call with null field because the serializer doesn't use it
    return serializer.deserialize(new DataObject(input), null);
  }

  private static String serialize(Values input) {
    return serializer.serialize(input, null).getAsString();
  }

  @Test
  public void testSerialization() {
    assertEquals("accept all true", serialize(Values.ACCEPT_ALL_TRUE));
    assertEquals("true", serialize(Values.TRUE));
    assertEquals("tha cool", serialize(Values.THA_COOL));
  }

  @Test
  public void testDeserializingNormal() {
    assertEquals(Values.ACCEPT_ALL_TRUE, deserialize("accept all true"));
    assertEquals(Values.ACCEPT_ALL_TRUE, deserialize("accept_all_true"));
    assertEquals(Values.ACCEPT_ALL_TRUE, deserialize("ACCEPT_ALL_TRUE"));
  }

  @Test
  public void testDeserializingShuffled() {
    assertEquals(Values.DENY_ALL_TRUE, deserialize("all deny true"));
    assertEquals(Values.DENY_ALL_TRUE, deserialize("all true deny"));
    assertEquals(Values.DENY_ALL_TRUE, deserialize("true deny all"));
    assertEquals(Values.FALSE, deserialize("false"));
    assertEquals(Values.DENY_ALL_TRUE, deserialize("true all deny"));
    assertEquals(Values.DENY_ALL_TRUE, deserialize("deny true all"));
    assertEquals(Values.DENY_ALL_BOTH, deserialize("deny all both"));
    assertEquals(Values.ACCEPT_ALL_FALSE, deserialize("accept_all false"));
    assertEquals(Values.ACCEPT_ALL_FALSE, deserialize("all_accept false"));
    assertEquals(Values.ACCEPT_ALL_FALSE, deserialize("accept_false all"));
    assertEquals(Values.ACCEPT_ALL_BOTH, deserialize("all accept_both"));
    assertEquals(Values.DENY_ALL_FALSE, deserialize("deny_false all"));
  }

  @Test
  public void testDeserializingError() {
    assertNull(deserialize(null));
    assertNull(deserialize("something"));
    assertNull(deserialize("deny aa bb cc"));
  }
}
