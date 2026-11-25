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

package com.mrivanplays.annotationconfig.core.custom;

import com.mrivanplays.annotationconfig.core.annotations.custom.AnnotationValidator;
import com.mrivanplays.annotationconfig.core.annotations.custom.CustomAnnotationRegistry;
import com.mrivanplays.annotationconfig.core.annotations.custom.ValidationResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestCustomAnnotationRegistry {

  private static final CustomAnnotationRegistry REGISTRY = CustomAnnotationRegistry.INSTANCE;
  private static final AnnotationValidator<Checked> VALIDATOR =
      (_, _, _, _) -> ValidationResponse.success();

  // the registry is global, so no test may leave the validator registered
  @AfterEach
  void unregister() {
    if (REGISTRY.hasValidator(Checked.class)) {
      REGISTRY.unregister(Checked.class);
    }
  }

  @Test
  void registerAndUnregister() {
    REGISTRY.register(Checked.class, VALIDATOR);
    Assertions.assertTrue(REGISTRY.hasValidator(Checked.class));
    Assertions.assertFalse(REGISTRY.isEmpty());
    Assertions.assertSame(VALIDATOR, REGISTRY.getValidator(Checked.class).orElseThrow());

    REGISTRY.unregister(Checked.class);
    Assertions.assertFalse(REGISTRY.hasValidator(Checked.class));
    Assertions.assertTrue(REGISTRY.getValidator(Checked.class).isEmpty());
  }

  @Test
  void registeringTwiceIsRejected() {
    REGISTRY.register(Checked.class, VALIDATOR);
    AnnotationValidator<Checked> other = (_, _, _, _) -> ValidationResponse.failSilently();

    IllegalArgumentException e =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> REGISTRY.register(Checked.class, other));
    Assertions.assertTrue(e.getMessage().contains("already registered"), e.getMessage());
    Assertions.assertSame(VALIDATOR, REGISTRY.getValidator(Checked.class).orElseThrow());
  }

  @Test
  void unregisteringWithoutValidatorIsRejected() {
    IllegalArgumentException e =
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> REGISTRY.unregister(Checked.class));
    Assertions.assertTrue(e.getMessage().contains("is not registered"), e.getMessage());
  }

  @Test
  void concurrentRegistrationsLetExactlyOneWin() throws Exception {
    int threads = 16;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger rejected = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (int i = 0; i < threads; i++) {
        futures.add(
            executor.submit(() -> {
              start.await();
              try {
                REGISTRY.register(Checked.class, VALIDATOR);
              } catch (IllegalArgumentException e) {
                rejected.incrementAndGet();
              }
              return null;
            }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get();
      }
    } finally {
      executor.shutdownNow();
    }
    Assertions.assertEquals(threads - 1, rejected.get());
  }

  /** An annotation only this test registers validators for. */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  @interface Checked {}
}
