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

package com.mrivanplays.annotationconfig.core.annotations.custom;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Represents a response of a custom annotation value validation.
 *
 * @author Ivan Pekov
 * @since 2.0.0
 */
@NullMarked
@SuppressWarnings("unused")
public interface ValidationResponse {

  /**
   * Returns validation response given the boolean value.
   *
   * @param successful whether successful
   * @return validation response
   */
  static ValidationResponse fromBooleanValue(boolean successful) {
    return successful ? success() : failSilently();
  }

  /**
   * Returns validation response given the boolean value. If it fails, the specified {@link
   * Throwable} {@code onFailError} will be thrown via a wrapped {@link RuntimeException}.
   *
   * @param successful whether successful
   * @param onFailError error, thrown on fail
   * @return validation response
   */
  static ValidationResponse fromBooleanValue(boolean successful, Throwable onFailError) {
    return successful ? success() : fail(onFailError);
  }

  /**
   * Returns validation response given the boolean value. If it fails, the specified {@link
   * Throwable} {@code onFailError} will be thrown via a wrapped {@link RuntimeException}. If it is
   * successful, it will execute the specified {@link Runnable} {@code onSuccess}.
   *
   * @param successful whether successful
   * @param onFailError error, thrown on fail
   * @param onSuccess runnable, ran on success
   * @return validation response
   */
  static ValidationResponse fromBooleanValue(
      boolean successful, Throwable onFailError, Runnable onSuccess) {
    return successful ? success(onSuccess) : fail(onFailError);
  }

  /**
   * Returns a successful validation response. The specified {@link Runnable} {@code runnable} will
   * be executed whenever we get to this response.
   *
   * @param runnable runnable to execute
   * @return successful validation response
   */
  static ValidationResponse success(Runnable runnable) {
    return new ValidationResponse() {
      @Override
      public Runnable onSuccess() {
        return runnable;
      }
    };
  }

  /** Returns successful validation response. */
  ValidationResponse SUCCESS = new ValidationResponse() {};

  /** Returns a failure validation response, which is going to cancel field set silently. */
  ValidationResponse SILENT_FAIL =
      new ValidationResponse() {
        @Override
        public boolean shouldFailSilently() {
          return true;
        }
      };

  /**
   * Returns successful validation response.
   *
   * @return successful validation response
   */
  static ValidationResponse success() {
    return SUCCESS;
  }

  /**
   * Returns a failure validation response. The specified {@link Throwable} {@code error} will be
   * thrown via a wrapped {@link RuntimeException} whenever we get to this response.
   *
   * @param error the error to throw
   * @return failure validation response
   */
  static ValidationResponse fail(Throwable error) {
    return new ValidationResponse() {
      @Override
      public Throwable throwError() {
        return error;
      }
    };
  }

  /**
   * Returns a failure validation response, which is going to cancel field set silently.
   *
   * @return silent failure validation response
   */
  static ValidationResponse failSilently() {
    return SILENT_FAIL;
  }

  /**
   * Returns a {@link Runnable} to run, which marks this validation response is successful.
   *
   * @return runnable to run
   */
  @Nullable
  default Runnable onSuccess() {
    return null;
  }

  /**
   * Returns whether to fail silently, which marks this validation response is not successful.
   *
   * @return true in order to fail silently, false otherwise
   */
  default boolean shouldFailSilently() {
    return false;
  }

  /**
   * Returns a {@link Throwable} to throw, which marks this validation response is not successful.
   * The returned throwable will be thrown via a wrapped {@link RuntimeException}.
   *
   * @return throwable to throw
   */
  @Nullable
  default Throwable throwError() {
    return null;
  }
}
