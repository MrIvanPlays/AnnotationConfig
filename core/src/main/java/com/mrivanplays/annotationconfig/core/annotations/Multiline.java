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

package com.mrivanplays.annotationconfig.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which tells AnnotationConfig that the field it is annotated on should be written as a
 * multiline {@link String}. This annotation can only be applied on {@link java.lang.reflect.Field
 * Fields} with type {@link String}.
 *
 * <p>(YAML related): Since 3.1.0 the YAML module writes values through SnakeYAML instead of its own
 * writer, i.e. AnnotationConfig does a bit of preprocessing before calling SnakeYAML's dumper. This
 * means that, if double quotes are used to dump multiline strings, SnakeYAML writes them as {@code
 * "foo \n bar \n baz"}. The string still reads back with its line breaks, but it isn't pretty in
 * the file. For a block, change the marker char to either '|' or '>'. The default is kept for
 * compatibility.
 *
 * @since 2.1.0
 * @author Ivan Pekov
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Multiline {

  /**
   * The character to dump which indicates a multiline {@code String}. AnnotationConfig supports
   * double quotes, '>' and '|', but if a custom configuration type is used, it may support other
   * characters.
   *
   * @return character indicating multiline string
   */
  char value() default '"';
}
