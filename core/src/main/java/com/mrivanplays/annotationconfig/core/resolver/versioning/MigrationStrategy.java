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

package com.mrivanplays.annotationconfig.core.resolver.versioning;

import com.mrivanplays.annotationconfig.core.resolver.key.KeyResolver;
import com.mrivanplays.annotationconfig.core.serialization.DataObject;
import java.util.function.BiFunction;
import org.jspecify.annotations.NullMarked;

/**
 * One step of the migration of a config file to a newer version: it changes the values read from
 * the file before the fields of the config are set from them.
 *
 * <p>It gets every value of the file, without the version, as a mutable {@link DataObject}, and the
 * {@link KeyResolver} the config is read with (its {@code KEY_RESOLVER} setting), and returns the
 * values of the newer version: the same data object changed, or a new one. Keys are the keys of the
 * file as the format read them: sections are nested data objects in YAML, JSON and TOML, while
 * {@code .properties} keeps dotted keys such as {@code server.port} flat. Pass the key resolver to
 * {@link DataObject#renameDottedKey(String, String, KeyResolver)} to move an option the same way in
 * every format:
 *
 * <pre>{@code
 * (values, keyResolver) -> {
 *   values.renameDottedKey("server.port", "network.port", keyResolver);
 *   return values;
 * }
 * }</pre>
 *
 * <p>Values are of the types the format's parser reads, so use the getters of {@link DataObject}
 * ({@link DataObject#getAsInt()}, ...) to read them.
 *
 * <p>Keys it doesn't move are kept, but only the keys of the config's fields are read, and the file
 * is rewritten from the fields: a strategy doesn't have to remove keys that are no longer used.
 *
 * @author Ivan Pekov
 * @since 3.1.0
 * @see MigrationStrategies
 */
@NullMarked
@FunctionalInterface
public interface MigrationStrategy extends BiFunction<DataObject, KeyResolver, DataObject> {}
