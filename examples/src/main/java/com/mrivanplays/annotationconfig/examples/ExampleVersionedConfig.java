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

package com.mrivanplays.annotationconfig.examples;

import com.mrivanplays.annotationconfig.core.annotations.ConfigVersion;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import org.jspecify.annotations.NullMarked;

/**
 * A config with a version, written to {@code examples/versioned-config.yaml}. {@link
 * ExampleMigrateConfig} registers the migrations that bring files of older versions up to date.
 *
 * <p>Its options changed twice. Version 1 had them all at the top level, with the timeout in
 * seconds:
 *
 * <pre>{@code
 * host: "0.0.0.0"
 * port: 25565
 * timeout: 30
 * }</pre>
 *
 * <p>Version 2 moved the host and the port into the section {@code network}, and version 3 made
 * the timeout a duration ({@code "30s"}).
 */
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "unused"})
@ConfigVersion(
    value = 3,
    comments = "The version of this file, used to update it. Don't change it.")
@NullMarked
public class ExampleVersionedConfig {

  @Comment("The address to bind to")
  @Key("network.host")
  private String host = "0.0.0.0";

  @Comment("The port to bind to")
  @Key("network.port")
  private int port = 25565;

  @Comment("How long to wait for a client, e.g. 30s or 2m")
  private String timeout = "30s";

  // getters would go here
}
