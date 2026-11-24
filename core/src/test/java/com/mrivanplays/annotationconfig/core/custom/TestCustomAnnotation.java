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

package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.PropertyConfig;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestCustomAnnotation {

  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @BeforeAll
  public static void registerValidators() {
    CustomAnnotationRegistry.INSTANCE.register(DummyAnnotation.class, new DummyAnnotationHandler());
    CustomAnnotationRegistry.INSTANCE.register(
        DummyAnnotation2.class, new DummyAnnotation2Handler());
  }

  // the registry is global, other test classes must not see these validators
  @AfterAll
  public static void unregisterValidators() {
    CustomAnnotationRegistry.INSTANCE.unregister(DummyAnnotation.class);
    CustomAnnotationRegistry.INSTANCE.unregister(DummyAnnotation2.class);
  }

  private InputStream resource(String name) {
    return getClass().getClassLoader().getResourceAsStream("custom/" + name);
  }

  @Test
  void testSuccessful() {
    final int callbacksBefore = DummyAnnotation2Handler.SUCCESS_CALLBACKS.get();
    DummyConfig config = new DummyConfig();
    resolver.load(config, resource("custom-anno-def.properties"));

    Assertions.assertEquals("Hello mama", config.getFoo());
    Assertions.assertEquals("MrIvanPlays is cool", config.getBar());
    Assertions.assertEquals(callbacksBefore + 1, DummyAnnotation2Handler.SUCCESS_CALLBACKS.get());
  }

  @Test
  void testFail() {
    // foo does not contain "Hello", so DummyAnnotationHandler rejects it with its error
    RuntimeException e =
        Assertions.assertThrows(
            RuntimeException.class,
            () -> resolver.load(new DummyConfig(), resource("custom-anno-fail.properties")));
    Assertions.assertEquals("Dummy throwable", e.getCause().getMessage());
  }

  @Test
  void theFieldOfADataTypeIsValidated() {
    resolver.load(new WithGreeting(), new StringReader("greeting=Hello there"));
    // only the fields of a config and its config objects were validated before
    RuntimeException e =
        Assertions.assertThrows(
            RuntimeException.class,
            () -> resolver.load(new WithGreeting(), new StringReader("greeting=Bye")));
    Assertions.assertEquals("Dummy throwable", e.getCause().getMessage());
  }

  @Test
  void testAlterOption() {
    Settings settings = ACDefaultSettings.getDefault().copy();
    settings.put(OptsConstant.DUMMY_DEFAULT, false);
    resolver.load(new DummyConfig(), resource("custom-anno-def.properties"), settings);
    // the validator of @DummyAnnotation flips the option
    Assertions.assertTrue(settings.get(OptsConstant.DUMMY_DEFAULT).orElseThrow());
  }

  @Test
  void testDump() {
    DummyConfig config = new DummyConfig();
    StringWriter writer = new StringWriter();
    resolver.dump(config, writer);
    // @DummyAnnotation2's writeValue replaces bar's value when writing
    resolver.load(config, new StringReader(writer.toString()));
    Assertions.assertEquals("Lorem ipsum dolor sit amet.", config.getBar());
  }

  static class WithGreeting {

    Greeting greeting = new Greeting();
  }

  static class Greeting {

    @DummyAnnotation String text = "Hello";
  }
}
