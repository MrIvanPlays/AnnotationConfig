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

import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The implementations a {@link Polymorphic} annotation allows for a base type, checked once. An
 * invalid annotation fails when it is first used, not when the class that holds it is read, so it
 * doesn't break a field that is ignored or skipped.
 *
 * @hidden
 */
@NullMarked
public final class PolymorphicInfo {

  // the annotation on the declaration of a type; empty when there is none
  private static final ClassValue<Optional<PolymorphicInfo>> BY_TYPE =
      new ClassValue<>() {
        @Override
        protected Optional<PolymorphicInfo> computeValue(Class<?> type) {
          Polymorphic annotation = type.getDeclaredAnnotation(Polymorphic.class);
          return annotation == null
              ? Optional.empty()
              : Optional.of(new PolymorphicInfo(annotation, type));
        }
      };

  // annotations on fields, per base type; a ClassValue and not a static map, so that classes can
  // still be unloaded
  private static final ClassValue<Map<Polymorphic, PolymorphicInfo>> BY_FIELD =
      new ClassValue<>() {
        @Override
        protected Map<Polymorphic, PolymorphicInfo> computeValue(Class<?> type) {
          return new ConcurrentHashMap<>();
        }
      };

  private final Class<?> baseType;
  private final String discriminator;
  // in the order they are declared, for error messages
  private final Map<String, Class<?>> types;
  private final Map<Class<?>, String> names;

  private PolymorphicInfo(Polymorphic annotation, Class<?> baseType) {
    this.baseType = baseType;
    this.discriminator = annotation.discriminator();
    if (this.discriminator.isBlank()) {
      throw invalid("the discriminator must not be blank");
    }
    if (this.discriminator.indexOf('.') != -1) {
      throw invalid(
          "the discriminator '"
              + this.discriminator
              + "' must not hold a dot, as dotted keys are nested sections in most formats");
    }
    Map<String, Class<?>> types = new LinkedHashMap<>();
    Map<Class<?>, String> names = new HashMap<>();
    if (annotation.value().length == 0) {
      if (!baseType.isSealed()) {
        throw invalid(
            "it lists no subtypes, which only a sealed base type can leave out to use its"
                + " permitted subclasses");
      }
      this.addPermitted(baseType, types, names);
    } else {
      for (Polymorphic.Subtype subtype : annotation.value()) {
        this.add(subtype.name(), subtype.type(), types, names);
      }
    }
    this.types = types;
    this.names = names;
  }

  /**
   * The info of the {@link Polymorphic} on the declaration of {@code type}, {@code null} if it has
   * none.
   */
  public static @Nullable PolymorphicInfo ofType(Class<?> type) {
    return BY_TYPE.get(type).orElse(null);
  }

  /** The info of {@code annotation} for values of {@code baseType}. */
  public static PolymorphicInfo of(Polymorphic annotation, Class<?> baseType) {
    if (annotation.equals(baseType.getDeclaredAnnotation(Polymorphic.class))) {
      return BY_TYPE.get(baseType).orElseThrow();
    }
    // an exception thrown while computing is not stored, so an invalid annotation fails every time
    return BY_FIELD
        .get(baseType)
        .computeIfAbsent(annotation, a -> new PolymorphicInfo(a, baseType));
  }

  private IllegalArgumentException invalid(String reason) {
    return new IllegalArgumentException(
        "Invalid @Polymorphic for " + this.baseType.getName() + ": " + reason);
  }

  private void addPermitted(
      Class<?> sealed, Map<String, Class<?>> types, Map<Class<?>, String> names) {
    for (Class<?> permitted : sealed.getPermittedSubclasses()) {
      if (permitted.isInterface() || Modifier.isAbstract(permitted.getModifiers())) {
        if (!permitted.isSealed()) {
          throw invalid(
              "the permitted subclass "
                  + permitted.getName()
                  + " is abstract and not sealed, so its implementations are unknown; list the"
                  + " subtypes in @Polymorphic");
        }
        this.addPermitted(permitted, types, names);
        continue;
      }
      Polymorphic.Name name = permitted.getDeclaredAnnotation(Polymorphic.Name.class);
      this.add(name != null ? name.value() : permitted.getSimpleName(), permitted, types, names);
    }
  }

  private void add(
      String name, Class<?> type, Map<String, Class<?>> types, Map<Class<?>, String> names) {
    if (name.isBlank()) {
      throw invalid("the name of " + type.getName() + " is blank");
    }
    if (!this.baseType.isAssignableFrom(type)) {
      throw invalid(
          type.getName()
              + " is not a "
              + this.baseType.getName()
              + (Map.class.isAssignableFrom(this.baseType)
                  ? "; the values of maps are not supported"
                  : ""));
    }
    if (type.isInterface()
        || Modifier.isAbstract(type.getModifiers())
        || type.isEnum()
        || type.isArray()
        || type.isPrimitive()) {
      throw invalid(type.getName() + " is not a concrete class or a record");
    }
    if (!type.isRecord()) {
      try {
        type.getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        throw invalid(
            type.getName()
                + " has no empty constructor"
                + (type.isMemberClass() && !Modifier.isStatic(type.getModifiers())
                    ? ", as it is a non-static inner class"
                    : ""));
      }
    }
    this.checkDiscriminatorIsFree(type);
    Class<?> previous = types.putIfAbsent(name, type);
    if (previous != null) {
      throw invalid(
          "the name '"
              + name
              + "' is used for both "
              + previous.getName()
              + " and "
              + type.getName());
    }
    String previousName = names.putIfAbsent(type, name);
    if (previousName != null) {
      throw invalid(
          type.getName() + " is listed twice, as '" + previousName + "' and '" + name + "'");
    }
  }

  // a field with the discriminator's key would be written twice and never read
  private void checkDiscriminatorIsFree(Class<?> type) {
    List<FieldInfo> fields;
    if (type.isRecord()) {
      fields = ClassInfo.of(type).record().components();
    } else {
      fields = new ArrayList<>();
      // also the fields of parent classes, which FIND_PARENT_FIELDS reads
      for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
        fields.addAll(ClassInfo.of(c).fields());
      }
    }
    for (FieldInfo field : fields) {
      // static fields are only options with FIND_STATIC_FIELDS: a constant named so is no clash
      if (field.ignored() || field.rawConfig() || field.isStatic()) {
        continue;
      }
      if (field.key().equals(this.discriminator)) {
        throw invalid(
            "the field '"
                + field.name()
                + "' of "
                + field.field().getDeclaringClass().getName()
                + " has the key '"
                + this.discriminator
                + "', which is the discriminator");
      }
    }
  }

  /** The key that names the implementation. */
  public String discriminator() {
    return this.discriminator;
  }

  /**
   * The name written for a value of {@code type}.
   *
   * @param where the value's place in the config, for the error message
   * @throws IllegalArgumentException if {@code type} is not listed
   */
  public String nameOf(Class<?> type, Supplier<String> where) {
    String name = this.names.get(type);
    if (name == null) {
      throw new IllegalArgumentException(
          "Can't write "
              + where.get()
              + ": its class "
              + type.getName()
              + " is not listed in @Polymorphic for "
              + this.baseType.getName()
              + ", which lists "
              + this.listed());
    }
    return name;
  }

  /** Whether {@code section} has no value for the discriminator. */
  public boolean lacksDiscriminator(Map<?, ?> section) {
    return section.get(this.discriminator) == null;
  }

  /**
   * The implementation {@code section} names under the discriminator; without a discriminator, the
   * class of {@code current} if it is listed.
   *
   * @param current the value the field holds, if any
   * @param where the section's place in the config, for the error message
   * @throws IllegalArgumentException if the discriminator is unknown, or missing without a listed
   *     {@code current}
   */
  public Class<?> typeOf(Map<?, ?> section, @Nullable Object current, Supplier<String> where) {
    Object value = section.get(this.discriminator);
    if (value == null) {
      if (current != null && this.names.containsKey(current.getClass())) {
        return current.getClass();
      }
      throw new IllegalArgumentException(
          "Can't read "
              + where.get()
              + ": it has no '"
              + this.discriminator
              + "' key naming its type, one of "
              + this.listed());
    }
    String name;
    if (value instanceof String string) {
      name = string;
    } else if (value instanceof Number || value instanceof Boolean || value instanceof Character) {
      name = String.valueOf(value);
    } else {
      throw new IllegalArgumentException(
          "Can't read "
              + where.get()
              + ": its '"
              + this.discriminator
              + "' must name its type, one of "
              + this.listed()
              + ", but is "
              + value);
    }
    Class<?> type = this.types.get(name);
    if (type == null) {
      throw new IllegalArgumentException(
          "Can't read "
              + where.get()
              + ": unknown "
              + this.discriminator
              + " '"
              + name
              + "', expected one of "
              + this.listed()
              + (value instanceof String
                  ? ""
                  : " (it was read as a " + value.getClass().getSimpleName() + "; quote it)"));
    }
    return type;
  }

  private String listed() {
    return this.types.keySet().toString();
  }
}
