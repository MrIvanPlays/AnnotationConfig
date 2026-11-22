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
import org.jspecify.annotations.NonNull;

/**
 * Represents the default yaml value writer. It uses homebrew writing to support comments.
 *
 * @author Ivan Pekov
 * @since 2.1.0
 * @deprecated replaced by {@link YamlHandler}
 */
@Deprecated(since = "3.0.4")
public final class YamlValueWriter implements ValueWriter {

  @Override
  public void write(
      @NonNull Map<String, Object> values,
      @NonNull Map<String, List<String>> fieldComments,
      @NonNull PrintWriter writer,
      @NonNull Settings settings) {
    YamlConfig.HANDLER.write(values, fieldComments, writer, settings);
  }
}
