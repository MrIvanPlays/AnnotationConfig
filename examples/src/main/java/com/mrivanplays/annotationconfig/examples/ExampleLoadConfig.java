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
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import com.mrivanplays.annotationconfig.core.utils.TypeToken;
import com.mrivanplays.annotationconfig.examples.ExampleConfig.SectionObject;
import com.mrivanplays.annotationconfig.examples.ExampleConfig.SomethingToSerialize;
import com.mrivanplays.annotationconfig.json.JsonConfig;
import com.mrivanplays.annotationconfig.toml.TomlConfig;
import com.mrivanplays.annotationconfig.yaml.SectionObjectList;
import com.mrivanplays.annotationconfig.yaml.SectionObjectListSerializer;
import com.mrivanplays.annotationconfig.yaml.YamlConfig;
import java.io.File;

/**
 * Loads {@link ExampleConfig}, or writes defaults for the config type there isn't a default for.
 */
public class ExampleLoadConfig {

  static void main(String[] args) {
    SerializerRegistry serializers = SerializerRegistry.INSTANCE;
    CommentPlaceholderRegistry commentPlaceholders = CommentPlaceholderRegistry.INSTANCE;

    // register our custom serializers
    serializers.registerSerializer(
        SomethingToSerialize.class, new ExampleConfig.SomethingToSerializeSerializer());

    // register a custom placeholder; a real application would put its own version here
    commentPlaceholders.registerPlaceholder(CommentPlaceholder.simple("%version%", "3.1.0"));

    // loads if the file exists, otherwise writes the defaults to it
    ExampleConfig config = new ExampleConfig();

    // this main class is also used to generate the example files, so this is not an "exact"
    // practice of how to load a config. Make sure you're using the examples just as "examples".
    loadJson5(config);
    loadJson(config);
    loadToml(config);
    loadYaml(serializers);

    // do stuff with config ...
  }

  private static void loadJson5(ExampleConfig cfg) {
    JsonConfig.getJson5ConfigResolver()
        .loadOrDump(cfg, new File("examples/application-config.json5"));
  }

  private static void loadJson(ExampleConfig cfg) {
    JsonConfig.getConfigResolver().loadOrDump(cfg, new File("examples/application-config.json"));
  }

  private static void loadToml(ExampleConfig cfg) {
    TomlConfig.getConfigResolver().loadOrDump(cfg, new File("examples/application-config.toml"));
  }

  private static void loadYaml(SerializerRegistry serializers) {
    serializers.registerSerializer(
        new TypeToken<SectionObjectList<SectionObject>>() {}.getType(),
        new SectionObjectListSerializer<>());
    // example settings usage.
    ExampleConfigYaml yaml = new ExampleConfigYaml();
    YamlConfig.getConfigResolver()
        .loadOrDump(
            yaml,
            new File("examples/application-config.yaml"),
            new Settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true));
  }
}
