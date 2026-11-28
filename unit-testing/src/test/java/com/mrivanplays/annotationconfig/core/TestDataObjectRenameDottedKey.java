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

import com.mrivanplays.annotationconfig.core.resolver.key.DottedResolver;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link DataObject#renameDottedKey(String, String, KeyResolver)} with the key resolvers of the
 * formats: {@code KeyResolver.DEFAULT} ({@code .properties}), {@link DottedResolver} (YAML, JSON)
 * and one that reads a dotted key as one key first, as TOML's does. The values are shaped as each
 * format's parser reads them; the migration scenarios run it through the formats themselves.
 */
class TestDataObjectRenameDottedKey {

  private static final KeyResolver DOTTED = DottedResolver.getInstance();

  // as TomlConfig's: 'server.port' = 1, how 3.0.x wrote it, is read first, then the tables
  private static final KeyResolver WHOLE_KEY_FIRST =
      new KeyResolver() {
        @Override
        public @Nullable Object unbox(String key, Map<String, Object> values) {
          Object value = values.get(key);
          return value != null ? value : DOTTED.unbox(key, values);
        }

        @Override
        public void boxTo(String key, Object value, Map<String, Object> values) {
          DOTTED.boxTo(key, value, values);
        }
      };

  /** A mutable data object with the keys and values {@code keysAndValues}, in that order. */
  private static DataObject of(Object... keysAndValues) {
    return new DataObject(map(keysAndValues), false);
  }

  /** A mutable map with the keys and values {@code keysAndValues}, in that order. */
  private static Map<String, Object> map(Object... keysAndValues) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      values.put((String) keysAndValues[i], keysAndValues[i + 1]);
    }
    return values;
  }

  // <editor-fold desc="Sections">

  @Test
  void anOptionMovesBetweenSections() {
    DataObject data = of("server", map("host", "example.org", "port", 25565));
    Assertions.assertTrue(data.renameDottedKey("server.port", "network.port", DOTTED));
    Assertions.assertEquals(
        map("server", map("host", "example.org"), "network", map("port", 25565)), data.getAsMap());
  }

  @Test
  void aSectionLeftEmptyIsRemoved() {
    DataObject data = of("a", map("b", map("c", 1)), "d", 2);
    Assertions.assertTrue(data.renameDottedKey("a.b.c", "e", DOTTED));
    Assertions.assertEquals(map("d", 2, "e", 1), data.getAsMap());
  }

  @Test
  void aTopLevelOptionMovesIntoASection() {
    DataObject nested = of("port", 25565);
    Assertions.assertTrue(nested.renameDottedKey("port", "network.port", DOTTED));
    Assertions.assertEquals(map("network", map("port", 25565)), nested.getAsMap());

    // .properties reads network.port as one key, so it is written as one
    DataObject flat = of("port", 25565);
    Assertions.assertTrue(flat.renameDottedKey("port", "network.port", KeyResolver.DEFAULT));
    Assertions.assertEquals(map("network.port", 25565), flat.getAsMap());
  }

  @Test
  void anOptionInASectionMovesToTheTopLevel() {
    DataObject data = of("server", map("port", 25565));
    Assertions.assertTrue(data.renameDottedKey("server.port", "port", DOTTED));
    Assertions.assertEquals(map("port", 25565), data.getAsMap());
  }

  @Test
  void aWholeSectionMoves() {
    DataObject data = of("server", map("network", map("port", 1, "host", "a")));
    Assertions.assertTrue(data.renameDottedKey("server.network", "network", DOTTED));
    Assertions.assertEquals(map("network", map("port", 1, "host", "a")), data.getAsMap());
  }

  // </editor-fold>

  // <editor-fold desc="Keys read as one key">

  @Test
  void aDottedKeyReadAsOneKeyIsMovedAsOne() {
    DataObject data = of("server.host", "example.org", "server.port", 25565);
    Assertions.assertTrue(data.renameDottedKey("server.port", "network.port", KeyResolver.DEFAULT));
    Assertions.assertEquals(
        map("server.host", "example.org", "network.port", 25565), data.getAsMap());
  }

  @Test
  void aSectionIsNotReadByAResolverOfWholeKeys() {
    DataObject data = of("server", map("port", 25565));
    Assertions.assertFalse(
        data.renameDottedKey("server.port", "network.port", KeyResolver.DEFAULT));
    Assertions.assertEquals(map("server", map("port", 25565)), data.getAsMap());
  }

  @Test
  void aResolverThatReadsWholeKeysFirstMovesEither() {
    // a TOML file of 3.0.x: one quoted key, written back as a table
    DataObject quoted = of("server.port", 25565);
    Assertions.assertTrue(quoted.renameDottedKey("server.port", "network.port", WHOLE_KEY_FIRST));
    Assertions.assertEquals(map("network", map("port", 25565)), quoted.getAsMap());

    DataObject table = of("server", map("port", 25565));
    Assertions.assertTrue(table.renameDottedKey("server.port", "network.port", WHOLE_KEY_FIRST));
    Assertions.assertEquals(map("network", map("port", 25565)), table.getAsMap());
  }

  @Test
  void theValueTheResolverReadIsTheOneMoved() {
    // both hold 1, which is one cached Integer: the resolver reads the table, so it moves
    DataObject data = of("server.port", 1, "server", map("port", 1));
    Assertions.assertTrue(data.renameDottedKey("server.port", "network.port", DOTTED));
    Assertions.assertEquals(map("server.port", 1, "network", map("port", 1)), data.getAsMap());
  }

  // </editor-fold>

  // <editor-fold desc="Existing and missing values">

  @Test
  void aValueUnderTheNewKeyIsKept() {
    DataObject data = of("server", map("port", 1), "network", map("port", 2, "host", "a"));
    Assertions.assertTrue(data.renameDottedKey("server.port", "network.port", DOTTED));
    Assertions.assertEquals(map("network", map("port", 2, "host", "a")), data.getAsMap());
  }

  @Test
  void anExistingSectionGetsTheValue() {
    DataObject data = of("server", map("port", 1), "network", map("host", "a"));
    Assertions.assertTrue(data.renameDottedKey("server.port", "network.port", DOTTED));
    Assertions.assertEquals(map("network", map("host", "a", "port", 1)), data.getAsMap());
  }

  @Test
  void aMissingKeyChangesNothing() {
    DataObject data = of("server", map("host", "a"));
    Assertions.assertFalse(data.renameDottedKey("server.port", "network.port", DOTTED));
    Assertions.assertFalse(data.renameDottedKey("client.port", "network.port", DOTTED));
    Assertions.assertEquals(map("server", map("host", "a")), data.getAsMap());
  }

  @Test
  void aKeyWhoseSectionIsAValueIsMissing() {
    DataObject data = of("server", 5);
    Assertions.assertFalse(data.renameDottedKey("server.port", "network.port", DOTTED));
    Assertions.assertEquals(map("server", 5), data.getAsMap());
  }

  @Test
  void aNewKeyWhoseSectionIsAValueFailsWithoutChanges() {
    DataObject data = of("server", map("port", 1), "network", "lan");
    DataObject server = data.get("server");
    IllegalArgumentException error =
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> data.renameDottedKey("server.port", "network.port", DOTTED));
    Assertions.assertTrue(error.getMessage().contains("'network'"), error.getMessage());
    Assertions.assertEquals(map("server", map("port", 1), "network", "lan"), data.getAsMap());
    // nothing was rebuilt either
    Assertions.assertSame(server, data.get("server"));
  }

  @Test
  void renamingAKeyToItselfChangesNothing() {
    DataObject data = of("server", map("port", 1));
    Assertions.assertTrue(data.renameDottedKey("server.port", "server.port", DOTTED));
    Assertions.assertFalse(data.renameDottedKey("server.host", "server.host", DOTTED));
    Assertions.assertEquals(map("server", map("port", 1)), data.getAsMap());
  }

  // </editor-fold>

  // <editor-fold desc="Invalid calls">

  @Test
  void anEmptyKeyIsRejected() {
    DataObject data = of("port", 1);
    IllegalArgumentException emptyFrom =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> data.renameDottedKey("", "a", DOTTED));
    Assertions.assertEquals("The key 'from' of renameDottedKey is empty", emptyFrom.getMessage());
    IllegalArgumentException emptyTo =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> data.renameDottedKey("port", "", DOTTED));
    Assertions.assertEquals("The key 'to' of renameDottedKey is empty", emptyTo.getMessage());
    Assertions.assertEquals(map("port", 1), data.getAsMap());
  }

  @Test
  void onlyAMutableMapCanBeChanged() {
    DataObject immutable = new DataObject(map("port", 1), true);
    Assertions.assertThrows(
        UnsupportedOperationException.class,
        () -> immutable.renameDottedKey("port", "a.port", DOTTED));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new DataObject("value").renameDottedKey("a", "b", DOTTED));
  }

  // </editor-fold>
}
