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

package com.mrivanplays.annotationconfig.scenarios.lineendings;

import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import com.mrivanplays.annotationconfig.scenarios.Format;
import com.mrivanplays.annotationconfig.scenarios.Scenario;
import com.mrivanplays.annotationconfig.scenarios.Sources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every format is written with '\n' on every OS. The class comment used the OS line separator and
 * the values did not, so files written on Windows mixed CRLF and LF. The outputs are compared
 * without normalizing line endings, so a regression only shows on Windows.
 */
@ParameterizedClass(name = "{0}")
@MethodSource(Sources.ALL)
class TestLineEndings extends Scenario {

  TestLineEndings(Format format) {
    super(format, "line-endings");
  }

  @Test
  void linesEndWithLineFeeds() {
    assertDump("output", new Subject());
  }

  @Comment("Class comment")
  static class Subject {

    @Comment("field comment")
    public int a = 1;

    public int b = 2;
  }
}
