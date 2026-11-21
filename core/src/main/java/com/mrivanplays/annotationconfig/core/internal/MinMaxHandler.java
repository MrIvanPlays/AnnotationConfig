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

package com.mrivanplays.annotationconfig.core.internal;

import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @hidden
 */
@NullMarked
class MinMaxHandler {

  enum State {
    START,
    INVALID_MIN,
    INVALID_MAX,
    MORE_THAN_ONE_MIN,
    MORE_THAN_ONE_MAX,
    UNDER,
    ABOVE,
    ALRIGHT
  }

  static class NumberResult {

    static NumberResult stateOnly(State state) {
      return result(state, null);
    }

    static NumberResult result(State state, @Nullable Number number) {
      return new NumberResult(state, number);
    }

    private final State state;
    @Nullable private final Number number;

    private NumberResult(State state, @Nullable Number number) {
      this.state = state;
      this.number = number;
    }

    public boolean isStateOnly() {
      return number == null;
    }

    public State getState() {
      return state;
    }

    public Optional<Number> getNumber() {
      return Optional.ofNullable(number);
    }
  }

  static State compare(NumberResult min, NumberResult max, String compareTo) {
    return compare(min, max, compareTo.length());
  }

  static State compare(NumberResult minResult, NumberResult maxResult, Number compareTo) {
    State minState = minResult.getState();
    State maxState = maxResult.getState();
    if (minResult.isStateOnly() && minState != State.START) {
      return minResult.getState();
    }
    if (maxResult.isStateOnly() && maxState != State.START) {
      return maxResult.getState();
    }
    if (minState != State.START && maxState != State.START) {
      Number min = minResult.getNumber().get();
      Number max = maxResult.getNumber().get();
      if (compareTo.intValue() < min.intValue()) {
        return State.UNDER;
      }
      if (compareTo.intValue() > max.intValue()) {
        return State.ABOVE;
      }
      return State.ALRIGHT;
    }
    if (minState != State.START) {
      Number min = minResult.getNumber().get();
      if (compareTo.intValue() < min.intValue()) {
        return State.UNDER;
      }
    }
    if (maxState != State.START) {
      Number max = maxResult.getNumber().get();
      if (compareTo.intValue() > max.intValue()) {
        return State.ABOVE;
      }
    }
    return State.ALRIGHT;
  }

  static NumberResult getNumber(Min min) {
    if (!isValid(min)) {
      return NumberResult.stateOnly(State.INVALID_MIN);
    }
    if (min.minByte() != -1) {
      if (min.minLong() != -1
          || min.minFloat() != -1
          || min.minShort() != -1
          || min.minInt() != -1
          || min.minDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MIN);
      }
      return NumberResult.result(State.ALRIGHT, (min.minByte()));
    }
    if (min.minLong() != -1) {
      if (min.minFloat() != -1
          || min.minShort() != -1
          || min.minInt() != -1
          || min.minDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MIN);
      }
      return NumberResult.result(State.ALRIGHT, (min.minLong()));
    }
    if (min.minFloat() != -1) {
      if (min.minShort() != -1 || min.minInt() != -1 || min.minDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MIN);
      }
      return NumberResult.result(State.ALRIGHT, (min.minFloat()));
    }
    if (min.minShort() != -1) {
      if (min.minInt() != -1 || min.minDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MIN);
      }
      return NumberResult.result(State.ALRIGHT, (min.minShort()));
    }
    if (min.minInt() != -1) {
      if (min.minDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MIN);
      }
      return NumberResult.result(State.ALRIGHT, (min.minInt()));
    }
    return NumberResult.result(State.ALRIGHT, (min.minDouble()));
  }

  static NumberResult getNumber(Max max) {
    if (!isValid(max)) {
      return NumberResult.stateOnly(State.INVALID_MAX);
    }
    if (max.maxByte() != -1) {
      if (max.maxLong() != -1
          || max.maxFloat() != -1
          || max.maxShort() != -1
          || max.maxInt() != -1
          || max.maxDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MAX);
      }
      return NumberResult.result(State.ALRIGHT, (max.maxByte()));
    }
    if (max.maxLong() != -1) {
      if (max.maxFloat() != -1
          || max.maxShort() != -1
          || max.maxInt() != -1
          || max.maxDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MAX);
      }
      return NumberResult.result(State.ALRIGHT, (max.maxLong()));
    }
    if (max.maxFloat() != -1) {
      if (max.maxShort() != -1 || max.maxInt() != -1 || max.maxDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MAX);
      }
      return NumberResult.result(State.ALRIGHT, (max.maxFloat()));
    }
    if (max.maxShort() != -1) {
      if (max.maxInt() != -1 || max.maxDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MAX);
      }
      return NumberResult.result(State.ALRIGHT, (max.maxShort()));
    }
    if (max.maxInt() != -1) {
      if (max.maxDouble() != -1) {
        return NumberResult.stateOnly(State.MORE_THAN_ONE_MAX);
      }
      return NumberResult.result(State.ALRIGHT, (max.maxInt()));
    }
    return NumberResult.result(State.ALRIGHT, (max.maxDouble()));
  }

  private static boolean isValid(Min min) {
    return min.minByte() != -1
        || min.minDouble() != -1
        || min.minInt() != -1
        || min.minFloat() != -1
        || min.minLong() != -1
        || min.minShort() != -1;
  }

  private static boolean isValid(Max max) {
    return max.maxByte() != -1
        || max.maxDouble() != -1
        || max.maxInt() != -1
        || max.maxFloat() != -1
        || max.maxShort() != -1
        || max.maxLong() != -1;
  }
}
