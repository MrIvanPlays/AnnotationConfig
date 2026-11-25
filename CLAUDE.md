# CLAUDE.md

AnnotationConfig: a Java library that maps annotated Java objects to config files and back.
Java 25, Maven multi-module build.

## Project state

- The version is `3.1.0-SNAPSHOT` and not released yet. The last release is 3.0.3 (on Maven
  Central). Breaking changes are allowed in
  3.1.0, but every user-visible one goes into the 3.1.0 section of `CHANGELOG.md`, under
  "Breaking changes" when it can change what existing configs do. Changes are compared with
  3.0.3 (`git show 3.0.3:<path>`), not with earlier 3.1.0 commits: what something new in 3.1.0
  (records, JSON, JSON5, the TOML and `.properties` writers, writing through a temporary file)
  does is described in its own section, not as a change or a fix.
- Decisions made on purpose, don't change them without asking:
  - An empty string in a config is read as `""`, not as no value; empty numbers are no value.
  - Static fields are skipped unless `ACDefaultSettings.FIND_STATIC_FIELDS` is set.
  - TOML keeps `GENERATE_NEW_OPTIONS` off. Dotted `@Key`s are nested in every format except
    `.properties`: YAML and JSON use `DottedResolver`, TOML a key resolver in `TomlConfig` that
    first reads the key as one quoted key (`'a.b' = 1`, how 3.0.x wrote it) so existing TOML files
    keep loading, and otherwise uses `DottedResolver`.
  - `@Min` / `@Max` members default to the bound that never rejects (`MIN_VALUE` / `MAX_VALUE`,
    infinities), so `-1` is a usable bound.
  - Every module `pom.xml` repeats `url`, `developers`, `licenses` and `scm`: Maven Central requires
    them in each published POM. Don't remove them as duplicates of the parent's.
  - Settings passed to `load` / `dump` are filled with the resolver's settings
    (`ConfigResolverImpl#passSettings` calls `combine` on them), which is why they must be mutable.
  - A decimal read into a whole-number field (`int`, `long`, `BigInteger`, ...) is cut at its
    decimal point (`8080.9` is `8080`), not rejected.
  - Booleans: `true`, `yes` and `on` (any case) are `true`, anything else is `false`, never an
    error (`DataObject#parseBoolean`, used by `getAsBoolean` and the default serializer). Making
    unknown values fail would be a breaking change.
  - YAML is read as YAML 1.1, SnakeYAML's default: `yes`, `no`, `on` and `off` are booleans there.
  - `TomlConfig` registers the `java.time` serializers in the shared `SerializerRegistry` on first
    use; they write native TOML date-times when the settings hold `MAPPER_KEY`, strings otherwise.
  - Kept as they are: the name `SHOULD_REVERSE_FIELDS` (`true` means declaration order),
    `DefaultSerializer`'s `PRIMITIVE_CLASS` check, its `System.err` warning and `printStackTrace()`,
    `DataObject#putAll`, `@author claude, Ivan Pekov`, and the "DO NOT TOUCH" comment of the
    record code in `DefaultSerializer`.
  - `.properties` files have only the `.properties` extension: `.conf` usually means HOCON, so
    `resolveMultiple` would read other `.conf` files as `.properties`. 3.0.3 didn't register it
    either.

## Modules

- `core`: annotations (`@Key`, `@Comment`, `@ConfigObject`, `@Min`, `@Max`, ...), the resolver,
  serialization, settings and the `.properties` format (`PropertyConfig`)
- `yaml`, `json`, `toml`: one module per format, each depending on `core`
- `examples`: usage examples, compiled as part of the build
- `unit-testing`: the tests of every module, see [Tests](#tests)
- Reflection is read once per class: `core.internal.ClassInfo` (a `ClassValue`) holds each class's
  `FieldInfo`s (key, annotations, generic type, raw comments) and the annotation holders per
  combination of settings, and `AnnotatedConfigResolver` and `DefaultSerializer` use it. Comment
  placeholders are applied on every dump, and serializers are looked up on every use, as both
  registries can change at runtime.
- `core.internal.FieldValidation` applies `@Min`, `@Max` and custom annotation validators to a read
  value. Every path that sets a value uses it: config fields and `@ConfigObject` sections
  (`AnnotatedConfigResolver#setFields`), the fields of custom data types (also of a type with one
  field, read from the bare value) and record components (`DefaultSerializer`). `@Min` / `@Max`
  also target `TYPE_USE`: `MinMaxValidator` walks the field's `AnnotatedType` next to the value
  for bounds on element types (`List<@Max(...) Integer>`, map keys and values, nested types,
  wildcard bounds). On an array field Java puts `@Max(...) int[]` on the element type, so there
  it bounds the elements, not the array.
- `@Polymorphic` is handled in two places: `AnnotatedConfigResolver` for config fields and
  sections (`FieldInfo#polymorphic()`, the annotation on the field or its type's declaration), and
  `DefaultSerializer` for everything else (list and array elements, fields of custom data types,
  record components). `core.internal.PolymorphicInfo` checks an annotation on first use, not when
  `ClassInfo` reads the class, so an invalid one on an ignored field breaks nothing. On a `List`
  or array field, the annotation applies to the elements: reading passes the elements only the
  field's `@Polymorphic` (writing already passed them all its annotations).
- Records are created with their canonical constructor, found by and filled in the order of
  `Class#getRecordComponents()`; constructor parameter names are not used, as tools like
  obfuscators strip them.
- `AnnotatedConfigResolver#dump(Path)` creates missing directories and follows symbolic links to
  the file they point to. A new file is written in place; an existing one is replaced by a
  temporary file (owner-only on POSIX) that first gets the old file's POSIX permissions.
- An immutable `DataObject` made from a map wraps it lazily, and `DataObject#mapView()`
  (package-private) gives `DefaultSerializer` the map without `getAsMap()`'s deep copy. Loading
  used to wrap and copy each value once per level of nesting.

## Formats

- YAML is read by SnakeYAML with `YamlHandler`'s default `Yaml`, whose constructor reads a decimal
  that a `double` can't hold exactly as a `BigDecimal` (`ExactDecimalConstructor`); other decimals
  stay `Double`. A `Yaml` passed through `YAML_INSTANCE` reads every decimal as a `double`.
- Reading goes through each format's library: `java.util.Properties`, SnakeYAML, Gson (numbers
  read as `BigDecimal`), Jackson's `TomlMapper` (floats read as `BigDecimal`, also in 2.15.2, the
  version 3.0.x used). `DataObject#getAsMap()` hands numbers on as read, so these `BigDecimal`s
  reach `Map<String, Object>` fields; in 3.0.x they were turned into `Double`. A `BigDecimal` for a
  `double` / `float` field is converted with `doubleValue()` / `floatValue()`, which are correctly
  rounded since Java 21 and were checked to equal parsing the text; other numbers go through their
  text (so `3.11f` stays `3.11` in a `double` field).
- `.properties` splits every value that holds a comma into an array when reading
  (`PropertyConfig#parseValue`), as 3.0.x did, so a string with a comma does not read back. This
  is kept on purpose: telling them apart needs the field's type, which the reader doesn't have,
  and a setting for the default serializer was tried and dropped.
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
  offline, so the scenarios check the exact JSON5 output and read it back with Gson, and
  `TestJson5ValueWriter` checks the syntax only JSON5 has.
- Gson's lenient reader (2.15) does not read all of JSON5: a trailing comma fails in an object and
  adds a `null` element in an array; hex numbers, a leading `+` and a leading or trailing decimal
  point are read as strings (number fields parse all but hex); the `\x`, `\v` and `\0` escapes fail;
  a line continuation (backslash before a line break) keeps the line break, which JSON5 drops. So
  `@Multiline` strings are written on one line with `\n` escapes. The CHANGELOG and the Javadoc of
  `JsonConfig#getJson5ConfigResolver()` list these limits; keep them in sync.
- `PropertiesDumper` and `Json5ValueWriter` build the whole output in a `StringBuilder` and write
  it to the `PrintWriter` once, and JSON is written by one `Gson#toJson` call: many small writes
  through the `PrintWriter` (each takes its lock) measured ~35% slower. `YamlHandler` gives
  SnakeYAML's emitter an unlocked `Writer` over a `StringBuilder` for the same reason, and
  `TomlValueWriter` builds the whole file in one `StringBuilder` too. Buffering centrally in
  `AnnotatedConfigResolver` instead was benchmarked and was slower (an extra copy of the output,
  and SnakeYAML's writes still go through a `PrintWriter`), so each writer buffers on its own.
- TOML, JSON5 and YAML split comments with `core.utils.LinesHelper` (public API; a regex only when a
  comment has a line break), and build the dotted paths of keys only in maps that hold a commented
  key (`MapUtils#parentPaths`). YAML still looks up a key that holds a map by its plain key everywhere
  (the `commentpaths` scenarios). `.properties` splits only at `\r` and `\n`, in `PropertiesDumper`.
- Map helpers go in `MapUtils` (public, in `utils`), not as private copies in the classes that
  use them.
- Every writer ends lines with `'\n'`, never `println()` or `System.lineSeparator()`, and so does
  the class comment written by `AnnotatedConfigResolver`. Every format's file ends with a line
  break, JSON and JSON5 included. Mixing them gave files with CRLF and LF
  on Windows. The `lineendings` scenarios compare the output of every format without normalizing
  line endings; they can only catch a regression when run on Windows.
- Field order: `.properties` writes fields in reverse declaration order; YAML, JSON and TOML set
  `SHOULD_REVERSE_FIELDS`, so they write them in declaration order.
- Jackson's TOML parser before 2.23 misreads 19-digit integers (drops the first 9 digits);
  `TestValues#numbersAreReadExactly` (the `values` scenarios) catches a regression.

## Build and test

- Full build with tests: `mvn -o install`
- `.mvn/maven.config` builds modules in parallel (`-T 1C`, so output of different modules
  interleaves), and `.mvn/jvm.config` plus the `argLine` property in `pom.xml` run Maven and the
  test JVMs with C1 only (`-XX:TieredStopAtLevel=1`). Together they made `install` ~35% faster and
  `clean install` ~40% faster. `-T 2C` and `-XX:+UseSerialGC` were measured and gave nothing more.
- Every test is in the `unit-testing` module. One test:
  `mvn -o test -pl unit-testing -am -Dtest=TestPolymorphic -Dsurefire.failIfNoSpecifiedTests=false`
  (`-am` builds the changed sources of the other modules; the flag stops it from failing in them,
  as they have no tests). Without `-am`, `unit-testing` uses the installed jars of the other
  modules, so run `mvn -o install -DskipTests` after changing them.
- Checkstyle (Google style, `checkstyle.xml`) runs during `process-classes` on main and test
  sources, and any violation fails the build: `checkstyle.xml` reports them as warnings, and
  `violationSeverity` in `pom.xml` makes warnings fail. It caches the files that passed in
  `target/checkstyle-cachefile`, so after changing `checkstyle.xml` run `clean` to check every
  file. The `deployment` profile skips it (`checkstyle.skip`); add `-Dcheckstyle.skip=false` to
  run it there.
- `checkstyle.xml` deviates from Google style on purpose: `_` is allowed as a variable name, `AC`
  as an abbreviation (`ACDefaultSettings`), Unicode escapes are not checked (sources are ASCII),
  and test sources are exempt from the naming checks for methods, fields and abbreviations
  (`aMissingOptionKeepsItsValue`, fields `a`, `b`, `TestYAML`) and from missing Javadoc.
  The Javadoc of types marked `@hidden` (all of `core.internal`, too) is not checked; the `@hidden`
  filter (`SuppressWithNearbyTextFilter`) assumes the tag is on a type, as it is everywhere
  today, and skips the rest of the file after it. `@Deprecated` code needs no Javadoc and is exempt
  from `LocalVariableName`, and a Javadoc may be
  only tags (such as `@deprecated`): XPath filters can't exempt `@Deprecated` code from Javadoc
  checks, so that is allowed everywhere.
- Building an older commit for comparison: `git archive <commit> | tar -x -C <dir>` and
  `mvn -o package -Dmaven.test.skip=true` there. `package` leaves `~/.m2` alone, and `git archive`
  leaves the repository untouched.

## Benchmarks

- `BENCHMARKS.md` holds measured dump and load times. The harness is not in the repository (JMH is
  not available offline): a standalone class compiled against the module jars, one JVM per
  benchmark, 3 s warm-up, 10 measured rounds, median per operation, allocation per thread. It
  dumps and loads four configs (flat, nested, large, dotted; `.properties` only flat) per format,
  plus the format's parser alone. Every benchmark runs in two passes; one whose passes differ by
  more than 10% runs twice more, and the median of all runs counts.
- When a change can affect a writer's or reader's speed, measure the affected benchmarks the same
  way against the previous commit before calling it an improvement. Differences below ~10% are
  noise.
- Then update `BENCHMARKS.md` by re-measuring the current version and replacing the numbers in the
  result tables and notes. Don't add a note about the change on top of old numbers.
- Don't rebuild or re-measure old commits for the comparison with `6207cbc` (3.0.x): its column is
  fixed. Only the current version's column and the "Change" column are updated.
- A change in percent is relative to the old value (`new / old - 1`, so 3.18 -> 3.56 is +12%),
  computed from the rounded numbers the tables show; from 2x on it is written as "4.4x slower".

## Conventions

- Every text file is stored and checked out with LF line endings: `.gitattributes` sets
  `* text=auto eol=lf`, which overrides `core.autocrlf`. Git turns CRLF into LF when a file is
  added, but write new files with LF anyway. Perl halves backslashes in replacement text, so make edits that
  contain backslashes with an editor, not a regex.
- Java sources are plain ASCII: write non-ASCII characters as `\uXXXX` escapes. Tools that write
  files can turn such escapes into the raw characters (in docs too), so check new files with
  `grep -nP '[^\x00-\x7F]'` and look at every `\u` escape after writing it.
- Commit messages follow Conventional Commits (`feat(core):`, `fix(core):`, ...); mark breaking
  changes with `!` and a `BREAKING CHANGE:` footer. Commits that only change documentation end
  their subject with `[ci skip]`.
- Commit only when asked, and as one commit unless asked otherwise. Don't push.
- `// <editor-fold ...>` / `// </editor-fold>` comments are the IDE layout the maintainer uses: keep
  them, and keep code added next to a fold inside or outside it as fits. Test classes put their
  tests first and their subject classes after them (the IDE's rearrange order, enums first); a
  topic has a fold around its tests and a `"<topic>: subjects"` fold around its subject classes.
- `CHANGELOG.md` has one section per version; user-visible and breaking changes go there.
- Public API: `annotations`, `resolver`, `serialization`, `settings` and `utils` in `core`, plus the
  format entry points (`YamlConfig`, `JsonConfig`, ...). The `core.internal` package and classes
  marked `@hidden` (for example `DefaultSerializer`) are not API and may change freely.

## Tests

- Every test is in the `unit-testing` module, which is never installed or published (excluded from
  the Central bundle like `examples`). Tests keep the package of the module they test (`core`,
  `yaml`, `json`, `toml`), so they reach its package-private classes; there are no JPMS modules.
- What the formats share is tested once, by the scenarios in `scenarios`: one package per topic
  (`sections`, `collections`, `values`, `objects`, `comments`, `bounds`, `validation`, `fields`,
  `files`, `fullconfig`, `polymorphic`, `resolver`, `lineendings`, `commentpaths`,
  `threadsafety`). Every test that loads or dumps a config goes there, also when it tests `core`
  logic: each parser hands values on as types of its own, which a test through `.properties` alone
  doesn't see. A scenario class is a
  `@ParameterizedClass` that extends `Scenario` and runs once per format that has the features it
  needs: its `@MethodSource` is one of `Sources` (`ALL`, `WITH_SECTIONS`, ...). A check that only
  some formats can pass goes in a class with a narrower source (`TestNulls`, `TestSpecialFloats`,
  `TestUntypedNumbers`), or branches on `format.has(...)` when it differs in one assertion.
- `scenarios.Format` is the list of formats: each constant has its resolver, file extension, entry
  point and `Feature`s (sections, comments, generating new options, nulls, integers beyond 64 bits,
  NaN and infinities), and `Format` has its commenting writer and parser. A new
  format is a constant there plus its scenario texts; no scenario changes.
- The text of a scenario in each format is a resource, `scenarios/<topic>/<name>.<extension>`:
  the inputs it loads and the exact outputs it expects (`Scenario#load`, `Scenario#assertDump`). A
  JSON5 scenario without a `.json5` file reads the `.json` one, as JSON5 reads JSON; outputs have
  their own file. Inputs are written by hand. Outputs are recorded:
  `mvn -o test -pl unit-testing -Dscenarios.record=<directory>` writes them to that directory
  instead of comparing them; review every file there before copying it to the resources.
- Scenarios compare values whose number type depends on the parser with
  `Scenario#numbersAsDecimals`. Where they work around a limit of a format, a comment says so.
- Tests of one format's own syntax stay in its package (`TestYamlScalars`, `TestTomlStructure`,
  `TestJson5ValueWriter`, `TestProperties`, ...). The `core` tests are of logic no file reaches:
  registries, `DataObject`, the default serializer called with Java maps, annotation checks, the
  file system (`TestConfigFiles`).
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
- `TestConfigFiles` (core) checks POSIX permissions and symbolic links; those tests skip where the
  file system has no POSIX view or links can't be created, as on Windows without developer mode.
  They run on CI (Ubuntu).
- Expected output of a writer ends with a line break: in a text block, the closing `"""` goes on
  its own line, not after a `\` that would drop the final line break.
- Test classes run in parallel (JUnit configuration parameters in the surefire config of
  `pom.xml`), all in one JVM; the methods of one class run one after another. A class may change
  global state (registries, static fields) only under keys and types no other class uses, or it
  must be annotated `@Isolated`. The formats of a scenario class run in parallel and share its
  subjects, so a scenario that changes a static field or a registry locks it with `@ResourceLock`
  (`TestFields`, `TestCollections`, `TestValidation`).
- The shared resolvers are created in holder classes (`ResolverHolder`, ...): the JVM initializes a
  class once, so concurrent first calls get one instance. The `threadsafety` scenarios load each
  entry point in a fresh class loader to check that.
- Writer tests compare the exact output (Java text blocks, or the resource files of scenarios;
  both use `\n`) without normalizing line endings, and also read it back. A test for a fixed bug
  should fail on the old code: check that by running it against the previous commit's version of
  the class.
