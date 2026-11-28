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

package com.mrivanplays.annotationconfig.scenarios.values;

import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.serialization.helpers.UuidToStringSerializer;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link UuidToStringSerializer}, registered by this class for {@link UUID}, which no other test
 * class uses: as a field in every format, and in the formats with sections as the elements of
 * arrays and lists and a record component, which the default serializer hands to the registered
 * serializer.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestUuids extends Scenario {

  private static final UUID FIRST = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID THIRD = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");

  TestUuids(Format format) {
    super(format, "values");
  }

  @BeforeAll
  static void registerSerializer() {
    SerializerRegistry.INSTANCE.registerSerializer(UUID.class, new UuidToStringSerializer());
  }

  @AfterAll
  static void unregisterSerializer() {
    SerializerRegistry.INSTANCE.unregisterSerializer(UUID.class);
  }

  // <editor-fold desc="UUIDs">

  @Test
  void uuidsAreWrittenAsTextAndReadBack() {
    final String written = assertDump("uuids", new Ids());
    Ids read = new Ids();
    read.id = null;
    loadText(read, written);
    Assertions.assertEquals(FIRST, read.id);
  }

  @Test
  void uuidsInArraysListsAndRecordsAreWrittenAsTextAndReadBack() {
    // .properties has neither lists nor records, and writes an array of a type with a serializer
    // as a list
    Assumptions.assumeTrue(this.format.has(Format.Feature.SECTIONS));
    final String written = assertDump("uuid-elements", new Elements());
    Elements read = new Elements();
    read.ids = null;
    read.list = null;
    read.owner = null;
    loadText(read, written);
    Assertions.assertArrayEquals(new UUID[] {SECOND, THIRD}, read.ids);
    Assertions.assertEquals(List.of(FIRST, THIRD), read.list);
    Assertions.assertEquals(new Owner("admin", SECOND), read.owner);
  }

  @Test
  void anEmptyUuidIsNoValue() {
    Assertions.assertNull(load(new Ids(), "empty-uuid").id);
  }

  @Test
  void anInvalidUuidIsRejected() {
    Throwable error =
        Assertions.assertThrows(RuntimeException.class, () -> load(new Ids(), "invalid-uuid"));
    while (error.getCause() != null && !(error instanceof IllegalArgumentException)) {
      error = error.getCause();
    }
    Assertions.assertTrue(error.getMessage().contains("'not-a-uuid'"), error.getMessage());
  }

  // </editor-fold>

  // <editor-fold desc="UUIDs: subjects">

  static class Ids {

    public UUID id = FIRST;
  }

  record Owner(String name, UUID id) {}

  static class Elements {

    public UUID[] ids = {SECOND, THIRD};
    public List<UUID> list = List.of(FIRST, THIRD);
    public Owner owner = new Owner("admin", SECOND);
  }

  // </editor-fold>
}
