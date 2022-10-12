package com.mrivanplays.annotationconfig.core.utils;

/**
 * Represents a function, taking 3 inputs and receiving a return value.
 *
 * @param <A> input a
 * @param <B> input b
 * @param <C> input c
 * @param <R> return value
 * @since 3.0.0
 * @author MrIvanPlays
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {

  /**
   * Applies this function to the given arguments.
   *
   * @param a function argument a
   * @param b function argument b
   * @param c function argument c
   * @return function result
   */
  R apply(A a, B b, C c);
}
