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

import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.core.resolver.CommentPlaceholder;
import com.mrivanplays.annotationconfig.core.resolver.CommentPlaceholderRegistry;
import java.io.StringWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CommentPlaceholderRegistryTest {

  private static final CommentPlaceholder PLACEHOLDER = CommentPlaceholder.simple("%foo%", "bar");
  private static final CommentPlaceholderRegistry REGISTRY = CommentPlaceholderRegistry.INSTANCE;

  static class Subject {

    @Comment("I get that %foo%")
    private String foo = "bar";
  }

  // the registry is global, so no test may leave the placeholder registered
  @AfterEach
  void unregister() {
    REGISTRY.unregisterPlaceholder(PLACEHOLDER);
  }

  private static String dump() {
    StringWriter writer = new StringWriter();
    PropertyConfig.getConfigResolver().dump(new Subject(), writer);
    return writer.toString().replaceAll("\\R", "\n");
  }

  @Test
  void testPlaceholder() {
    REGISTRY.registerPlaceholder(PLACEHOLDER);
    Assertions.assertEquals("# I get that bar\nfoo=bar\n", dump());
  }

  @Test
  void testUnregisterPlaceholder() {
    REGISTRY.registerPlaceholder(PLACEHOLDER);
    // an equal placeholder, not the registered instance, has to unregister it
    REGISTRY.unregisterPlaceholder(CommentPlaceholder.simple("%foo%", "bar"));
    Assertions.assertEquals("# I get that %foo%\nfoo=bar\n", dump());
  }
}
