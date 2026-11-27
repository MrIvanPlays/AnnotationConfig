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

package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.resolver.ValueWriter;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * The YAML value writer of 3.0.x. It now writes through {@link YamlHandler}, as {@link
 * YamlConfig#getConfigResolver()} does.
 *
 * @author Ivan Pekov
 * @since 2.1.0
 * @deprecated replaced by {@link YamlHandler}
 */
@Deprecated(since = "3.1.0")
@NullMarked
public final class YamlValueWriter implements ValueWriter {

  @Override
  public void write(
      Map<String, Object> values,
      Map<String, List<String>> fieldComments,
      PrintWriter writer,
      Settings settings) {
    YamlConfig.HANDLER.write(values, fieldComments, writer, settings);
  }

  @Override
  public String schemaComment(String schema) {
    return YamlConfig.HANDLER.schemaComment(schema);
  }
}
