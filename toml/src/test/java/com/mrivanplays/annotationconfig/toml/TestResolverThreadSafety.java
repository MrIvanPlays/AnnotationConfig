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

package com.mrivanplays.annotationconfig.toml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The shared resolver is created once even when the first calls come from many threads at once.
 * Other tests have usually created it already, so the entry point is loaded again by a fresh class
 * loader, where its resolver does not exist yet.
 */
class TestResolverThreadSafety {

  private static final int THREADS = 16;

  @Test
  void concurrentFirstCallsGetOneResolver() throws Exception {
    assertOneInstance(TomlConfig.class, "getConfigResolver");
  }

  private static void assertOneInstance(Class<?> entryPoint, String getter) throws Exception {
    Method method =
        new PackageFirstLoader(entryPoint).loadClass(entryPoint.getName()).getMethod(getter);
    CyclicBarrier start = new CyclicBarrier(THREADS);
    try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
      List<Future<Object>> results = new ArrayList<>();
      for (int i = 0; i < THREADS; i++) {
        results.add(
            executor.submit(
                () -> {
                  start.await();
                  return method.invoke(null);
                }));
      }
      Object first = results.getFirst().get();
      Assertions.assertNotNull(first);
      for (Future<Object> result : results) {
        Assertions.assertSame(first, result.get());
      }
    }
  }

  /**
   * Defines the classes of one package itself instead of asking its parent, so their static state
   * starts fresh. The whole package is loaded, because classes from different loaders don't share
   * package-private access.
   */
  private static final class PackageFirstLoader extends ClassLoader {

    private final String packagePrefix;

    PackageFirstLoader(Class<?> member) {
      super(member.getClassLoader());
      this.packagePrefix = member.getPackageName() + ".";
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (!name.startsWith(packagePrefix) || name.indexOf('.', packagePrefix.length()) >= 0) {
        return super.loadClass(name, resolve);
      }
      synchronized (getClassLoadingLock(name)) {
        Class<?> loaded = findLoadedClass(name);
        if (loaded != null) {
          return loaded;
        }
        try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
          if (in == null) {
            throw new ClassNotFoundException(name);
          }
          byte[] bytes = in.readAllBytes();
          return defineClass(name, bytes, 0, bytes.length);
        } catch (IOException e) {
          throw new ClassNotFoundException(name, e);
        }
      }
    }
  }
}
