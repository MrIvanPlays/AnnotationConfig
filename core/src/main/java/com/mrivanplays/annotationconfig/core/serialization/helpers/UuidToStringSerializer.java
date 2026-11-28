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
import java.util.UUID;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@link UUID} serializer that writes UUIDs as their text, such as {@code
 * 123e4567-e89b-12d3-a456-426614174000}, and reads them from it.
 *
 * <p>It is not registered by default. Register it for the {@link UUID} fields of your configs
 * before loading or dumping them:
 *
 * <pre>{@code
 * SerializerRegistry.INSTANCE.registerSerializer(UUID.class, new UuidToStringSerializer());
 * }</pre>
 *
 * <p>Reading ignores case and surrounding spaces. An empty value is no value, and text that is not
 * a UUID fails loading.
 *
 * @see SerializerRegistry#registerSerializer(Class, FieldTypeSerializer)
 * @author Ivan Pekov
 * @since 3.1.1
 */
@NullMarked
public final class UuidToStringSerializer implements ContextFreeFieldTypeSerializer<UUID> {

  /** Creates a UUID serializer. */
  public UuidToStringSerializer() {}

  private static IllegalArgumentException invalid(String text, @Nullable Throwable cause) {
    return new IllegalArgumentException(
        "Invalid UUID '"
            + text
            + "': expected 32 hexadecimal digits in groups of 8-4-4-4-12, such as"
            + " 123e4567-e89b-12d3-a456-426614174000",
        cause);
  }

  @Override
  public @Nullable UUID deserializeSimple(DataObject data) {
    if (data.isEmpty() || !data.isSingleValue()) {
      throw new IllegalArgumentException("Illegal UUID to deserialize " + data);
    }
    String text = data.getAsString().trim();
    if (text.isEmpty()) {
      return null;
    }
    // UUID#fromString also takes shortened groups, such as 1-2-3-4-5
    if (text.length() != 36) {
      throw invalid(text, null);
    }
    try {
      return UUID.fromString(text);
    } catch (IllegalArgumentException e) {
      throw invalid(text, e);
    }
  }

  @Override
  public DataObject serializeSimple(UUID value) {
    return new DataObject(value.toString());
  }
}
