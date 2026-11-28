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

import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.OneOf;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@code @OneOf} checks the values a format reads, in every format: each parser hands them on as a
 * type of its own. Its use on the element types of lists, maps and records is in {@code
 * bounds.TestOneOfElements}: {@code .properties} doesn't read a {@code List<String>}.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestOneOf extends Scenario {

  TestOneOf(Format format) {
    super(format, "validation");
  }

  private void assertRejected(Object config, String input, String... parts) {
    String message = rejected(config, input);
    for (String part : parts) {
      Assertions.assertTrue(message.contains(part), message);
    }
  }

  // <editor-fold desc="Values">

  @Test
  void aListedValueIsLoaded() {
    Assertions.assertEquals("creative", load(new Mode(), "oneof-listed").mode);
  }

  @Test
  void anUnlistedValueIsRejectedWithTheAllowedValues() {
    assertRejected(new Mode(), "oneof-unlisted", "mode", "[survival, creative]", "'hardcore'");
  }

  @Test
  void valuesAreComparedWithCase() {
    assertRejected(new Mode(), "oneof-wrong-case", "'Creative'");
  }

  @Test
  void ignoreCaseKeepsTheValueAsWritten() {
    Assertions.assertEquals("HARD", load(new Difficulty(), "oneof-ignore-case").difficulty);
  }

  @Test
  void numbersAndBooleansAreComparedAsText() {
    // YAML reads "yes" as true, every format but .properties reads 2 as a number
    Typed read = load(new Typed(), "oneof-typed");
    Assertions.assertEquals("2", read.version);
    Assertions.assertEquals("true", read.enabled);
  }

  // </editor-fold>

  // <editor-fold desc="Elements">

  @Test
  void theElementsOfAnArrayAreChecked() {
    Assertions.assertArrayEquals(
        new String[] {"a", "b"}, load(new Letters(), "oneof-letters-listed").letters);
    assertRejected(new Letters(), "oneof-letters-unlisted", "letters[1]", "'c'");
  }

  // </editor-fold>

  // <editor-fold desc="Invalid annotations">

  @Test
  void anAnnotationOnATypeOtherThanStringIsRejected() {
    // Object could hold a String; it was accepted before
    assertRejected(new OnObject(), "oneof-value", "invalid field type");
    assertRejected(new OnList(), "oneof-values", "List<@OneOf(...) String>");
  }

  @Test
  void anAnnotationWithoutValuesIsRejected() {
    assertRejected(new Empty(), "oneof-value", "at least one value");
  }

  @Test
  void anInvalidAnnotationOnAFieldThatIsNotLoadedBreaksNothing() {
    // it failed every load and dump of the class when it was checked as the class was read
    Assertions.assertEquals("creative", load(new WithIgnored(), "oneof-listed").mode);
  }

  // </editor-fold>

  // <editor-fold desc="Values: subjects">

  static class Mode {

    @OneOf({"survival", "creative"})
    String mode = "survival";
  }

  static class Difficulty {

    @OneOf(
        value = {"easy", "hard"},
        ignoreCase = true)
    String difficulty = "easy";
  }

  static class Typed {

    @OneOf({"1", "2"})
    String version = "1";

    @OneOf({"true", "false", "auto"})
    String enabled = "auto";
  }

  // </editor-fold>

  // <editor-fold desc="Elements: subjects">

  static class Letters {

    @OneOf({"a", "b"})
    String[] letters = {"a"};
  }

  // </editor-fold>

  // <editor-fold desc="Invalid annotations: subjects">

  static class OnObject {

    @OneOf("a")
    Object value = "a";
  }

  static class OnList {

    @OneOf("a")
    List<String> values = List.of("a");
  }

  static class Empty {

    @OneOf({})
    String value = "a";
  }

  static class WithIgnored {

    @OneOf({"survival", "creative"})
    String mode = "survival";

    @Ignore
    @OneOf("a")
    int ignored = 1;
  }

  // </editor-fold>
}
