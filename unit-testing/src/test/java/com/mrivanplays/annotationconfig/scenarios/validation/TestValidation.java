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

package com.mrivanplays.annotationconfig.scenarios.validation;

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code @Min}, {@code @Max} and the validators of custom annotations check the values a format
 * reads, in every format: each parser hands them on as a type of its own. The comparison rules are
 * covered by {@code core.internal.TestMinMaxValidator}, and checks of values that don't come from a
 * file by {@code core.TestMinMax}.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
// the formats run in parallel, and DummyAnnotation2Handler counts its calls
@ResourceLock("TestValidation")
class TestValidation extends Scenario {

  TestValidation(Format format) {
    super(format, "validation");
  }

  @BeforeAll
  static void registerValidators() {
    CustomAnnotationRegistry.INSTANCE.register(DummyAnnotation.class, new DummyAnnotationHandler());
    CustomAnnotationRegistry.INSTANCE.register(
        DummyAnnotation2.class, new DummyAnnotation2Handler());
  }

  // the registry is global, other test classes must not see these validators
  @AfterAll
  static void unregisterValidators() {
    CustomAnnotationRegistry.INSTANCE.unregister(DummyAnnotation.class);
    CustomAnnotationRegistry.INSTANCE.unregister(DummyAnnotation2.class);
  }

  // <editor-fold desc="@Min and @Max">

  @Test
  void numbersInRangeAreLoaded() {
    Assertions.assertEquals(-33.9, load(new NumberSubject(), "double-at-min").doubleValue);
    Assertions.assertEquals(44.4, load(new NumberSubject(), "double-at-max").doubleValue);
  }

  @Test
  void numbersOutOfRangeAreRejected() {
    rejected(new NumberSubject(), "double-below-min");
    rejected(new NumberSubject(), "double-above-max");
  }

  @Test
  void stringsAreCheckedByLength() {
    Assertions.assertEquals("abc", load(new StringSubject(), "string-in-range").value);
    rejected(new StringSubject(), "string-too-short");
    rejected(new StringSubject(), "string-too-long");
  }

  @Test
  void anAnnotationOnATypeWithoutBoundsIsRejected() {
    rejected(new MisplacedSubject(), "misplaced");
  }

  @Test
  void theFieldOfAOneFieldDataTypeIsChecked() {
    Assertions.assertEquals(5, load(new WithLimit(), "limit-in-range").limit.value);
    rejected(new WithLimit(), "limit-above-max");
  }

  @Test
  void theElementsOfAnArrayAreChecked() {
    Weights read = load(new Weights(), "weights-in-range");
    Assertions.assertArrayEquals(new int[] {1, 10}, read.weights);
    // an @Max on an array field failed with "annotation placed on invalid field type" before
    String message = rejected(new Weights(), "weights-above-max");
    Assertions.assertTrue(message.contains("weights[1]"), message);
  }

  // </editor-fold>

  // <editor-fold desc="Custom annotations">

  @Test
  void customValidatorsAcceptValidValues() {
    final int callbacksBefore = DummyAnnotation2Handler.SUCCESS_CALLBACKS.get();
    DummyConfig config = load(new DummyConfig(), "custom-valid");
    Assertions.assertEquals("Hello mama", config.getFoo());
    Assertions.assertEquals("MrIvanPlays is cool", config.getBar());
    Assertions.assertEquals(callbacksBefore + 1, DummyAnnotation2Handler.SUCCESS_CALLBACKS.get());
  }

  @Test
  void customValidatorsRejectInvalidValues() {
    // foo does not contain "Hello", so DummyAnnotationHandler rejects it with its error
    RuntimeException e =
        Assertions.assertThrows(
            RuntimeException.class, () -> load(new DummyConfig(), "custom-invalid"));
    Assertions.assertEquals("Dummy throwable", e.getCause().getMessage());
  }

  @Test
  void theFieldOfADataTypeIsValidated() {
    load(new WithGreeting(), "greeting-valid");
    // only the fields of a config and its config objects were validated before
    RuntimeException e =
        Assertions.assertThrows(
            RuntimeException.class, () -> load(new WithGreeting(), "greeting-invalid"));
    Assertions.assertEquals("Dummy throwable", e.getCause().getMessage());
  }

  @Test
  void aValidatorCanChangeTheSettings() {
    Settings settings = ACDefaultSettings.getDefault().copy();
    settings.put(OptsConstant.DUMMY_DEFAULT, false);
    load(new DummyConfig(), "custom-valid", settings);
    // the validator of @DummyAnnotation flips the option
    Assertions.assertTrue(settings.get(OptsConstant.DUMMY_DEFAULT).orElseThrow());
  }

  @Test
  void aValidatorCanReplaceTheWrittenValue() {
    // @DummyAnnotation2's writeValue replaces bar's value when writing
    DummyConfig read = roundTrip(new DummyConfig(), new DummyConfig());
    Assertions.assertEquals("Lorem ipsum dolor sit amet.", read.getBar());
  }

  // </editor-fold>

  // <editor-fold desc="@Min and @Max: subjects">

  static class NumberSubject {

    @Min(minDouble = -33.9)
    @Max(maxDouble = 44.4)
    private double doubleValue = 33.5;
  }

  static class StringSubject {

    @Min(minInt = 3)
    @Max(maxInt = 7)
    private String value = "Hello";
  }

  static class MisplacedSubject {

    @Min(minInt = 1)
    private boolean value = false;
  }

  static class WithLimit {

    Limit limit = new Limit();
  }

  static class Limit {

    @Max(maxInt = 10)
    int value = 1;
  }

  static class Weights {

    @Max(maxInt = 10)
    int[] weights = {1};
  }

  // </editor-fold>

  // <editor-fold desc="Custom annotations: subjects">

  static class WithGreeting {

    Greeting greeting = new Greeting();
  }

  static class Greeting {

    @DummyAnnotation String text = "Hello";
  }

  // </editor-fold>
}
