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

import com.mrivanplays.annotationconfig.core.annotations.ConfigVersion;
import com.mrivanplays.annotationconfig.core.annotations.Max;
import com.mrivanplays.annotationconfig.core.annotations.Min;
import com.mrivanplays.annotationconfig.core.annotations.OneOf;
import com.mrivanplays.annotationconfig.core.annotations.Polymorphic;
import com.mrivanplays.annotationconfig.core.resolver.MultilineString;
import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.resolver.settings.ACDefaultSettings;
import com.mrivanplays.annotationconfig.core.resolver.settings.Settings;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import com.mrivanplays.annotationconfig.core.serialization.SerializerRegistry;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Generates the JSON Schema (draft-07) of the files a config is written to, from the same holders
 * and settings a dump uses. The defaults are the values a dump writes.
 *
 * <p>Config fields and sections are described from their holders, as {@code
 * AnnotatedConfigResolver} writes them (dotted keys boxed by the key resolver); the values of
 * fields, list elements, records and custom data types from their types, as {@code
 * DefaultSerializer} writes them.
 *
 * @hidden
 */
@NullMarked
final class SchemaGenerator {

  static final String DRAFT = "http://json-schema.org/draft-07/schema#";

  // what a key resolver boxes in place of a schema, to find where it puts the key
  private static final Object SLOT = new Object();

  private final Settings settings;
  private final KeyResolver keyResolver;
  private final boolean findStaticFields;
  // the custom data types being described, so that a type that holds itself ends
  private final Set<Class<?>> describing = new HashSet<>();

  private SchemaGenerator(Settings settings) {
    this.settings = settings;
    this.keyResolver = settings.get(ACDefaultSettings.KEY_RESOLVER).orElseThrow();
    this.findStaticFields = settings.get(ACDefaultSettings.FIND_STATIC_FIELDS).orElseThrow();
  }

  /** The schema of {@code config}, whose fields are {@code holders}. */
  static Map<String, Object> generate(
      Object config, Set<AnnotationHolder> holders, Settings settings) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("$schema", DRAFT);
    describe(schema, classComments(holders));
    schema.put("type", "object");
    Map<String, Object> properties = new LinkedHashMap<>();
    ConfigVersion version = ConfigMigrator.versionOf(config.getClass());
    if (version != null) {
      Map<String, Object> node = new LinkedHashMap<>();
      describe(node, FieldInfo.applyPlaceholders(List.of(version.comments())));
      node.put("type", "integer");
      node.put("default", version.value());
      properties.put(version.key(), node);
    }
    Map<String, Object> values = AnnotatedConfigResolver.values(config, holders, settings);
    new SchemaGenerator(settings).putFields(config, holders, values, properties);
    schema.put("properties", properties);
    return schema;
  }

  // <editor-fold desc="Config fields and sections" defaultstate="collapsed">

  /** The value of {@code info} in {@code instance}. */
  private static @Nullable Object fieldValue(FieldInfo info, Object instance) {
    try {
      return info.ensureAccessible().get(instance);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("A field became inaccessible", e);
    }
  }

  /** The comments of the classes among {@code holders}, as a dump writes them. */
  private static List<String> classComments(Set<AnnotationHolder> holders) {
    List<String> comments = new ArrayList<>();
    for (AnnotationHolder holder : holders) {
      if (holder.isClass()) {
        comments.addAll(ClassInfo.of(holder.getClazz()).comments());
      }
    }
    return comments;
  }

  @SuppressWarnings("unchecked")
  private static void place(
      Map<String, Object> properties, Map<String, Object> boxed, Map<String, Object> node) {
    for (Map.Entry<String, Object> entry : boxed.entrySet()) {
      if (entry.getValue() instanceof Map<?, ?> nested) {
        // a dotted key: the parts before the last are sections
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("type", "object");
        section.put("properties", new LinkedHashMap<String, Object>());
        section = (Map<String, Object>) merge(properties, entry.getKey(), section);
        place((Map<String, Object>) section.get("properties"), (Map<String, Object>) nested, node);
      } else {
        merge(properties, entry.getKey(), node);
      }
    }
  }

  /**
   * Puts {@code node} under {@code key}; when both it and the schema there have properties, as a
   * section and a dotted key into it do, they are merged into the one there.
   *
   * @return the schema under {@code key}
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> merge(
      Map<String, Object> properties, String key, Map<String, Object> node) {
    Object existing = properties.get(key);
    if (existing instanceof Map<?, ?> map
        && map.get("properties") instanceof Map<?, ?> existingProperties
        && node.get("properties") instanceof Map<?, ?> nodeProperties) {
      ((Map<String, Object>) existingProperties).putAll((Map<String, Object>) nodeProperties);
      return (Map<String, Object>) map;
    }
    properties.put(key, node);
    return node;
  }

  /** {@code @OneOf} on a String: its values, which only suggest when case is ignored. */
  private static void oneOf(Map<String, Object> node, AnnotatedType annotated) {
    OneOf oneOf = annotated.getAnnotation(OneOf.class);
    if (oneOf == null || oneOf.value().length == 0) {
      // without values loading fails, whatever the schema says
      return;
    }
    List<Object> values = new ArrayList<>(List.of(oneOf.value()));
    node.put(oneOf.ignoreCase() ? "examples" : "enum", values);
  }

  /** {@code @Min} and {@code @Max}: the bounds of a number, or of a String's length. */
  private static void bounds(Map<String, Object> node, AnnotatedType annotated, boolean length) {
    String where = annotated.getType().getTypeName();
    Min min = annotated.getAnnotation(Min.class);
    if (min != null) {
      Number bound = MinMaxValidator.minBound(where, min);
      if (length) {
        long minLength = (long) Math.ceil(bound.doubleValue());
        if (minLength > 0) {
          node.put("minLength", minLength);
        }
      } else {
        node.put("minimum", bound);
      }
    }
    Max max = annotated.getAnnotation(Max.class);
    if (max != null) {
      Number bound = MinMaxValidator.maxBound(where, max);
      if (length) {
        // a negative maximum length accepts no String; JSON Schema doesn't allow one
        node.put("maxLength", Math.max(0, (long) Math.floor(bound.doubleValue())));
      } else {
        node.put("maximum", bound);
      }
    }
  }

  private static void describe(Map<String, Object> node, List<String> comments) {
    StringBuilder description = new StringBuilder();
    for (String comment : comments) {
      if (!description.isEmpty()) {
        description.append('\n');
      }
      description.append(comment.strip());
    }
    String text = description.toString().strip();
    if (!text.isEmpty()) {
      node.put("description", text);
    }
  }

  private static Map<String, Object> withDefault(
      Map<String, Object> node, @Nullable Object written) {
    Object value = json(written);
    if (value != null) {
      node.put("default", value);
    }
    return node;
  }

  // </editor-fold>

  // <editor-fold desc="Values" defaultstate="collapsed">

  /** {@code written} as a JSON value; {@code null} for what JSON can't hold. */
  private static @Nullable Object json(@Nullable Object written) {
    return switch (written) {
      case null -> null;
      case String string -> string;
      case Boolean bool -> bool;
      case MultilineString multiline -> multiline.getString();
      case Character character -> character.toString();
      case Double d when !Double.isFinite(d) -> null;
      case Float f when !Float.isFinite(f) -> null;
      case Number number -> number;
      case Map<?, ?> map -> {
        Map<String, Object> converted = new LinkedHashMap<>();
        map.forEach((key, value) -> converted.put(String.valueOf(key), json(value)));
        yield converted;
      }
      case Collection<?> collection -> {
        List<Object> converted = new ArrayList<>(collection.size());
        collection.forEach(element -> converted.add(json(element)));
        yield converted;
      }
      default -> {
        if (written.getClass().isArray()) {
          int length = Array.getLength(written);
          List<Object> converted = new ArrayList<>(length);
          for (int i = 0; i < length; i++) {
            converted.add(json(Array.get(written, i)));
          }
          yield converted;
        }
        // such as a java.time value a registered serializer writes
        yield written.toString();
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static @Nullable Map<String, Object> asMap(@Nullable Object value) {
    return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
  }

  private static boolean isInteger(Class<?> type) {
    return type == int.class
        || type == Integer.class
        || type == long.class
        || type == Long.class
        || type == short.class
        || type == Short.class
        || type == byte.class
        || type == Byte.class
        || type == BigInteger.class;
  }

  private static boolean isNumber(Class<?> type) {
    return type == double.class
        || type == Double.class
        || type == float.class
        || type == Float.class
        || type == BigDecimal.class
        || type == Number.class;
  }

  private static Class<?> rawClass(Type type) {
    return switch (type) {
      case Class<?> c -> c;
      case ParameterizedType parameterized -> rawClass(parameterized.getRawType());
      case GenericArrayType array ->
          Array.newInstance(rawClass(array.getGenericComponentType()), 0).getClass();
      case WildcardType wildcard ->
          wildcard.getUpperBounds().length == 0
              ? Object.class
              : rawClass(wildcard.getUpperBounds()[0]);
      case TypeVariable<?> variable ->
          variable.getBounds().length == 0 ? Object.class : rawClass(variable.getBounds()[0]);
      default -> Object.class;
    };
  }

  /**
   * Puts the schemas of the fields {@code holders} of a config or section into {@code properties}.
   *
   * @param instance the config or section, if there is one: it tells the classes of sections
   * @param values what a dump writes for it, for the defaults
   */
  private void putFields(
      @Nullable Object instance,
      Set<AnnotationHolder> holders,
      @Nullable Map<String, Object> values,
      Map<String, Object> properties) {
    for (AnnotationHolder holder : holders) {
      if (holder.isClass()) {
        continue;
      }
      FieldInfo info = holder.getInfo();
      if (info.rawConfig()) {
        continue;
      }
      String key = info.key();
      if (!info.configObject()) {
        Object written = values == null ? null : this.keyResolver.unbox(key, values);
        Map<String, Object> node = new LinkedHashMap<>();
        describe(node, info.comments());
        node.putAll(this.fieldSchema(info, written));
        this.box(properties, key, node);
        continue;
      }
      // a section is put under its key as it is, as a dump does
      Object section = instance == null ? null : fieldValue(info, instance);
      Map<String, Object> sectionValues = asMap(values == null ? null : values.get(key));
      PolymorphicInfo polymorphic = info.polymorphic();
      Map<String, Object> node = new LinkedHashMap<>();
      List<String> comments = info.comments();
      Class<?> sectionType = section != null ? section.getClass() : info.type();
      if (polymorphic != null) {
        describe(node, comments);
        node.putAll(this.polymorphicSchema(polymorphic, sectionType, sectionValues, true));
      } else {
        Set<AnnotationHolder> sectionHolders = this.holders(sectionType);
        describe(node, comments.isEmpty() ? classComments(sectionHolders) : comments);
        node.putAll(this.sectionSchema(section, sectionHolders, sectionValues));
      }
      merge(properties, key, node);
    }
  }

  private Set<AnnotationHolder> holders(Class<?> type) {
    return ClassInfo.of(type)
        .holders(
            this.settings.get(ACDefaultSettings.FIND_PARENT_FIELDS).orElseThrow(),
            this.settings.get(ACDefaultSettings.SHOULD_REVERSE_FIELDS).orElseThrow(),
            this.findStaticFields);
  }

  // </editor-fold>

  // <editor-fold desc="Annotations" defaultstate="collapsed">

  /** The schema of a section whose fields are {@code holders}, as a dump writes it. */
  private Map<String, Object> sectionSchema(
      @Nullable Object instance,
      Set<AnnotationHolder> holders,
      @Nullable Map<String, Object> values) {
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("type", "object");
    Map<String, Object> properties = new LinkedHashMap<>();
    this.putFields(instance, holders, values, properties);
    node.put("properties", properties);
    return node;
  }

  /** Puts {@code node} where the key resolver puts the value of {@code key}. */
  private void box(Map<String, Object> properties, String key, Map<String, Object> node) {
    Map<String, Object> boxed = new LinkedHashMap<>();
    this.keyResolver.boxTo(key, SLOT, boxed);
    place(properties, boxed, node);
  }

  // </editor-fold>

  // <editor-fold desc="Helpers" defaultstate="collapsed">

  /** The schema of the value of a field that isn't a section, with its default. */
  private Map<String, Object> fieldSchema(FieldInfo info, @Nullable Object written) {
    // on a list or array field, @Polymorphic is the elements'
    Polymorphic elements =
        info.type().isArray() || List.class.isAssignableFrom(info.type())
            ? info.field().getDeclaredAnnotation(Polymorphic.class)
            : null;
    return this.valueSchema(info.field().getAnnotatedType(), written, elements);
  }

  /**
   * The schema of a value of {@code annotated}, as the default serializer writes it.
   *
   * @param written what a dump writes for it, for the default
   * @param elementPolymorphic the {@code @Polymorphic} of a list or array field, for its elements
   */
  private Map<String, Object> valueSchema(
      AnnotatedType annotated, @Nullable Object written, @Nullable Polymorphic elementPolymorphic) {
    if (annotated instanceof AnnotatedWildcardType wildcard) {
      AnnotatedType[] bounds = wildcard.getAnnotatedUpperBounds();
      return bounds.length == 0
          ? withDefault(new LinkedHashMap<>(), written)
          : this.valueSchema(bounds[0], written, elementPolymorphic);
    }
    Type type = annotated.getType();
    Class<?> raw = rawClass(type);
    Map<String, Object> node = new LinkedHashMap<>();
    SerializerRegistry registry = SerializerRegistry.INSTANCE;
    if (registry.getSerializer(type).isPresent() || registry.getSerializer(raw).isPresent()) {
      // only the serializer knows what it writes
      return withDefault(node, written);
    }
    if (raw == String.class || raw == char.class || raw == Character.class) {
      node.put("type", "string");
      if (raw == String.class) {
        oneOf(node, annotated);
        bounds(node, annotated, true);
      }
      return withDefault(node, written);
    }
    if (raw == boolean.class || raw == Boolean.class) {
      node.put("type", "boolean");
      return withDefault(node, written);
    }
    if (isInteger(raw)) {
      node.put("type", "integer");
      bounds(node, annotated, false);
      return withDefault(node, written);
    }
    if (isNumber(raw)) {
      node.put("type", "number");
      bounds(node, annotated, false);
      return withDefault(node, written);
    }
    if (raw.isEnum()) {
      node.put("type", "string");
      List<Object> names = new ArrayList<>();
      for (Object constant : raw.getEnumConstants()) {
        // as they are written; they are read ignoring case
        names.add(((Enum<?>) constant).name().toLowerCase(Locale.ROOT));
      }
      node.put("enum", names);
      return withDefault(node, written);
    }
    if (annotated instanceof AnnotatedArrayType array) {
      node.put("type", "array");
      node.put(
          "items",
          this.elementSchema(array.getAnnotatedGenericComponentType(), elementPolymorphic));
      return withDefault(node, written);
    }
    if (Collection.class.isAssignableFrom(raw)) {
      node.put("type", "array");
      node.put(
          "items",
          annotated instanceof AnnotatedParameterizedType parameterized
              ? this.elementSchema(
                  parameterized.getAnnotatedActualTypeArguments()[0], elementPolymorphic)
              : new LinkedHashMap<>());
      return withDefault(node, written);
    }
    if (Map.class.isAssignableFrom(raw)) {
      node.put("type", "object");
      if (annotated instanceof AnnotatedParameterizedType parameterized) {
        AnnotatedType[] arguments = parameterized.getAnnotatedActualTypeArguments();
        Map<String, Object> keys = this.valueSchema(arguments[0], null, null);
        if (rawClass(arguments[0].getType()) == String.class && keys.size() > 1) {
          // a String key with @OneOf or a length
          node.put("propertyNames", keys);
        }
        node.put("additionalProperties", this.valueSchema(arguments[1], null, null));
      }
      return withDefault(node, written);
    }
    PolymorphicInfo polymorphic = PolymorphicInfo.ofType(raw);
    if (polymorphic != null) {
      return this.polymorphicSchema(polymorphic, null, asMap(written), false);
    }
    if (raw == Object.class
        || raw == DataObject.class
        || raw.isInterface()
        || Modifier.isAbstract(raw.getModifiers())) {
      // anything
      return withDefault(node, written);
    }
    return this.dataSchema(raw, written);
  }

  /** The schema of the elements of a list or array. */
  private Map<String, Object> elementSchema(
      AnnotatedType element, @Nullable Polymorphic polymorphic) {
    if (polymorphic != null) {
      return this.polymorphicSchema(
          PolymorphicInfo.of(polymorphic, rawClass(element.getType())), null, null, false);
    }
    return this.valueSchema(element, null, null);
  }

  /** The schema of a record or custom data type, as the default serializer writes it. */
  private Map<String, Object> dataSchema(Class<?> type, @Nullable Object written) {
    if (!this.describing.add(type)) {
      // a type that holds itself; described once
      return new LinkedHashMap<>();
    }
    try {
      List<FieldInfo> fields = this.dataFields(type);
      if (fields.size() == 1) {
        // written as its one field's value, a record's too
        FieldInfo only = fields.getFirst();
        return only.ignored() ? new LinkedHashMap<>() : this.fieldSchema(only, written);
      }
      Map<String, Object> node = new LinkedHashMap<>();
      node.put("type", "object");
      node.put("properties", this.dataProperties(fields, asMap(written)));
      return node;
    } finally {
      this.describing.remove(type);
    }
  }

  private List<FieldInfo> dataFields(Class<?> type) {
    // record components are never static
    return type.isRecord()
        ? ClassInfo.of(type).record().components()
        : ClassInfo.of(type).dataFields(this.findStaticFields);
  }

  /** The schemas of {@code fields} of a record or custom data type, by their keys. */
  private Map<String, Object> dataProperties(
      List<FieldInfo> fields, @Nullable Map<String, Object> values) {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (FieldInfo info : fields) {
      if (info.ignored() || info.rawConfig()) {
        continue;
      }
      Map<String, Object> node = new LinkedHashMap<>();
      describe(node, info.comments());
      node.putAll(this.fieldSchema(info, values == null ? null : values.get(info.key())));
      properties.put(info.key(), node);
    }
    return properties;
  }

  /**
   * The schema of a {@code @Polymorphic} value: any of its implementations, chosen by the
   * discriminator. Only the implementation {@code values} names (or {@code current}) gets the
   * defaults.
   *
   * @param current the class of the value a section holds, if any
   * @param section whether it is a config section, written as sections are, or a value written by
   *     the default serializer
   */
  private Map<String, Object> polymorphicSchema(
      PolymorphicInfo polymorphic,
      @Nullable Class<?> current,
      @Nullable Map<String, Object> values,
      boolean section) {
    String discriminator = polymorphic.discriminator();
    Map<String, Class<?>> types = polymorphic.types();
    Class<?> chosen = current;
    if (values != null && values.get(discriminator) != null) {
      chosen = types.get(String.valueOf(values.get(discriminator)));
    }
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("type", "object");
    Map<String, Object> names = new LinkedHashMap<>();
    names.put("type", "string");
    names.put("enum", new ArrayList<Object>(types.keySet()));
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(discriminator, names);
    node.put("properties", properties);
    // anyOf and not oneOf: without the discriminator, which a section can leave out, every
    // implementation matches
    List<Object> anyOf = new ArrayList<>();
    for (Map.Entry<String, Class<?>> entry : types.entrySet()) {
      Class<?> type = entry.getValue();
      Map<String, Object> branchValues = type == chosen ? values : null;
      Map<String, Object> branch = new LinkedHashMap<>();
      Map<String, Object> branchProperties = new LinkedHashMap<>();
      Map<String, Object> name = new LinkedHashMap<>();
      name.put("const", entry.getKey());
      branchProperties.put(discriminator, name);
      if (section && !type.isRecord()) {
        Set<AnnotationHolder> holders = this.holders(type);
        describe(branch, classComments(holders));
        this.putFields(null, holders, branchValues, branchProperties);
      } else if (this.describing.add(type)) {
        describe(branch, ClassInfo.of(type).comments());
        try {
          branchProperties.putAll(this.dataProperties(this.dataFields(type), branchValues));
        } finally {
          this.describing.remove(type);
        }
      }
      branch.put("properties", branchProperties);
      anyOf.add(branch);
    }
    node.put("anyOf", anyOf);
    return node;
  }

  // </editor-fold>
}
