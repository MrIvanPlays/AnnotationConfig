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
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.utils.AnnotationUtils;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GetCommentsTest {

  static class Subject {

    @Comment("This is a single comment")
    private String aa = "bb";

    @Comment("And these are")
    @Comment("Multiple comments")
    private String bb = "aa";
  }

  @Test
  void testGetComments() throws Exception {
    Subject subj = new Subject();
    Field aaField = subj.getClass().getDeclaredField("aa");
    Field bbField = subj.getClass().getDeclaredField("bb");

    List<String> aaComments =
        AnnotationUtils.getComments(AnnotationAccessor.createFromField(aaField));
    List<String> bbComments =
        AnnotationUtils.getComments(AnnotationAccessor.createFromField(bbField));

    Assertions.assertEquals(1, aaComments.size());
    Assertions.assertEquals("This is a single comment", aaComments.getFirst());

    Assertions.assertEquals(2, bbComments.size());
    Assertions.assertEquals("And these are", bbComments.getFirst());
    Assertions.assertEquals("Multiple comments", bbComments.get(1));
  }
}
