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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * A type registered with only a serialize method is read by the default serializer, also as a
 * record component, which has no annotated config.
 */
class TestSimpleValueSerializerInRecord {

  @AfterEach
  void unregister() {
    if (SerializerRegistry.INSTANCE.hasSerializer(Wrapped.class)) {
      SerializerRegistry.INSTANCE.unregisterSerializer(Wrapped.class);
    }
  }

  @Test
  void recordComponentIsReadByTheDefaultSerializer() {
    SerializerRegistry.INSTANCE.registerSimpleValueSerializer(
        Wrapped.class, wrapped -> new DataObject(wrapped.text));
    Config config = new Config();

    PropertyConfig.getConfigResolver().load(config, Map.of("holder", Map.of("wrapped", "read")));

    assertEquals("read", config.holder.wrapped().text);
  }

  public static final class Wrapped {

    String text = "";

    public Wrapped() {}

    Wrapped(String text) {
      this.text = text;
    }
  }

  record Holder(Wrapped wrapped) {}

  static class Config {

    Holder holder = new Holder(new Wrapped("default"));
  }
}
