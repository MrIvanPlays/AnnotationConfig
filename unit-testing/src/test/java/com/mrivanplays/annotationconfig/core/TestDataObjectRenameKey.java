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

package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** {@link DataObject#renameKey(String, String)}, which migrations use to rename options. */
class TestDataObjectRenameKey {

  /** A mutable data object with the keys and values {@code keysAndValues}, in that order. */
  private static DataObject of(Object... keysAndValues) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      values.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return new DataObject(values, false);
  }

  @Test
  void theValueMovesToTheNewKey() {
    DataObject data = of("port", 25565, "motd", "Hello");
    Assertions.assertTrue(data.renameKey("port", "server-port"));
    // the renamed key comes last
    Assertions.assertEquals(
        List.of("motd", "server-port"), List.copyOf(data.getAsMap().keySet()));
    Assertions.assertEquals(25565, data.get("server-port").getAsInt());
    Assertions.assertFalse(data.has("port"));
  }

  @Test
  void aSectionMovesWithItsValues() {
    DataObject data = of("server", Map.of("port", 25565));
    Assertions.assertTrue(data.renameKey("server", "network"));
    Assertions.assertEquals(Map.of("network", Map.of("port", 25565)), data.getAsMap());
  }

  @Test
  void aNullValueIsMoved() {
    DataObject data = of("port", null);
    Assertions.assertTrue(data.renameKey("port", "server-port"));
    Assertions.assertTrue(data.has("server-port"));
    Assertions.assertNull(data.get("server-port").getAsObject());
    Assertions.assertFalse(data.has("port"));
  }

  @Test
  void aMissingKeyChangesNothing() {
    DataObject data = of("motd", "Hello");
    Assertions.assertFalse(data.renameKey("port", "server-port"));
    Assertions.assertEquals(Map.of("motd", "Hello"), data.getAsMap());
  }

  @Test
  void aValueUnderTheNewKeyIsKept() {
    DataObject data = of("port", 1, "server-port", 2);
    Assertions.assertTrue(data.renameKey("port", "server-port"));
    Assertions.assertEquals(Map.of("server-port", 2), data.getAsMap());
  }

  @Test
  void renamingAKeyToItselfChangesNothing() {
    DataObject data = of("port", 1, "motd", "Hello");
    Assertions.assertTrue(data.renameKey("port", "port"));
    Assertions.assertFalse(data.renameKey("missing", "missing"));
    Assertions.assertEquals(List.of("port", "motd"), List.copyOf(data.getAsMap().keySet()));
  }

  @Test
  void aDottedKeyIsOneKeyOfThisDataObject() {
    // as in YAML, JSON and TOML: server.port is the key port of the section server
    DataObject nested = of("server", Map.of("port", 1));
    Assertions.assertFalse(nested.renameKey("server.port", "server.server-port"));
    // as in .properties: server.port is one key
    DataObject flat = of("server.port", 1);
    Assertions.assertTrue(flat.renameKey("server.port", "server.server-port"));
    Assertions.assertEquals(Map.of("server.server-port", 1), flat.getAsMap());
  }

  @Test
  void anEmptyKeyIsRejected() {
    DataObject data = of("port", 1);
    IllegalArgumentException emptyFrom =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> data.renameKey("", "server-port"));
    Assertions.assertEquals("The key 'from' of renameKey is empty", emptyFrom.getMessage());
    IllegalArgumentException emptyTo =
        Assertions.assertThrows(IllegalArgumentException.class, () -> data.renameKey("port", ""));
    Assertions.assertEquals("The key 'to' of renameKey is empty", emptyTo.getMessage());
    // nothing was moved
    Assertions.assertEquals(Map.of("port", 1), data.getAsMap());

    // checked first: an immutable data object fails for the empty key, not for being immutable
    DataObject immutable = new DataObject(Map.of("port", 1), true);
    Assertions.assertThrows(IllegalArgumentException.class, () -> immutable.renameKey("", "a"));
  }

  @Test
  void onlyAMutableMapCanBeChanged() {
    DataObject immutable = new DataObject(Map.of("port", 1), true);
    Assertions.assertThrows(
        UnsupportedOperationException.class, () -> immutable.renameKey("port", "server-port"));
    Assertions.assertThrows(
        IllegalArgumentException.class, () -> new DataObject("value").renameKey("a", "b"));
  }
}
