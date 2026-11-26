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
import com.mrivanplays.annotationconfig.core.serialization.AnnotationAccessor;
import com.mrivanplays.annotationconfig.core.utils.AnnotationUtils;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * What AnnotationConfig needs to know about a class, read once per class instead of on every dump
 * and load. Classes don't change at runtime, so nothing here is ever invalidated; the serializers
 * that are registered can change, so they are looked up on every use and not cached here.
 *
 * @hidden
 */
@NullMarked
public final class ClassInfo {

  private static final ClassValue<ClassInfo> CACHE =
      new ClassValue<>() {
        @Override
        protected ClassInfo computeValue(Class<?> type) {
          return new ClassInfo(type);
        }
      };
  private final Class<?> type;
  private final List<FieldInfo> fields;
  private final List<String> comments;
  private final boolean hasDeclaredAnnotations;
  private final boolean hasAnnotations;
  // the fields without the static ones
  private final List<FieldInfo> instanceFields;
  // one set per combination of the three settings resolveAnnotationHolders depends on
  private final AtomicReferenceArray<@Nullable Set<AnnotationHolder>> holders =
      new AtomicReferenceArray<>(8);
  @Nullable private volatile RecordInfo record;
  // the @ConfigVersion of the class, also one it inherits, and the class that declares it
  @Nullable private final ConfigVersion version;
  @Nullable private final Class<?> versionClass;

  private ClassInfo(Class<?> type) {
    this.type = type;
    Field[] declared = type.getDeclaredFields();
    List<FieldInfo> fields = new ArrayList<>(declared.length);
    List<FieldInfo> instanceFields = new ArrayList<>(declared.length);
    for (Field field : declared) {
      // synthetic fields, such as the outer instance of an inner class ("this$0") or the flag of
      // assert statements ("$assertionsDisabled"), are added by the compiler and no config data
      if (field.isSynthetic()) {
        continue;
      }
      FieldInfo info = new FieldInfo(field);
      fields.add(info);
      if (!info.isStatic()) {
        instanceFields.add(info);
      }
    }
    this.fields = List.copyOf(fields);
    this.instanceFields = List.copyOf(instanceFields);
    this.comments =
        List.copyOf(AnnotationUtils.getComments(AnnotationAccessor.createFromClass(type), false));
    this.hasDeclaredAnnotations = type.getDeclaredAnnotations().length > 0;
    this.hasAnnotations = type.getAnnotations().length > 0;
    this.version = type.getAnnotation(ConfigVersion.class);
    Class<?> versionClass = null;
    if (this.version != null) {
      versionClass = type;
      while (versionClass.getDeclaredAnnotation(ConfigVersion.class) == null) {
        versionClass = versionClass.getSuperclass();
      }
    }
    this.versionClass = versionClass;
  }

  /** The info of {@code type}, created on first use. */
  public static ClassInfo of(Class<?> type) {
    return CACHE.get(type);
  }

  /**
   * Creates an instance of {@code type} with its declared empty constructor, which may be private.
   *
   * @throws RuntimeException if there is none, or it fails
   */
  public static Object newInstance(Class<?> type) {
    try {
      Constructor<?> constructor = type.getDeclaredConstructor();
      if (!constructor.trySetAccessible()) {
        throw new RuntimeException("Could not set constructor accessible: " + type.getName());
      }
      return constructor.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException("Cannot instantiate " + type.getName(), e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Cannot invoke constructor of " + type.getName(), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Constructor of " + type.getName() + " became inaccessible", e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(
          "Type " + type.getName() + " does not have an empty constructor", e);
    }
  }

  /** Every declared field that isn't synthetic, in declaration order. */
  public List<FieldInfo> fields() {
    return this.fields;
  }

  /**
   * The fields the default serializer reads and writes: static fields only with {@code
   * FIND_STATIC_FIELDS}, and never synthetic ones. Fields with {@code @Ignore} are included.
   */
  public List<FieldInfo> dataFields(boolean findStaticFields) {
    return findStaticFields ? this.fields : this.instanceFields;
  }

  /** The {@code @ConfigVersion} of the class, also an inherited one; {@code null} if none. */
  public @Nullable ConfigVersion version() {
    return this.version;
  }

  /**
   * The class that declares {@link #version()}, which its migration strategies are registered
   * for; {@code null} if the class has no version.
   */
  public @Nullable Class<?> versionClass() {
    return this.versionClass;
  }

  /** The class comments with the registered placeholders applied. */
  public List<String> comments() {
    return FieldInfo.applyPlaceholders(this.comments);
  }

  /**
   * The annotation holders {@code AnnotatedConfigResolver} iterates over: the commented classes,
   * then the fields. Computed once per combination of settings; the returned set can't be modified.
   */
  public Set<AnnotationHolder> holders(
      boolean findParentFields, boolean reverseFields, boolean findStaticFields) {
    int index = (findParentFields ? 1 : 0) | (reverseFields ? 2 : 0) | (findStaticFields ? 4 : 0);
    Set<AnnotationHolder> cached = this.holders.get(index);
    if (cached == null) {
      // computed twice at worst, by threads that get here at the same time; both are equal
      cached = computeHolders(findParentFields, reverseFields, findStaticFields);
      this.holders.compareAndSet(index, null, cached);
      cached = this.holders.get(index);
    }
    return cached;
  }

  private Set<AnnotationHolder> computeHolders(
      boolean findParentFields, boolean reverseFields, boolean findStaticFields) {
    Set<AnnotationHolder> ret = new TreeSet<>();
    if (this.hasDeclaredAnnotations) {
      ret.add(new AnnotationHolder(this.type, 0));
    }
    List<FieldInfo> fields = new ArrayList<>(this.fields);
    if (findParentFields) {
      // a field of a class further up that a field of the same name hides is not an option of its
      // own: both were added, and the dump failed with a duplicate key
      Set<String> names = new HashSet<>();
      for (FieldInfo field : this.fields) {
        names.add(field.name());
      }
      int classOrder = 0;
      for (Class<?> parent = this.type.getSuperclass();
          parent != null && parent != Object.class;
          parent = parent.getSuperclass()) {
        // one lower per class up, so the classes further up are written first
        classOrder--;
        ClassInfo parentInfo = of(parent);
        if (parentInfo.hasAnnotations) {
          ret.add(new AnnotationHolder(parent, classOrder));
        }
        for (FieldInfo field : parentInfo.fields) {
          if (names.add(field.name())) {
            fields.add(field);
          }
        }
      }
    }

    if (reverseFields) {
      Collections.reverse(fields);
    }

    boolean record = this.type.isRecord();
    int order = 1;
    for (FieldInfo field : fields) {
      if (field.ignored() && !record) {
        continue;
      }
      if (!findStaticFields && field.isStatic()) {
        continue;
      }
      ret.add(new AnnotationHolder(field, order));
      order++;
    }
    return Collections.unmodifiableSet(ret);
  }

  /** How to create an instance of this record class. Only for records. */
  public RecordInfo record() {
    RecordInfo record = this.record;
    if (record == null) {
      // created twice at worst, by threads that get here at the same time; both are equal
      record = new RecordInfo(this);
      this.record = record;
    }
    return record;
  }

  /**
   * The constructor of a record and the order of its parameters.
   *
   * @hidden
   */
  public static final class RecordInfo {

    private final List<FieldInfo> components;
    private final Constructor<?> constructor;
    private final boolean accessible;
    private final String[] parameterNames;

    private RecordInfo(ClassInfo info) {
      Class<?> type = info.type;
      this.components = info.dataFields(false);
      // The canonical constructor takes the components in their declared order. Its parameter
      // names are not used: they are only kept when compiled with -parameters, and are arg0,
      // arg1, ... otherwise.
      RecordComponent[] recordComponents = type.getRecordComponents();
      Class<?>[] types = new Class<?>[recordComponents.length];
      this.parameterNames = new String[recordComponents.length];
      for (int i = 0; i < recordComponents.length; i++) {
        types[i] = recordComponents[i].getType();
        // the name of the component, which is the name of its field
        this.parameterNames[i] = recordComponents[i].getName();
      }
      Constructor<?> constructor;
      try {
        constructor = type.getDeclaredConstructor(types);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException("could not find record ctor of '" + type.getName() + "'", e);
      }
      this.constructor = constructor;
      this.accessible = constructor.trySetAccessible();
    }

    /** The record's components: its fields that are neither static nor synthetic. */
    public List<FieldInfo> components() {
      return this.components;
    }

    public Constructor<?> constructor() {
      return this.constructor;
    }

    /** Whether {@link #constructor()} could be made accessible. */
    public boolean accessible() {
      return this.accessible;
    }

    /**
     * The names of the record's components, in the order the constructor takes them. Shared:
     * don't modify the array.
     */
    public String[] parameterNames() {
      return this.parameterNames;
    }
  }
}
