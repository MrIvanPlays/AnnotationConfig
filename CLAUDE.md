# CLAUDE.md

AnnotationConfig: a Java library that maps annotated Java objects to config files and back.
Java 25, Maven multi-module build.

## Modules

- `core`: annotations (`@Key`, `@Comment`, `@ConfigObject`, `@Min`, `@Max`, ...), the resolver,
  serialization, settings and the `.properties` format (`PropertyConfig`)
- `yaml`, `json`, `toml`: one module per format, each depending on `core`
- `examples`: usage examples, compiled as part of the build

## Formats

- Reading goes through each format's library: `java.util.Properties`, SnakeYAML, Gson (numbers
  read as `BigDecimal`), Jackson's `TomlMapper`.
- Writing: `.properties` by `PropertiesDumper`, YAML by `YamlHandler` (a SnakeYAML node tree; for
  the default `Yaml` instance numbers and booleans are built by hand exactly as SnakeYAML's
  representer would, guarded by `TestYamlScalars`), JSON by Gson, TOML by `TomlValueWriter`
  (written by hand to the TOML 1.0 spec; Jackson only reads TOML). Jackson's TOML writer (checked
  with 2.23.0) can't write comments or `java.time` values, writes `null` as `''` and integers
  beyond 64 bits as bare integers, and was slower than `TomlValueWriter`.
- Writers build the whole output in a `StringBuilder` and write it to the `PrintWriter` once:
  many small writes through the `PrintWriter` (each takes its lock) measured ~35% slower.
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

## Conventions

- Source files are committed with CRLF line endings; keep them CRLF. In Git Bash, `sed -i` strips
  the CR from every line it rewrites. Use `perl -pi -e '...'` or an editor instead, and check with
  `file <path>` (it should say "with CRLF line terminators"). Perl also halves backslashes in
  replacement text, so make edits that contain backslashes with an editor, not a regex.
- Commit messages follow Conventional Commits (`feat(core):`, `fix(core):`, ...); mark breaking
  changes with `!` and a `BREAKING CHANGE:` footer.
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
