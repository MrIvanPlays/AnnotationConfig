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

package com.mrivanplays.annotationconfig.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a value whose type is chosen by the config: an interface, abstract class or sealed type
 * whose implementations are listed here. The section holding the value names its implementation
 * under the {@link #discriminator() discriminator} key:
 *
 * <pre>{@code
 * class Subject {
 *
 *   @Polymorphic({
 *     @Polymorphic.Subtype(name = "sequential", type = SequentialStrategy.class),
 *     @Polymorphic.Subtype(name = "parallel", type = ParallelStrategy.class)
 *   })
 *   ExecutionStrategy execution = new SequentialStrategy();
 * }
 * }</pre>
 *
 * <pre>{@code
 * execution:
 *   type: "parallel"
 *   threads: 8
 * }</pre>
 *
 * <p>Where it can be put:
 *
 * <ul>
 *   <li>On a field: the field's value. A config field or a field of a {@code @ConfigObject} section
 *       is a section itself, so {@link ConfigObject} is not needed. On a {@code List} or array
 *       field, the annotation applies to the elements.
 *   <li>On the declaration of the base type: every value of that type, in config fields, sections,
 *       lists, arrays, the fields of custom data types and record components. An annotation on a
 *       field wins over one on its type.
 * </ul>
 *
 * <p>On a sealed type, the implementations can be left out: the permitted subclasses are used,
 * named by {@link Name} or else by their simple class name.
 *
 * <pre>{@code
 * @Polymorphic(discriminator = "kind")
 * sealed interface Shape permits Circle, Square {}
 *
 * record Circle(double radius) implements Shape {} // named "Circle"
 *
 * @Polymorphic.Name("box")
 * record Square(int side) implements Shape {}
 *
 * class Subject {
 *
 *   List<Shape> shapes = List.of(new Circle(1.5), new Square(2));
 * }
 * }</pre>
 *
 * <pre>{@code
 * shapes:
 *   - kind: "Circle"
 *     radius: 1.5
 *   - kind: "box"
 *     side: 2
 * }</pre>
 *
 * <p>Reading:
 *
 * <ul>
 *   <li>The discriminator names the implementation. A value that is not a string, such as YAML's
 *       {@code on} (a boolean in YAML 1.1) or {@code 1}, is matched by its text; quote it to keep
 *       YAML from reading {@code yes}, {@code no}, {@code on} and {@code off} as booleans.
 *   <li>When the value the field holds is of that implementation, it is filled in place, as a
 *       {@code @ConfigObject} section is, so options missing from the file keep their values.
 *       Otherwise a new instance is created with its empty constructor, and options missing from
 *       the file keep the values that constructor gives them. A record is created with its
 *       canonical constructor, and its missing components are handled as for any record: with
 *       {@code USE_DEFAULT_VALUE} they are taken from the record the field holds if it is of the
 *       same implementation.
 *   <li>A section without the discriminator keeps the implementation of the value the field holds,
 *       and counts as a missing option, so {@code GENERATE_NEW_OPTIONS} writes it. Without such a
 *       value (in a list, for example) it is an error, as is an unknown name.
 * </ul>
 *
 * <p>Writing: the discriminator is written first in the section, followed by the fields of the
 * value's class. The value's class must be listed exactly: a subclass of a listed implementation,
 * including an anonymous one, is rejected.
 *
 * <p>The implementations must be assignable to the base type, concrete classes with an empty
 * constructor (a non-static inner class has none) or records, and none may have a field whose key
 * is the discriminator. The annotation is checked the first time it is used.
 *
 * <p>Not supported: the values of maps, which are read and written as they are for every custom
 * data type, and {@code .properties} files, which have no sections.
 *
 * @author Ivan Pekov
 * @since 3.1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Polymorphic {

  /**
   * The key that names the implementation in the section. It must not be blank, and must not hold a
   * dot, as dotted keys are nested sections in most formats.
   *
   * @return the discriminator key
   */
  String discriminator() default "type";

  /**
   * The implementations and their names. May be left empty when the base type is {@code sealed}:
   * its permitted subclasses are then used, those that are sealed interfaces or abstract classes
   * replaced by their own permitted subclasses, named by {@link Name} or else by their simple class
   * name. Simple names change when classes are renamed or obfuscated; use {@link Name} where that
   * can happen.
   *
   * @return the implementations
   */
  Subtype[] value() default {};

  /** One implementation and the name the discriminator holds for it. */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({})
  @interface Subtype {

    /**
     * The value of the {@link Polymorphic#discriminator() discriminator} for {@link #type()}. It
     * must not be blank, and must be unique among the implementations.
     *
     * @return the name
     */
    String name();

    /**
     * The implementation that {@link #name()} stands for.
     *
     * @return the implementation
     */
    Class<?> type();
  }

  /**
   * The name the {@link Polymorphic#discriminator() discriminator} holds for the annotated class,
   * when it is a permitted subclass of a sealed base type whose {@link Polymorphic} lists no
   * subtypes. Without it, the class's simple name is used.
   *
   * <pre>{@code
   * @Polymorphic
   * sealed interface Storage permits SqliteStorage, MysqlStorage {}
   *
   * @Polymorphic.Name("sqlite")
   * final class SqliteStorage implements Storage {
   *
   *   String file = "data.db";
   * }
   *
   * final class MysqlStorage implements Storage { // named "MysqlStorage"
   *
   *   String host = "localhost";
   * }
   * }</pre>
   *
   * <p>Use it where a class can be renamed or obfuscated: the simple name would change with it, and
   * existing files would no longer load. The name must not be blank, and must be unique among the
   * implementations of the base type, also those of nested sealed types, whose permitted subclasses
   * are named the same way.
   *
   * <p>It is ignored when {@link Polymorphic#value()} lists the subtypes, as {@link Subtype#name()}
   * names them there, and on a class that is not a permitted subclass of an annotated sealed type.
   *
   * @author Ivan Pekov
   * @since 3.1.0
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface Name {

    /**
     * The value of the {@link Polymorphic#discriminator() discriminator} for the annotated class.
     *
     * @return the name
     */
    String value();
  }
}
