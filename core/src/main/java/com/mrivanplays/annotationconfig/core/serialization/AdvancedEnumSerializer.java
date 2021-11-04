package com.mrivanplays.annotationconfig.core.serialization;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@link Enum} serializer, which uses different tricks to serialize enum constants to user-friendly
 * values for configs, and also to read them properly.
 *
 * <p>Lets be real, every developer tried or is going to try to serialize enum constants, so they
 * are more user-friendly to be configured. Yet everyone who tried wasn't really able to go farther
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
 * <pre>
 *   <code>
 *     public enum Option {
 *       ACCEPT_TRUE,
 *       ACCEPT_FALSE,
 *       ACCEPT_BOTH,
 *       DO_NOT_ACCEPT_TRUE,
 *       DO_NOT_ACCEPT_FALSE,
 *       DO_NOT_ACCEPT_BOTH
 *     }
 *   </code>
 * </pre>
 *
 * and you want to use AdvancedEnumSerializer to serialize it, you just register it using the {@link
 * SerializerRegistry}. An example of this is:
 *
 * <pre>
 *   <code>
 *     SerializerRegistry.INSTANCE.registerSerializer(Option.class, AdvancedEnumSerializer.forEnum(Option.class));
 *   </code>
 * </pre>
 *
 * @since 2.0.0
 * @author MrIvanPlays
 * @param <E> enum class
 */
public final class AdvancedEnumSerializer<E extends Enum<E>> implements FieldTypeSerializer<E> {

  /**
   * Creates a new {@link AdvancedEnumSerializer} for the specified {@code enumClass}
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

  private final Class<E> enumClass;

  private AdvancedEnumSerializer(Class<E> enumClass) {
    if (enumClass == null) {
      throw new NullPointerException("enumClass");
    }
    if (!enumClass.isEnum()) {
      throw new IllegalArgumentException("Not an enum");
    }
    this.enumClass = enumClass;
  }

  /** {@inheritDoc} */
  @Override
  public E deserialize(DataObject data, Field field) {
    String input = data.getAsString();
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
    // normalized and tried again
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
      return possibleConstants.stream().findFirst().orElse(null);
    }

    // last effort: findTheBestMatch
    return findTheBestMatch(possibleConstants, parts);
  }

  /**
   * Tries to find the most matching enum constant from the inputted values with the parts inputted.
   *
   * @param values the values to search from
   * @param parts the matches to search for in an constant parts
   * @return best match
   */
  private E findTheBestMatch(EnumSet<E> values, List<String> parts) {
    Map<E, List<String>> partsMap = new HashMap<>();
    for (E val : values) {
      // this filters out even more the possible values
      String name = val.name();
      List<String> list = null;
      if (containsChar(name, '_')) {
        list = Arrays.asList(name.split("_"));
      } else {
        // skip entirely the map if parts.size() is 1
        if (parts.size() == 1) {
          if (name.equalsIgnoreCase(parts.get(0))) {
            return val;
          }
        }
      }
      if (list == null) {
        continue;
      }
      partsMap.put(val, list);
    }

    for (Map.Entry<E, List<String>> entry : partsMap.entrySet()) {
      int matches = 0;
      for (String part : parts) {
        for (String entryPart : entry.getValue()) {
          if (part.equalsIgnoreCase(entryPart)) {
            matches++;
          }
        }
      }
      if (matches == parts.size()) {
        return entry.getKey();
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
    List<String> parts = new ArrayList<>();
    Character splitChar = null;
    if (containsChar(val, '_')) {
      splitChar = '_';
    } else if (containsChar(val, ' ')) {
      splitChar = ' ';
    }
    if (splitChar == null) {
      return Collections.emptyList();
    }
    String[] split = val.split(Character.toString(splitChar));
    for (String part : split) {
      if (containsChar(part, '_') || containsChar(part, ' ')) {
        parts.addAll(getParts(part));
        continue;
      }
      parts.add(part);
    }
    return parts;
  }

  private boolean containsChar(String val, char c) {
    return val.indexOf(c) != -1;
  }

  /** {@inheritDoc} */
  @Override
  public DataObject serialize(E value, Field field) {
    return new DataObject(value.name().toLowerCase(Locale.ROOT).replace("_", " "));
  }
}
