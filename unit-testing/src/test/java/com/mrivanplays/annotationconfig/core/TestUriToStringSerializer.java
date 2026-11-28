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
import com.mrivanplays.annotationconfig.core.serialization.helpers.UriToStringSerializer;
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The scheme predicate of {@link UriToStringSerializer}, called directly: the scenarios register
 * one serializer for {@link URI} in the global registry, without a predicate.
 */
class TestUriToStringSerializer {

  private static final UriToStringSerializer WEB =
      new UriToStringSerializer(Set.of("http", "https")::contains);

  private static String rejection(UriToStringSerializer serializer, String text) {
    return Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> serializer.deserializeSimple(new DataObject(text)))
        .getMessage();
  }

  @Test
  void surroundingSpacesAreIgnored() {
    Assertions.assertEquals(
        URI.create("https://example.com"),
        new UriToStringSerializer().deserializeSimple(new DataObject("  https://example.com ")));
  }

  @Test
  void withoutAPredicateRelativeUrisAreRead() {
    Assertions.assertEquals(
        URI.create("docs/index.html"),
        new UriToStringSerializer().deserializeSimple(new DataObject("docs/index.html")));
  }

  @Test
  void anAllowedSchemeIsReadIgnoringCase() {
    Assertions.assertEquals(
        URI.create("HTTPS://example.com"),
        WEB.deserializeSimple(new DataObject("HTTPS://example.com")));
  }

  @Test
  void aSchemeThePredicateRejectsFails() {
    String message = rejection(WEB, "ftp://example.com");
    Assertions.assertTrue(message.contains("'ftp://example.com'"), message);
    Assertions.assertTrue(message.contains("'ftp' is not allowed"), message);
  }

  @Test
  void withAPredicateAUriWithoutASchemeFails() {
    // Set#contains throws on null, so the predicate must not get one
    String message = rejection(WEB, "example.com/path");
    Assertions.assertTrue(message.contains("expected a scheme"), message);
  }

  @Test
  void textThatIsNotAUriFails() {
    String message = rejection(new UriToStringSerializer(), "http://exa mple.com");
    Assertions.assertTrue(message.startsWith("Invalid URI 'http://exa mple.com': "), message);
  }

  @Test
  void anEmptyValueIsNoValue() {
    Assertions.assertNull(WEB.deserializeSimple(new DataObject("  ")));
  }
}
