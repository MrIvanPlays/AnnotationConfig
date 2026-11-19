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

package com.mrivanplays.annotationconfig.json;

import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestJsonTestSubjects {

  @Test
  void testByteWrite() {
    StringWriter dump = new StringWriter();
    JsonTestSubjects.TByte bb = new JsonTestSubjects.TByte();
    JsonConfig.getConfigResolver().dump(bb, dump);

    String expected = "{\n  \"b\": 1\n}";

    Assertions.assertEquals(expected, dump.toString());
  }

  @Test
  void testByteRead() {
    StringReader read = new StringReader("{\n  \"b\": 125\n}");
    JsonTestSubjects.TByte bb = new JsonTestSubjects.TByte();
    JsonConfig.getConfigResolver().load(bb, read);

    Assertions.assertEquals(Byte.MAX_VALUE - 2, bb.b);
  }

  @Test
  void testDoubleWrite() {
    StringWriter dump = new StringWriter();
    JsonTestSubjects.TDouble dd = new JsonTestSubjects.TDouble();
    JsonConfig.getConfigResolver().dump(dd, dump);

    String expected = "{\n  \"d\": 2.5\n}";

    Assertions.assertEquals(expected, dump.toString());
  }

  @Test
  void testDoubleRead() {
    StringReader read = new StringReader("{\n  \"d\": 3.3\n}");
    JsonTestSubjects.TDouble dd = new JsonTestSubjects.TDouble();
    JsonConfig.getConfigResolver().load(dd, read);

    Assertions.assertEquals(3.3, dd.d);
  }

  @Test
  void testFloatWrite() {
    StringWriter dump = new StringWriter();
    JsonTestSubjects.TFloat ff = new JsonTestSubjects.TFloat();
    JsonConfig.getConfigResolver().dump(ff, dump);

    String expected = "{\n  \"f\": 3.11\n}";

    Assertions.assertEquals(expected, dump.toString());
  }

  @Test
  void testFloatRead() {
    StringReader read = new StringReader("{\n  \"f\": 5.51\n}");
    JsonTestSubjects.TFloat ff = new JsonTestSubjects.TFloat();
    JsonConfig.getConfigResolver().load(ff, read);

    Assertions.assertEquals(5.51f, ff.f);
  }

  @Test
  void testLongWrite() {
    StringWriter dump = new StringWriter();
    JsonTestSubjects.TLong ll = new JsonTestSubjects.TLong();
    JsonConfig.getConfigResolver().dump(ll, dump);

    String expected = "{\n  \"l\": 22221111\n}";

    Assertions.assertEquals(expected, dump.toString());
  }

  @Test
  void testLongRead() {
    StringReader read = new StringReader("{\n  \"l\": 2222112\n}");
    JsonTestSubjects.TLong ll = new JsonTestSubjects.TLong();
    JsonConfig.getConfigResolver().load(ll, read);

    Assertions.assertEquals(2222112, ll.l);
  }

  @Test
  void testShortWrite() {
    StringWriter dump = new StringWriter();
    JsonTestSubjects.TShort ss = new JsonTestSubjects.TShort();
    JsonConfig.getConfigResolver().dump(ss, dump);

    String expected = "{\n  \"s\": 5\n}";

    Assertions.assertEquals(expected, dump.toString());
  }

  @Test
  void testShortRead() {
    StringReader read = new StringReader("{\n  \"s\": 6\n}");
    JsonTestSubjects.TShort ss = new JsonTestSubjects.TShort();
    JsonConfig.getConfigResolver().load(ss, read);

    Assertions.assertEquals(6, ss.s);
  }
}
