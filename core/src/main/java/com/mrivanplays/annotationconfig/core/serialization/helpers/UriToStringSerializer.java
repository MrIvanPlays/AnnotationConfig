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

package com.mrivanplays.annotationconfig.core.serialization.helpers;

import com.mrivanplays.annotationconfig.core.serialization.ContextFreeFieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.FieldTypeSerializer;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link URI} serializer that writes URIs as their text, such as {@code
 * https://example.com/path}, and reads them from it.
 *
 * <p>It is not registered by default. Register it for the {@link URI} fields of your configs before
 * loading or dumping them:
 *
 * <pre>{@code
 * SerializerRegistry.INSTANCE.registerSerializer(URI.class, new UriToStringSerializer());
 * }</pre>
 *
 * <p>To accept only some schemes, pass a predicate that tests the scheme:
 *
 * <pre>{@code
 * SerializerRegistry.INSTANCE.registerSerializer(
 *     URI.class, new UriToStringSerializer(Set.of("http", "https")::contains));
 * }</pre>
 *
 * <p>Reading ignores surrounding spaces. An empty value is no value, and text that is not a URI, as
 * {@link URI#URI(String)} reads it, fails loading. With a scheme predicate, a URI without a scheme
 * (a relative URI, such as {@code example.com/path}) or with a scheme the predicate rejects fails
 * loading too. Values are not checked when writing.
 *
 * @see SerializerRegistry#registerSerializer(Class, FieldTypeSerializer)
 * @author Ivan Pekov
 * @since 3.1.1
 */
@NullMarked
public final class UriToStringSerializer implements ContextFreeFieldTypeSerializer<URI> {

  private final @Nullable Predicate<String> schemeValidator;

  /** Creates a URI serializer that reads every URI, also relative ones. */
  public UriToStringSerializer() {
    this.schemeValidator = null;
  }

  /**
   * Creates a URI serializer that reads only URIs with a scheme that {@code schemeValidator}
   * accepts, such as {@code Set.of("http", "https")::contains}.
   *
   * @param schemeValidator tests the scheme of each URI read, in lower case ({@code https} for
   *     {@code HTTPS://example.com}), as schemes ignore case
   */
  public UriToStringSerializer(Predicate<String> schemeValidator) {
    this.schemeValidator = Objects.requireNonNull(schemeValidator, "schemeValidator");
  }

  @Override
  public @Nullable URI deserializeSimple(DataObject data) {
    if (data.isEmpty() || !data.isSingleValue()) {
      throw new IllegalArgumentException("Illegal URI to deserialize " + data);
    }
    String text = data.getAsString().trim();
    if (text.isEmpty()) {
      return null;
    }
    URI uri;
    try {
      uri = new URI(text);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URI '" + text + "': " + e.getReason(), e);
    }
    if (this.schemeValidator != null) {
      String scheme = uri.getScheme();
      if (scheme == null) {
        throw new IllegalArgumentException(
            "Invalid URI '" + text + "': expected a scheme, such as https://");
      }
      scheme = scheme.toLowerCase(Locale.ROOT);
      if (!this.schemeValidator.test(scheme)) {
        throw new IllegalArgumentException(
            "Invalid URI '" + text + "': the scheme '" + scheme + "' is not allowed");
      }
    }
    return uri;
  }

  @Override
  public DataObject serializeSimple(URI value) {
    return new DataObject(value.toString());
  }
}
