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
 * Restricts a {@code String} option to a fixed set of values. Loading a config whose value is not
 * one of them fails with an {@link IllegalArgumentException} that names the option and lists the
 * allowed values.
 *
 * <pre>{@code
 * @OneOf({"survival", "creative", "adventure"})
 * private String gamemode = "survival";
 * }</pre>
 *
 * <p>The value is checked as the field receives it, after the format's value was turned into a
 * {@code String}: a number or boolean in the file, such as YAML's {@code mode: 1} or {@code mode:
 * yes}, is compared as {@code "1"} or {@code "true"}. A key missing from the config is not checked,
 * and neither is the field's default value.
 *
 * <p>Like {@link Min} and {@link Max} it can be put on the element types of a field's type, to
 * restrict every element:
 *
 * <pre>{@code
 * private List<@OneOf({"read", "write"}) String> permissions = List.of("read");
 * private Map<@OneOf({"eu", "us"}) String, Integer> servers = Map.of("eu", 2);
 * private @OneOf({"a", "b"}) String[] letters = {"a"}; // restricts the elements
 * }</pre>
 *
 * <p><b>{@code @OneOf} or an enum?</b> An enum field already accepts only its constants, matches
 * them ignoring case and gives you a type to {@code switch} on, so prefer it when the values mean
 * something to your code. Use {@code @OneOf} when the option stays a plain {@code String}: values
 * that aren't valid Java identifiers ({@code "1.20.4"}, {@code "en-US"}), values you pass on as
 * text, or a {@code String} field you can't change to an enum without breaking your API.
 *
 * <p>It may only be put on {@code String} types, and must list at least one value; otherwise
 * loading a value for the field fails.
 *
 * @author Ivan Pekov
 * @since 3.1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE_USE})
public @interface OneOf {

  /**
   * The values the option may have.
   *
   * @return the allowed values
   */
  String[] value();

  /**
   * Whether the values are compared ignoring case. The field gets the value as it was written in
   * the config, not as it is listed here: with {@code @OneOf(value = "hard", ignoreCase = true)},
   * {@code HARD} is read as {@code "HARD"}.
   *
   * @return whether case is ignored
   */
  boolean ignoreCase() default false;
}
