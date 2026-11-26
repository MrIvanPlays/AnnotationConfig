# Benchmarks

Dump and load times of AnnotationConfig 3.1.0 (commit `da8cace`), and a comparison with commit
`6207cbc` (3.0.4-SNAPSHOT, right after the 3.0.3 release).
Lower is better.

## Setup

- AMD Ryzen 7 7730U (8 cores, 16 threads), 15 GB RAM, Windows 11, OpenJDK 25.0.4.1, `-Xms1g -Xmx1g`
- Libraries: SnakeYAML 2.7, Gson 2.15.0, Jackson TOML 2.23.0 (`6207cbc`: Jackson 2.22.4)
- Hand-written harness, not JMH: every benchmark runs in its own JVM, is warmed up for 3 seconds,
  then measured in 10 rounds of at least 100 ms each. The tables show the median time per
  operation; allocation is measured per thread.
- **Dump** writes a config object to an in-memory `Writer`, **load** reads one from an in-memory
  `Reader` into a new object. **Parser** is the format's library alone reading the same text into
  a `Map` (`java.util.Properties`, SnakeYAML, Gson, Gson in lenient mode for JSON5, Jackson's
  `TomlMapper`): the part of a load that is not AnnotationConfig.
- Every benchmark ran twice, in two passes. Benchmarks whose passes differed by more than 10% ran
  twice more; the tables show the median of all runs.
- The `6207cbc` column was measured once, in the same run as 3.1.0 at `1a07218`, with the two
  versions back to back in alternating order. It is not re-measured when the 3.1.0 numbers are
  updated.
- These are warmed-up numbers. An application usually loads its config once at startup, before the
  JIT has compiled anything; that first call is dominated by class loading and is not measured.
- Differences below ~10% are within noise, for the large YAML config up to ~20%.

### Configs

- **Flat**: 12 scalar options (strings, ints, a long, doubles, booleans), two with comments.
- **Nested**: two `@ConfigObject` sections, one with a nested section, comments, a list of strings,
  a list of 5 objects and a map with 10 entries.
- **Large**: a list of 1000 objects with 4 fields each, a map with 200 entries and an `int[]` of
  1000 numbers.
- **Dotted**: 12 scalar options, 11 of them with a dotted `@Key` of two or three parts
  (`server.ip`, `limits.players.max`, ...) that are written nested, two with comments.

`.properties` only supports flat options, so it is only measured with the flat config. The JSON
module did not exist at `6207cbc`.

## Results

### Flat config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| Properties | 1.85 µs | 3.98 µs | 2.60 µs | 5.5 / 22.4 KB | 206 chars |
| YAML | 13.73 µs | 15.58 µs | 13.78 µs | 28.6 / 34.6 KB | 226 chars |
| JSON | 1.79 µs | 2.06 µs | 1.02 µs | 4.6 / 5.9 KB | 256 chars |
| JSON5 | 1.83 µs | 2.23 µs | 1.16 µs | 5.6 / 5.9 KB | 260 chars |
| TOML | 1.65 µs | 3.03 µs | 2.05 µs | 4.5 / 4.5 KB | 238 chars |

### Nested config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 52.72 µs | 62.04 µs | 56.75 µs | 97.2 / 120.1 KB | 851 chars |
| JSON | 7.98 µs | 8.01 µs | 3.74 µs | 19.2 / 18.7 KB | 1,086 chars |
| JSON5 | 7.57 µs | 8.25 µs | 4.08 µs | 21.5 / 18.6 KB | 1,105 chars |
| TOML | 6.44 µs | 11.59 µs | 8.12 µs | 18.1 / 20.1 KB | 771 chars |

### Large config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 5.30 ms | 7.52 ms | 7.80 ms | 9.0 / 12.1 MB | 82,819 chars |
| JSON | 0.86 ms | 0.85 ms | 0.42 ms | 2.1 / 1.6 MB | 119,849 chars |
| JSON5 | 0.66 ms | 0.89 ms | 0.46 ms | 1.7 / 1.6 MB | 111,443 chars |
| TOML | 0.46 ms | 1.14 ms | 0.84 ms | 1.5 / 2.0 MB | 76,623 chars |

### Dotted config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 16.66 µs | 20.20 µs | 17.29 µs | 37.3 / 41.1 KB | 270 chars |
| JSON | 2.91 µs | 2.73 µs | 1.20 µs | 9.4 / 8.7 KB | 353 chars |
| JSON5 | 3.00 µs | 2.96 µs | 1.39 µs | 9.8 / 8.7 KB | 353 chars |
| TOML | 2.76 µs | 4.20 µs | 2.72 µs | 9.6 / 8.4 KB | 265 chars |

## Compared with `6207cbc`

| Benchmark | `6207cbc` | 3.1.0 | Change |
|---|---|---|---|
| Properties dump, flat | 3.18 µs | 1.85 µs | −42% |
| Properties load, flat | 4.23 µs | 3.98 µs | −6% |
| YAML dump, flat | 3.44 µs | 13.73 µs | 4.0× slower |
| YAML dump, nested | 11.19 µs | 52.72 µs | 4.7× slower |
| YAML dump, large | 0.75 ms | 5.30 ms | 7.1× slower |
| YAML load, flat | 16.20 µs | 15.58 µs | −4% |
| YAML load, nested | 63.79 µs | 62.04 µs | −3% |
| YAML load, large | 8.87 ms | 7.52 ms | −15% |
| TOML dump, flat | 4.83 µs | 1.65 µs | −66% |
| TOML dump, nested | 9.77 µs | 6.44 µs | −34% |
| TOML dump, large | 0.63 ms | 0.46 ms | −27% |
| TOML load, flat | 3.98 µs | 3.03 µs | −24% |
| TOML load, nested | 15.27 µs | 11.59 µs | −24% |
| TOML load, large | 1.55 ms | 1.14 ms | −26% |

The output differs between the versions: `6207cbc` wrote 220 chars of `.properties`, 241, 920 and
87,023 chars of YAML and 253, 869 and 78,217 chars of TOML (flat, nested, large).

## Notes

- **YAML dumping** is the one regression. `6207cbc` wrote YAML with a hand-written writer; 3.1.0
  builds a SnakeYAML node tree and lets SnakeYAML emit it, which handles quoting, escaping and
  indentation correctly but is slower, increasingly so for large configs.
- **YAML loading** is almost entirely SnakeYAML's parser (the parser column). The large config's
  load measured 4% faster than the parser alone in both passes (7.46 and 7.58 against 7.62 and
  7.99 ms): the two do not run the same code, as the parser column is a default
  `new Yaml().load(...)` and a load reads with `YamlHandler`'s own `Yaml`
  (`ExactDecimalConstructor`, `loadAs`). Its −15% against `6207cbc` is within the noise of the
  large YAML config.
- **Reflection** is read once per class: the fields of a config class and of the data types in it,
  their keys, annotations and comments are cached, so a dump or load only reads and sets values.
  Loading reads the maps a parser returns without wrapping and copying them once per level of
  nesting.
- **TOML** is written by `TomlValueWriter` in 3.1.0 and by Jackson's TOML generator in `6207cbc`.
  It is faster despite also writing the comments of options inside sections, and builds the whole
  file before writing it. Config objects and maps are written as tables, so the nested config's
  output is 11% smaller than `6207cbc`'s.
- **`.properties`** dumping escapes keys and values completely in 3.1.0, which `6207cbc` did not,
  and is still 42% faster.
- **JSON** loading takes 2.0 to 2.3 times the parser's time: Gson is fast, so the reflection and
  value conversion of AnnotationConfig make up about half of a load.
- **JSON5** is written by `Json5ValueWriter` and dumps as fast as JSON, the large config 23% faster
  (0.66 against 0.86 ms) with 7% less output, as identifier keys are written without quotes. It is
  read by Gson in lenient mode, whose parser is 9 to 16% slower than for JSON; loads are within 8%
  of JSON's.
- **Dotted keys**: the dotted config's dump and load take 1.3 to 1.7 times as long as the flat
  config's, which has as many options, in JSON, JSON5 and TOML (TOML: 2.76 against 1.65 µs dump,
  4.20 against 3.03 µs load), and allocate 47-113% more. Dumping pays for `DottedResolver#boxTo`
  and the merging of boxed keys in core, loading for `DottedResolver#unbox` and for the parser
  building nested maps instead of one flat one (TOML's parser: 2.72 against 2.05 µs). In YAML both
  take 1.2 to 1.3 times as long, mostly in SnakeYAML (its parser alone: 17.29 against 13.78 µs).
- **Big numbers in maps and lists** are kept exact instead of being turned into `double` and `int`.
  Gson and Jackson's TOML parser read decimals as `BigDecimal`; for `double` / `float` fields it is
  converted with `doubleValue()` / `floatValue()` rather than through its text, which kept the
  large config's JSON and TOML loads from getting slower than before the change.
