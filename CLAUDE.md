# CLAUDE.md

AnnotationConfig: a Java library that maps annotated Java objects to config files and back.
Java 25, Maven multi-module build.

## Project state

- The version is `3.1.0-SNAPSHOT` and not released yet. The last release is 3.0.3 (on Maven
  Central, but without a git tag; the newest tag is `3.0.2`). Breaking changes are allowed in
  3.1.0, but every user-visible one goes into the 3.1.0 section of `CHANGELOG.md`, under
  "Breaking changes" when it can change what existing configs do.
- Decisions made on purpose, don't change them without asking:
  - An empty string in a config is read as `""`, not as no value; empty numbers are no value.
  - Static fields are skipped unless `ACDefaultSettings.FIND_STATIC_FIELDS` is set.
  - TOML keeps `GENERATE_NEW_OPTIONS` off. Dotted `@Key`s are nested in every format except
    `.properties`: YAML and JSON use `DottedResolver`, TOML a key resolver in `TomlConfig` that
    first reads the key as one quoted key (`'a.b' = 1`, how 3.0.x wrote it) so existing TOML files
    keep loading, and otherwise uses `DottedResolver`.
  - `@Min` / `@Max` members default to the bound that never rejects (`MIN_VALUE` / `MAX_VALUE`,
    infinities), so `-1` is a usable bound.

## Modules

- `core`: annotations (`@Key`, `@Comment`, `@ConfigObject`, `@Min`, `@Max`, ...), the resolver,
  serialization, settings and the `.properties` format (`PropertyConfig`)
- `yaml`, `json`, `toml`: one module per format, each depending on `core`
- `examples`: usage examples, compiled as part of the build

## Formats

- Reading goes through each format's library: `java.util.Properties`, SnakeYAML, Gson (numbers
  read as `BigDecimal`), Jackson's `TomlMapper` (floats read as `BigDecimal`, also in 2.15.2, the
  version 3.0.x used). `DataObject#getAsMap()` hands numbers on as read, so these `BigDecimal`s
  reach `Map<String, Object>` fields; in 3.0.x they were turned into `Double`. A `BigDecimal` for a
  `double` / `float` field is converted with `doubleValue()` / `floatValue()`, which are correctly
  rounded since Java 21 and were checked to equal parsing the text; other numbers go through their
  text (so `3.11f` stays `3.11` in a `double` field).
- Writing: `.properties` by `PropertiesDumper`, YAML by `YamlHandler` (a SnakeYAML node tree; for
  the default `Yaml` instance numbers and booleans are built by hand exactly as SnakeYAML's
  representer would, guarded by `TestYamlScalars`), JSON by Gson, TOML by `TomlValueWriter`
  (written by hand to the TOML 1.0 spec; Jackson only reads TOML). Jackson's TOML writer (checked
  with 2.23.0) can't write comments or `java.time` values, writes `null` as `''` and integers
  beyond 64 bits as bare integers, and was slower than `TomlValueWriter`.
- JSON5 (JSON with comments) is written by `Json5ValueWriter` in the `json` module, by hand like
  TOML. It only uses JSON5 features that Gson's lenient reader understands (`//` comments,
  unquoted identifier keys, `NaN` / `Infinity`), so JSON5 is read with the same Gson settings as
  JSON; keep it that way, or reading needs a JSON5 parser. No JSON5 validator is available
  offline, so `TestJson5ValueWriter` checks exact output and reads it back with Gson.
- Gson's lenient reader (2.15) does not read all of JSON5: a trailing comma fails in an object and
  adds a `null` element in an array; hex numbers, a leading `+` and a leading or trailing decimal
  point are read as strings (number fields parse all but hex); the `\x`, `\v` and `\0` escapes fail;
  a line continuation (backslash before a line break) keeps the line break, which JSON5 drops. So
  `@Multiline` strings are written on one line with `\n` escapes. The CHANGELOG and the Javadoc of
  `JsonConfig#getJSON5ConfigResolver()` list these limits; keep them in sync.
- `PropertiesDumper` and `Json5ValueWriter` build the whole output in a `StringBuilder` and write
  it to the `PrintWriter` once, and JSON is written by one `Gson#toJson` call: many small writes
  through the `PrintWriter` (each takes its lock) measured ~35% slower. `YamlHandler` gives
  SnakeYAML's emitter an unlocked `Writer` over a `StringBuilder` for the same reason.
  `TomlValueWriter` is the exception: it builds each line in a `StringBuilder` but writes line by
  line. Buffering centrally in `AnnotatedConfigResolver` instead was benchmarked and was slower (an
  extra copy of the output, and SnakeYAML's writes still go through a `PrintWriter`), so each
  writer buffers on its own.
- Every writer ends lines with `'\n'`, never `println()` or `System.lineSeparator()`, and so does
  the class comment written by `AnnotatedConfigResolver`. Mixing them gave files with CRLF and LF
  on Windows. `TestLineEndings` (core) and `TestYamlLineEndings`, `TestJsonLineEndings` and
  `TestTomlLineEndings` compare output without normalizing line endings; they can only catch a
  regression when run on Windows.
- Field order: `.properties` writes fields in reverse declaration order; YAML, JSON and TOML set
  `SHOULD_REVERSE_FIELDS`, so they write them in declaration order.
- Jackson's TOML parser before 2.23 misreads 19-digit integers (drops the first 9 digits);
  `TestTomlValues#nineteenDigitIntegersAreReadExactly` catches a regression.

## Build and test

- Full build with tests: `mvn -o install`
- One test: `mvn -o test -pl yaml -Dtest=TestStaticFields`
- Testing a format module against changed `core` code: run `mvn -o install -DskipTests -pl core`
  first. With `-pl`, the module uses the installed core jar, not the sources.
- Checkstyle (Google style, `checkstyle.xml`) runs during `process-classes` on main and test
  sources. Violations are reported as warnings and do not fail the build, so look for them in
  the build output.
- Building an older commit for comparison: `git archive <commit> | tar -x -C <dir>` and
  `mvn -o package -Dmaven.test.skip=true` there. `package` leaves `~/.m2` alone, and `git archive`
  leaves the repository untouched.

## Benchmarks

- `BENCHMARKS.md` holds measured dump and load times. The harness is not in the repository (JMH is
  not available offline): a standalone class compiled against the module jars, one JVM per
  benchmark, 3 s warm-up, 10 measured rounds, median per operation, allocation per thread. It
  dumps and loads three configs (flat, nested, large) per format, plus the format's parser alone.
- When a change can affect a writer's or reader's speed, measure it the same way against the
  previous commit before calling it an improvement, and update `BENCHMARKS.md` with the new numbers.
  Differences below ~10% are noise.

## Conventions

- Source files are committed with CRLF line endings; keep them CRLF. In Git Bash, `sed -i` strips
  the CR from every line it rewrites. Use `perl -pi -e '...'` or an editor instead, and check with
  `file <path>` (it should say "with CRLF line terminators"). Perl also halves backslashes in
  replacement text, so make edits that contain backslashes with an editor, not a regex.
- Java sources are plain ASCII: write non-ASCII characters as `\uXXXX` escapes. Tools that write
  files can turn such escapes into the raw characters (in docs too), so check new files with
  `grep -nP '[^\x00-\x7F]'` and look at every `\u` escape after writing it.
- Commit messages follow Conventional Commits (`feat(core):`, `fix(core):`, ...); mark breaking
  changes with `!` and a `BREAKING CHANGE:` footer. Commits that only change documentation end
  their subject with `[ci skip]`.
- Commit only when asked, and as one commit unless asked otherwise. Don't push.
- `CHANGELOG.md` has one section per version; user-visible and breaking changes go there.
- Public API: `annotations`, `resolver`, `serialization`, `settings` and `utils` in `core`, plus the
  format entry points (`YamlConfig`, `JsonConfig`, ...). The `core.internal` package and classes
  marked `@hidden` (for example `DefaultSerializer`) are not API and may change freely.

## Tests

- `core` tests mostly go through `PropertyConfig.getConfigResolver()`; format module tests use
  e.g. `YamlConfig.getConfigResolver()` with a `StringReader` / `StringWriter`.
- `dump` requires every non-ignored field to have a value; a `null` field fails with
  "No default value for field".
- Non-default behaviour is enabled per call with a `Settings` passthrough, e.g.
  `new Settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true)`. Don't change the settings of
  the shared resolvers returned by `getConfigResolver()`: that leaks into other tests.
- `SerializerRegistry`, `CustomAnnotationRegistry` and `CommentPlaceholderRegistry` are global;
  tests that register something unregister it in `@AfterEach` / `@AfterAll`.
- Surefire only runs classes named `Test*`, `*Test`, `*Tests` or `*TestCase`; other names are
  silently skipped.
- Use `@TempDir` for files, never the working directory.
- Writer tests compare the exact output (Java text blocks, line endings normalized with
  `replaceAll("\\R", "\n")`) and also read it back. A test for a fixed bug should fail on the old
  code: check that by running it against the previous commit's version of the class.
