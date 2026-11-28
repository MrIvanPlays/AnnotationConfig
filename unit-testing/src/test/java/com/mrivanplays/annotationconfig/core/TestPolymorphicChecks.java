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

package com.mrivanplays.annotationconfig.core;

import com.mrivanplays.annotationconfig.core.annotations.Ignore;
import com.mrivanplays.annotationconfig.core.annotations.Key;
import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import com.mrivanplays.annotationconfig.core.resolver.ConfigResolver;
import java.io.StringWriter;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The checks of {@code @Polymorphic} annotations, and where their errors are reported. */
class TestPolymorphicChecks {

  private static final ConfigResolver RESOLVER = PropertyConfig.getConfigResolver();

  private static String dump(Object config) {
    StringWriter writer = new StringWriter();
    RESOLVER.dump(config, writer);
    return writer.toString();
  }

  private static String rejected(Object config) {
    return Assertions.assertThrows(IllegalArgumentException.class, () -> dump(config)).getMessage();
  }

  private static void assertRejected(Object config, String... parts) {
    String message = rejected(config);
    for (String part : parts) {
      Assertions.assertTrue(message.contains(part), message);
    }
  }

  @Test
  void theErrorNamesTheField() {
    assertRejected(new BlankDiscriminator(), "Field 'value' of ", "BlankDiscriminator");
  }

  @Test
  void theDiscriminatorMustNotBeBlankOrDotted() {
    assertRejected(new BlankDiscriminator(), "the discriminator must not be blank");
    assertRejected(new DottedDiscriminator(), "'meta.type' must not hold a dot");
  }

  @Test
  void anImplementationMustBeASubtype() {
    assertRejected(new NotASubtype(), Other.class.getName() + " is not a " + Base.class.getName());
  }

  @Test
  void anImplementationMustBeConcrete() {
    assertRejected(new AbstractImplementation(), "is not a concrete class or a record");
  }

  @Test
  void anImplementationNeedsAnEmptyConstructor() {
    assertRejected(new InnerImplementation(), "has no empty constructor", "non-static inner class");
    assertRejected(new NoEmptyConstructor(), "has no empty constructor");
  }

  @Test
  void namesAndImplementationsAreUnique() {
    assertRejected(new DuplicateName(), "the name 'a' is used for both");
    assertRejected(new DuplicateType(), "is listed twice, as 'a' and 'b'");
  }

  @Test
  void theNameMustNotBeBlank() {
    assertRejected(new BlankName(), "is blank");
  }

  @Test
  void noFieldMayHaveTheDiscriminatorsKey() {
    assertRejected(
        new KeyClash(), "the field 'type'", "has the key 'kind', which is the discriminator");
    // also a field inherited from a parent class
    assertRejected(new InheritedKeyClash(), "has the key 'kind'");
  }

  @Test
  void onlyASealedTypeMayListNoSubtypes() {
    assertRejected(new NoSubtypes(), "lists no subtypes");
    assertRejected(new UnsealedPermitted(), "is abstract and not sealed");
  }

  @Test
  void theValuesOfMapsAreNotSupported() {
    assertRejected(new OnAMap(), "the values of maps are not supported");
  }

  @Test
  void anInvalidAnnotationOnAnIgnoredFieldIsNotChecked() {
    Assertions.assertEquals("value=1\n", dump(new IgnoredInvalid()));
  }

  @Test
  void anAnnotationOnTheTypeMakesAFieldASection() {
    // .properties has no sections, so the values are loaded from a map
    TypeAnnotated config = new TypeAnnotated();
    A held = (A) config.value;
    RESOLVER.load(config, Map.of("value", Map.of("type", "a", "n", 5)));
    Assertions.assertSame(held, config.value);
    Assertions.assertEquals(5, held.n);
  }

  // <editor-fold desc="subjects">

  interface Base {}

  @Polymorphic({@Polymorphic.Subtype(name = "a", type = A.class)})
  interface Annotated {}

  sealed interface Sealed permits Unsealed {}

  abstract static non-sealed class Unsealed implements Sealed {}

  static class Concrete extends Unsealed {}

  static class BlankDiscriminator {

    @Polymorphic(
        discriminator = " ",
        value = {@Polymorphic.Subtype(name = "a", type = A.class)})
    Base value = new A();
  }

  static class DottedDiscriminator {

    @Polymorphic(
        discriminator = "meta.type",
        value = {@Polymorphic.Subtype(name = "a", type = A.class)})
    Base value = new A();
  }

  static class NotASubtype {

    @Polymorphic({@Polymorphic.Subtype(name = "a", type = Other.class)})
    Base value = new A();
  }

  static class AbstractImplementation {

    @Polymorphic({@Polymorphic.Subtype(name = "a", type = AbstractA.class)})
    Base value = new A();
  }

  static class InnerImplementation {

    @Polymorphic({@Polymorphic.Subtype(name = "a", type = Inner.class)})
    Base value = new A();

    class Inner implements Base {}
  }

  static class NoEmptyConstructor {

    @Polymorphic({@Polymorphic.Subtype(name = "a", type = WithArgument.class)})
    Base value = new A();
  }

  static class DuplicateName {

    @Polymorphic({
      @Polymorphic.Subtype(name = "a", type = A.class),
      @Polymorphic.Subtype(name = "a", type = B.class)
    })
    Base value = new A();
  }

  static class DuplicateType {

    @Polymorphic({
      @Polymorphic.Subtype(name = "a", type = A.class),
      @Polymorphic.Subtype(name = "b", type = A.class)
    })
    Base value = new A();
  }

  static class BlankName {

    @Polymorphic({@Polymorphic.Subtype(name = "", type = A.class)})
    Base value = new A();
  }

  static class KeyClash {

    @Polymorphic(
        discriminator = "kind",
        value = {@Polymorphic.Subtype(name = "a", type = WithKind.class)})
    Base value = new WithKind();
  }

  static class InheritedKeyClash {

    @Polymorphic(
        discriminator = "kind",
        value = {@Polymorphic.Subtype(name = "a", type = ChildOfWithKind.class)})
    Base value = new ChildOfWithKind();
  }

  static class NoSubtypes {

    @Polymorphic Base value = new A();
  }

  static class UnsealedPermitted {

    @Polymorphic Sealed value = new Concrete();
  }

  static class OnAMap {

    @Polymorphic({@Polymorphic.Subtype(name = "a", type = A.class)})
    Map<String, Base> value = Map.of();
  }

  static class IgnoredInvalid {

    @Ignore @Polymorphic Base ignored = new A();

    int value = 1;
  }

  static class TypeAnnotated {

    Annotated value = new A();
  }

  static class A implements Base, Annotated {

    int n = 1;
  }

  static class B implements Base {}

  static class Other {}

  abstract static class AbstractA implements Base {}

  static class WithArgument implements Base {

    WithArgument(int n) {}
  }

  static class WithKind implements Base {

    @Key("kind")
    String type = "x";
  }

  static class ChildOfWithKind extends WithKind {}

  // </editor-fold>
}
