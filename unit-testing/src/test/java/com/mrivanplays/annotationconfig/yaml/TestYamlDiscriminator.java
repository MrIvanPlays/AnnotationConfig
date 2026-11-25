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

package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import java.io.StringReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * What only YAML reads differently for {@code @Polymorphic}; the rest is in the {@code polymorphic}
 * scenarios.
 */
class TestYamlDiscriminator {

  @Test
  void anUnquotedBooleanIsMatchedByItsText() {
    // YAML 1.1 reads an unquoted on as the boolean true
    String message =
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () ->
                    YamlConfig.getConfigResolver()
                        .load(new Subject(), new StringReader("value: {type: on}")))
            .getMessage();
    Assertions.assertTrue(message.contains("'true'"), message);
    Assertions.assertTrue(message.contains("quote it"), message);
  }

  interface Value {}

  static class Subject {

    @Polymorphic({@Polymorphic.Subtype(name = "on", type = On.class)})
    Value value = new On();
  }

  static class On implements Value {}
}
