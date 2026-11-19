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
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestCustomAnnotation {

  private final ConfigResolver resolver = PropertyConfig.getConfigResolver();

  @BeforeAll
  public static void registerValidator() {
    CustomAnnotationRegistry.INSTANCE.register(DummyAnnotation.class, new DummyAnnotationHandler());
    CustomAnnotationRegistry.INSTANCE.register(
        DummyAnnotation2.class, new DummyAnnotation2Handler());
  }

  @Test
  public void testSuccessful() {
    DummyConfig config = new DummyConfig();
    resolver.load(
        config,
        getClass().getClassLoader().getResourceAsStream("custom/custom-anno-def.properties"));
  }

  @Test
  public void testFail() {
    DummyConfig config = new DummyConfig();
    Assertions.assertThrows(
        RuntimeException.class,
        () ->
            resolver.load(
                config,
                getClass()
                    .getClassLoader()
                    .getResourceAsStream("custom/custom-anno-fail.properties")));
  }

  @Test
  public void testAlterOption() {
    Settings settings = ACDefaultSettings.getDefault().copy();
    settings.put(OptsConstant.DUMMY_DEFAULT, false);
    DummyConfig config = new DummyConfig();
    resolver.load(
        config,
        getClass().getClassLoader().getResourceAsStream("custom/custom-anno-def.properties"),
        settings);
    Assertions.assertTrue(settings.get(OptsConstant.DUMMY_DEFAULT).get());
  }

  @Test
  public void testDump() {
    DummyConfig config = new DummyConfig();
    StringWriter writer = new StringWriter();
    resolver.dump(config, writer);

    String written = writer.toString();
    // now we have to load because we need to verify that bar=Lorem ipsum dolor sit amet.
    resolver.load(config, new StringReader(written));

    Assertions.assertEquals("Lorem ipsum dolor sit amet.", config.getBar());
  }
}
