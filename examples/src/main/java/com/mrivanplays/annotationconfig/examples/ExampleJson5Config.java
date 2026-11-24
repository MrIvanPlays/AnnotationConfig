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

import com.mrivanplays.annotationconfig.core.annotations.ConfigObject;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.comment.Comment;
import java.util.List;

/**
 * A config stored as JSON5, which is JSON with comments. {@link ExampleLoadJson5Config} loads it,
 * and {@code examples/server.json5} is the file it writes with these defaults.
 */
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "unused"})
@Comment("Server settings")
public class ExampleJson5Config {

  @Comment("The address to bind to")
  private String host = "0.0.0.0";

  @Comment("Must be between 1 and 65535")
  @Min(minInt = 1)
  @Max(maxInt = 65535)
  private int port = 25565;

  @Comment("A dotted key is written as a nested object")
  @Key("motd.text")
  private String motd = "Welcome!";

  // not a JavaScript identifier, so this key is written in quotes
  @Key("allowed-worlds")
  private List<String> allowedWorlds = List.of("world", "world_nether");

  @ConfigObject private Database database = new Database();

  /** A config object, written as a nested JSON5 object. */
  @Comment("Database connection")
  public static final class Database {

    private String url = "jdbc:mysql://localhost/server";

    @Comment("In seconds")
    private int timeout = 30;

    // getters would go here
  }

  // getters would go here
}
