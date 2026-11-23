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

import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

/**
 * Numbers and booleans are built by hand for the default Yaml instance instead of through
 * SnakeYAML's representer. The output has to be the same as the representer's.
 */
public class TestYamlScalars {

  static class Scalars {

    public int integer = -42;
    public long longValue = Long.MAX_VALUE;
    public short shortValue = 7;
    public byte byteValue = -1;
    public BigInteger bigInteger = new BigInteger("123456789012345678901234567890");
    public double decimal = 2.5;
    public double exponent = 1e300;
    public float single = 3.11f;
    public BigDecimal bigDecimal = new BigDecimal("0.12345678901234567890");
    public double notANumber = Double.NaN;
    public double positiveInfinity = Double.POSITIVE_INFINITY;
    public double negativeInfinity = Double.NEGATIVE_INFINITY;
    public float floatNotANumber = Float.NaN;
    public float floatInfinity = Float.POSITIVE_INFINITY;
    public boolean yes = true;
    public boolean no = false;
    public int[] numbers = {1, -2, 3};
    public List<Double> doubles = List.of(0.5, Double.NaN);
    public List<Boolean> booleans = List.of(true, false);
  }

  private static String dump(Settings settings) {
    StringWriter writer = new StringWriter();
    YamlConfig.getConfigResolver().dump(new Scalars(), writer, settings);
    return writer.toString();
  }

  /** A configured instance with the default options, which goes through the representer. */
  private static Yaml representerInstance() {
    DumperOptions options = new DumperOptions();
    options.setProcessComments(true);
    options.setDefaultFlowStyle(FlowStyle.BLOCK);
    options.setIndent(2);
    options.setIndicatorIndent(2);
    options.setIndentWithIndicator(true);
    options.setSplitLines(false);
    return new Yaml(options);
  }

  @Test
  public void scalarsMatchSnakeYamlsRepresenter() {
    String fast = dump(new Settings());
    String represented = dump(new Settings().put(YamlConfig.YAML_INSTANCE, representerInstance()));
    Assertions.assertEquals(represented, fast);
  }

  @Test
  public void specialFloatValues() {
    String yaml = dump(new Settings());
    Assertions.assertTrue(yaml.contains("notANumber: .NaN\n"), yaml);
    Assertions.assertTrue(yaml.contains("positiveInfinity: .inf\n"), yaml);
    Assertions.assertTrue(yaml.contains("negativeInfinity: -.inf\n"), yaml);
    Assertions.assertTrue(yaml.contains("bigDecimal: 0.12345678901234567890\n"), yaml);
    Assertions.assertTrue(yaml.contains("numbers: [1, -2, 3]\n"), yaml);
  }
}
