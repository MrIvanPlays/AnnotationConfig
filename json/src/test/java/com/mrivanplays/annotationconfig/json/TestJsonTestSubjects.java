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

package com.mrivanplays.annotationconfig.json;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Writing and reading each number type; every subject has a single public field. */
class TestJsonTestSubjects {

  static Stream<Arguments> writeCases() {
    return Stream.of(
        Arguments.of(new JsonTestSubjects.TByte(), "{\n  \"b\": 1\n}\n"),
        Arguments.of(new JsonTestSubjects.TDouble(), "{\n  \"d\": 2.5\n}\n"),
        Arguments.of(new JsonTestSubjects.TFloat(), "{\n  \"f\": 3.11\n}\n"),
        Arguments.of(new JsonTestSubjects.TLong(), "{\n  \"l\": 22221111\n}\n"),
        Arguments.of(new JsonTestSubjects.TShort(), "{\n  \"s\": 5\n}\n"));
  }

  static Stream<Arguments> readCases() {
    return Stream.of(
        Arguments.of(new JsonTestSubjects.TByte(), "{\n  \"b\": 125\n}", (byte) 125),
        Arguments.of(new JsonTestSubjects.TDouble(), "{\n  \"d\": 3.3\n}", 3.3),
        Arguments.of(new JsonTestSubjects.TFloat(), "{\n  \"f\": 5.51\n}", 5.51f),
        Arguments.of(new JsonTestSubjects.TLong(), "{\n  \"l\": 2222112\n}", 2222112L),
        Arguments.of(new JsonTestSubjects.TShort(), "{\n  \"s\": 6\n}", (short) 6));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("writeCases")
  void testWrite(Object subject, String expected) {
    StringWriter dump = new StringWriter();
    JsonConfig.getConfigResolver().dump(subject, dump);
    Assertions.assertEquals(expected, dump.toString());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("readCases")
  void testRead(Object subject, String json, Object expected) throws Exception {
    JsonConfig.getConfigResolver().load(subject, new StringReader(json));
    // the boxed value has to match exactly, so its type is checked too
    Assertions.assertEquals(expected, subject.getClass().getFields()[0].get(subject));
  }
}
