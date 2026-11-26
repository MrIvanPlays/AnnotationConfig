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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gives a config class a version, which is written to its files and brings older files up to date
 * when they are loaded.
 *
 * <p>The version is written as the first option of the file, under {@link #key()}. A file without
 * it is version 1, so files written before the class had a version are migrated from version 1.
 *
 * <p>When a file of an older version is loaded, the {@link
 * com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategy migration strategies}
 * registered in {@link
 * com.mrivanplays.annotationconfig.core.resolver.versioning.MigrationStrategies} for the class
 * that carries this annotation change the values read from the file, one step after another, until
 * they are of this version; then the fields are set from them. A file loaded from a path is then
 * rewritten in the current version, after the old file is copied to {@code <name>.v<version>.bak}
 * next to it. Loading from a reader or a map migrates the values without writing anything.
 *
 * <p>If no strategy is registered for the class at all, an older file is read as it is, without
 * migrating, copying or rewriting it, and a warning is logged through {@link System.Logger}
 * ({@code com.mrivanplays.annotationconfig.migration}) on every load. So register the strategies
 * before the first load: a dump, or a load that writes missing options with {@code
 * GENERATE_NEW_OPTIONS}, would replace such a file in the current version without migrating it
 * or backing it up, and the strategies would never be applied to it.
 *
 * <p>Loading fails, and the file is left untouched, if the file is of a newer version than the
 * class, if strategies are registered but none continues the chain at a version on the way, or if
 * a strategy fails.
 *
 * <p>Only the class of the config is versioned: the annotation has no effect on {@link
 * ConfigObject} sections. Subclasses inherit it.
 *
 * @author Ivan Pekov
 * @since 3.1.0
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigVersion {

  /**
   * The current version of the config class, at least 1.
   *
   * @return version
   */
  int value();

  /**
   * The key the version is written under, at the top level of the file. It can't hold a dot, so
   * that every format reads it the same way.
   *
   * @return key of the version
   */
  String key() default "config-version";

  /**
   * The comments to write on top of the "config version" field.
   *
   * @return comments to write
   */
  String[] comments() default {};
}
