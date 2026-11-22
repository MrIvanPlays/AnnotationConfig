# CLAUDE.md

AnnotationConfig: a Java library that maps annotated Java objects to config files and back.
Java 25, Maven multi-module build.

## Modules

- `core`: annotations (`@Key`, `@Comment`, `@ConfigObject`, `@Min`, `@Max`, ...), the resolver,
  serialization, settings and the `.properties` format (`PropertyConfig`)
- `yaml`, `json`, `toml`: one module per format, each depending on `core`
- `examples`: usage examples, compiled as part of the build

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
  `file <path>` (it should say "with CRLF line terminators").
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
  `new Settings().put(ACDefaultSettings.FIND_PARENT_FIELDS, true)`.
