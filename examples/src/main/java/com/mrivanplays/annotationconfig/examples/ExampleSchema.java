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

import com.mrivanplays.annotationconfig.core.resolver.CommentPlaceholder;
import com.mrivanplays.annotationconfig.core.resolver.CommentPlaceholderRegistry;
import com.mrivanplays.annotationconfig.core.resolver.ConfigSchema;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.examples.ExampleConfig.SomethingToSerialize;
import com.mrivanplays.annotationconfig.json.JsonConfig;
import java.nio.file.Path;

/**
 * Writes the JSON Schema of {@link ExampleConfig} to {@code
 * examples/application-config.schema.json}. An editor that knows it completes and checks the
 * config while it is edited: in VS Code, a {@code "$schema"} key in a JSON file, a {@code #
 * yaml-language-server: $schema=...} comment in a YAML file (with the YAML extension), or a {@code
 * #:schema ...} comment in a TOML file (with Even Better TOML).
 */
public class ExampleSchema {

  static void main(String[] args) {
    // the same serializers and placeholders as when the config is written: a type with a
    // serializer of its own can be anything in the schema, as only the serializer knows what it
    // writes, and the comments are the descriptions
    SerializerRegistry.INSTANCE.registerSerializer(
        SomethingToSerialize.class, new ExampleConfig.SomethingToSerializeSerializer());
    CommentPlaceholderRegistry.INSTANCE.registerPlaceholder(
        CommentPlaceholder.simple("%version%", "3.1.0"));

    // the schema of the files this resolver writes; the config's values are the defaults
    ConfigSchema schema = JsonConfig.getConfigResolver().schema(new ExampleConfig());
    schema.write(Path.of("examples/application-config.schema.json"));

    // to have dumps reference it, so that an editor finds it without being configured, load or
    // dump with new Settings().put(ACDefaultSettings.SCHEMA_REFERENCE, "config.schema.json")
  }
}
