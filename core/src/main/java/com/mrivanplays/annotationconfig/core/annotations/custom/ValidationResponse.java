package com.mrivanplays.annotationconfig.core.annotations.custom;

/**
 * Represents a response of a custom annotation value validation.
 *
 * @author MrIvanPlays
 * @since 2.0.0
 */
public interface ValidationResponse {

  /**
   * Returns validation response given the boolean value.
   *
   * @param successful whether successful
   * @return validation response
   */
  static ValidationResponse fromBooleanValue(boolean successful) {
    if (successful) {
      return success();
    } else {
      return failSilently();
    }
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
    if (successful) {
      return success();
    } else {
      return fail(onFailError);
    }
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
  default Throwable throwError() {
    return null;
  }
}
