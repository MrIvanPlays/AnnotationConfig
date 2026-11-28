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
import com.mrivanplays.annotationconfig.core.serialization.helpers.UriToStringSerializer;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link UriToStringSerializer}, registered by this class for {@link URI}, which no other test
 * class uses: as a field in every format, and in the formats with sections as the elements of
 * lists and a record component, which the default serializer hands to the registered serializer.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestUris extends Scenario {

  private static final URI SITE = URI.create("https://example.com/path?query=1#top");
  private static final URI RELATIVE = URI.create("docs/index.html");
  private static final URI MAIL = URI.create("mailto:admin@example.com");

  TestUris(Format format) {
    super(format, "values");
  }

  @BeforeAll
  static void registerSerializer() {
    SerializerRegistry.INSTANCE.registerSerializer(URI.class, new UriToStringSerializer());
  }

  @AfterAll
  static void unregisterSerializer() {
    SerializerRegistry.INSTANCE.unregisterSerializer(URI.class);
  }

  // <editor-fold desc="URIs">

  @Test
  void urisAreWrittenAsTextAndReadBack() {
    final String written = assertDump("uris", new Uris());
    Uris read = new Uris();
    read.uri = null;
    loadText(read, written);
    Assertions.assertEquals(SITE, read.uri);
  }

  @Test
  void urisInListsAndRecordsAreWrittenAsTextAndReadBack() {
    // .properties has neither lists nor records
    Assumptions.assumeTrue(this.format.has(Format.Feature.SECTIONS));
    final String written = assertDump("uri-elements", new Elements());
    Elements read = new Elements();
    read.list = null;
    read.link = null;
    loadText(read, written);
    Assertions.assertEquals(List.of(SITE, RELATIVE), read.list);
    Assertions.assertEquals(new Link("contact", MAIL), read.link);
  }

  @Test
  void anEmptyUriIsNoValue() {
    Assertions.assertNull(load(new Uris(), "empty-uri").uri);
  }

  @Test
  void anInvalidUriIsRejected() {
    Throwable error =
        Assertions.assertThrows(RuntimeException.class, () -> load(new Uris(), "invalid-uri"));
    while (error.getCause() != null && !(error instanceof IllegalArgumentException)) {
      error = error.getCause();
    }
    Assertions.assertTrue(
        error.getMessage().contains("'http://exa mple.com'"), error.getMessage());
  }

  // </editor-fold>

  // <editor-fold desc="URIs: subjects">

  static class Uris {

    public URI uri = SITE;
  }

  record Link(String name, URI uri) {}

  static class Elements {

    public List<URI> list = List.of(SITE, RELATIVE);
    public Link link = new Link("contact", MAIL);
  }

  // </editor-fold>
}
