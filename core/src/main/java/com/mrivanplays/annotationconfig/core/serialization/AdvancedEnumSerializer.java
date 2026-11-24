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

package com.mrivanplays.annotationconfig.core.serialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiPredicate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link Enum} serializer, which uses different tricks to serialize enum constants to user-friendly
 * values for configs, and also to read them properly.
 *
 * <p>Let's be real, every developer tried or is going to try to serialize enum constants, so they
 * are more user-friendly to be configured. Yet everyone who tried wasn't really able to go further
 * than accepting this input: {@code enum_constant} for an enum constant of {@code ENUM_CONSTANT}.
 * As already mentioned, this serializer uses tricks in order to accept literally every output
 * possible. Say you have an enum constant of {@code ACCEPT_ONLY_TRUE}. This serializer serializes
 * this constant as {@code accept only true}, but upon deserialization, the serializer accepts
 * literally any possible way of combining these words. This basically means it will accept {@code
 * only accept true}, {@code accept true only}, etc., and of course the input as a regular enum
 * constant e.g. {@code ACCEPT_ONLY_TRUE} and {@code accept_only_true}.
 *
 * <p>Example: Say you have an enum of:
 *
 * <pre>{@code
 * public enum Option {
 *   ACCEPT_TRUE,
 *   ACCEPT_FALSE,
 *   ACCEPT_BOTH,
 *   DO_NOT_ACCEPT_TRUE,
 *   DO_NOT_ACCEPT_FALSE,
 *   DO_NOT_ACCEPT_BOTH
 * }
 * }</pre>
 *
 * <p>To use AdvancedEnumSerializer to serialize it, you just register it using the {@link
 * SerializerRegistry}. An example of this is:
 *
 * <pre>{@code
 * SerializerRegistry.INSTANCE.registerSerializer(
 *     Option.class, AdvancedEnumSerializer.forEnum(Option.class));
 * }</pre>
 *
 * @since 2.0.0
 * @author Ivan Pekov
 * @param <E> enum class
 */
@NullMarked
public final class AdvancedEnumSerializer<E extends Enum<E>> implements FieldTypeSerializer<E> {

  /** The default matches condition. */
  public static final BiPredicate<Integer, List<String>> DEFAULT_MATCHES_CONDITION =
      (matches, parts) -> matches == parts.size();

  private final Class<E> enumClass;
  private final BiPredicate<Integer, List<String>> matchesCondition;

  private AdvancedEnumSerializer(Class<E> enumClass) {
    this(enumClass, DEFAULT_MATCHES_CONDITION);
  }

  private AdvancedEnumSerializer(
      Class<E> enumClass, BiPredicate<Integer, List<String>> matchesCondition) {
    Objects.requireNonNull(enumClass, "enumClass");
    if (!enumClass.isEnum()) {
      throw new IllegalArgumentException("Not an enum");
    }
    this.enumClass = enumClass;
    this.matchesCondition = Objects.requireNonNull(matchesCondition, "matchesCondition");
  }

  /**
   * Creates a new {@link AdvancedEnumSerializer} for the specified {@code enumClass}.
   *
   * @param enumClass the enum class you want an advanced enum serializer
   * @param <E> enum class
   * @return advanced enum serializer
   * @throws NullPointerException if null enumClass specified
   * @throws IllegalArgumentException if specified enumClass is not an {@link Enum}
   */
  public static <E extends Enum<E>> AdvancedEnumSerializer<E> forEnum(Class<E> enumClass) {
    return new AdvancedEnumSerializer<>(enumClass);
  }

  /**
   * Creates a new {@link AdvancedEnumSerializer} for the specified {@code enumClass} with the
   * specified {@code matchesCondition}.
   *
   * <p>The matches condition is used when the deserializer does a last effort to try and return an
   * enum value from the specified input.
   *
   * @param enumClass the enum class you want an advanced enum serializer
   * @param matchesCondition matches condition
   * @param <E> enum class
   * @return advanced enum serializer
   * @throws NullPointerException if null enumClass or null matchesCondition specified
   * @throws IllegalArgumentException if the specified enumClass is not an {@link Enum}
   */
  public static <E extends Enum<E>> AdvancedEnumSerializer<E> forEnumWithCondition(
      Class<E> enumClass, BiPredicate<Integer, List<String>> matchesCondition) {
    return new AdvancedEnumSerializer<>(enumClass, matchesCondition);
  }

  private static boolean containsChar(String val, char c) {
    return val.indexOf(c) != -1;
  }

  /** {@inheritDoc} */
  @Override
  @Nullable
  public E deserialize(
      DataObject data, SerializationContext<E> context, AnnotationAccessor annotations) {
    return this.deserializeSimple(data);
  }

  /**
   * Tries to deserialize the inputted {@link DataObject data object} into an {@link Enum enum
   * constant}. If no match is found, returns {@code null}.
   *
   * @param data data object
   * @return enum constant deserialized, or null
   * @since 3.1.0
   */
  @Nullable
  public E deserializeSimple(@Nullable DataObject data) {
    if (data == null || data.getAsObject() == null) {
      return null;
    }
    return this.deserializeSimple(data.getAsString());
  }

  /**
   * Tries to deserialize the inputted string into an {@link Enum enum constant}. If no match is
   * found, returns {@code null}
   *
   * @param input input to deserialize
   * @return enum constant deserialized, or null
   * @since 3.1.0
   */
  @Nullable
  public E deserializeSimple(@Nullable String input) {
    if (input == null) {
      return null;
    }
    // try firstly to parse it using Enum#valueOf
    try {
      return Enum.valueOf(enumClass, input.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      // no enum constant, continue
    }

    List<String> parts = getParts(input);
    if (parts.isEmpty()) {
      return null;
    }
    // ofc try to combine them first using _ so if we got an input of "something_asd fp" to be
    // normalised and tried again
    StringBuilder enumFriendlyNormalizedInput = new StringBuilder();
    for (int i = 0; i < parts.size(); i++) {
      String part = parts.get(i);
      enumFriendlyNormalizedInput.append(part.toUpperCase(Locale.ROOT));
      if ((i + 1) < parts.size()) {
        enumFriendlyNormalizedInput.append('_');
      }
    }
    // try this input as the enum constant
    try {
      return Enum.valueOf(enumClass, enumFriendlyNormalizedInput.toString());
    } catch (IllegalArgumentException e) {
      // no enum constant, continue
    }

    // find all the constants with parts.size
    EnumSet<E> possibleConstants = getSearchThroughConstants(parts.size());
    if (possibleConstants.isEmpty()) {
      return null;
    }

    // try to make our lives easier
    if (possibleConstants.size() == 1) {
      return possibleConstants.iterator().next();
    }

    // last effort: findTheBestMatch
    return findTheBestMatch(possibleConstants, parts);
  }

  /**
   * Tries to find the most matching enum constant from the inputted values with the parts inputted.
   *
   * @param values the values to search from
   * @param parts the matches to search for in a constant parts
   * @return best match
   */
  @Nullable
  private E findTheBestMatch(EnumSet<E> values, List<String> parts) {
    for (E val : values) {
      String name = val.name();
      if (!containsChar(name, '_')) {
        // a one word constant can only match one part, exactly
        if (parts.size() == 1 && name.equalsIgnoreCase(parts.getFirst())) {
          return val;
        }
        continue;
      }
      List<String> words = new ArrayList<>(Arrays.asList(name.split("_")));
      // every word of the constant is matched by one part at most, so "true true" doesn't match
      // ACCEPT_TRUE
      int matches = 0;
      for (String part : parts) {
        for (int i = 0; i < words.size(); i++) {
          if (part.equalsIgnoreCase(words.get(i))) {
            words.remove(i);
            matches++;
            break;
          }
        }
      }
      if (matchesCondition.test(matches, parts)) {
        return val;
      }
    }
    // sorry but for sure this has been an invalid input
    return null;
  }

  /**
   * Gets the enum constants, parts of which equal the inputted length.
   *
   * @param length the length of the parts
   * @return enum set
   */
  private EnumSet<E> getSearchThroughConstants(int length) {
    EnumSet<E> ret = EnumSet.noneOf(enumClass);
    for (E val : enumClass.getEnumConstants()) {
      String name = val.name();
      if (containsChar(name, '_')) {
        String[] parts = name.split("_");
        if (parts.length == length) {
          ret.add(val);
        }
      } else {
        if (length == 1) {
          ret.add(val);
        }
      }
    }
    return ret;
  }

  /**
   * Tries to split the value to its base parts.
   *
   * @param val the value which needs splitting
   * @return parts list, or an empty list
   */
  private List<String> getParts(String val) {
    if (!containsChar(val, '_') && !containsChar(val, ' ')) {
      return Collections.emptyList();
    }
    List<String> parts = new ArrayList<>();
    // underscores and spaces in any mix; empty parts from repeated separators are skipped
    for (String part : val.split("[_ ]")) {
      if (!part.isEmpty()) {
        parts.add(part);
      }
    }
    return parts;
  }

  /** {@inheritDoc} */
  @Override
  public DataObject serialize(
      E value, SerializationContext<E> context, AnnotationAccessor annotations) {
    return this.serializeSimple(value);
  }

  /**
   * Serializes the specified {@link Enum enum constant} in a config user-friendly way, by replacing
   * underscores with spaces, and lowercase.
   *
   * @param value value to serialize
   * @return {@link DataObject data object}
   * @since 3.1.0
   */
  public DataObject serializeSimple(E value) {
    return new DataObject(value.name().toLowerCase(Locale.ROOT).replace("_", " "));
  }
}
