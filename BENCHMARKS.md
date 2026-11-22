# Benchmarks

Dump and load times of AnnotationConfig 3.1.0 (commit `1a07218` with the single-pass
`DottedResolver`), and a comparison with commit `6207cbc` (3.0.4-SNAPSHOT, right after the 3.0.3
release). Lower is better.

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
| Properties | 3.56 µs | 4.18 µs | 2.60 µs | 15.9 / 24.9 KB | 206 chars |
| YAML | 15.17 µs | 16.13 µs | 13.89 µs | 39.3 / 36.8 KB | 226 chars |
| JSON | 3.48 µs | 2.42 µs | 0.99 µs | 15.8 / 8.5 KB | 255 chars |
| JSON5 | 3.53 µs | 2.58 µs | 1.14 µs | 15.9 / 8.5 KB | 259 chars |
| TOML | 3.81 µs | 3.48 µs | 2.00 µs | 16.1 / 7.1 KB | 238 chars |

### Nested config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 56.40 µs | 63.55 µs | 55.24 µs | 113.8 / 131.6 KB | 851 chars |
| JSON | 10.97 µs | 9.08 µs | 3.80 µs | 35.5 / 28.7 KB | 1,085 chars |
| JSON5 | 10.47 µs | 9.35 µs | 3.97 µs | 37.8 / 28.6 KB | 1,104 chars |
| TOML | 10.48 µs | 12.86 µs | 7.81 µs | 35.7 / 30.2 KB | 771 chars |

### Large config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 5.37 ms | 8.93 ms | 7.36 ms | 9.5 / 13.2 MB | 82,819 chars |
| JSON | 0.91 ms | 0.98 ms | 0.41 ms | 2.5 / 2.8 MB | 119,848 chars |
| JSON5 | 0.72 ms | 1.00 ms | 0.45 ms | 2.2 / 2.8 MB | 111,442 chars |
| TOML | 0.54 ms | 1.33 ms | 0.82 ms | 2.2 / 3.1 MB | 76,623 chars |

### Dotted config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 19.06 µs | 20.36 µs | 17.41 µs | 49.0 / 43.6 KB | 270 chars |
| JSON | 5.25 µs | 3.59 µs | 1.26 µs | 20.8 / 11.3 KB | 352 chars |
| JSON5 | 5.24 µs | 3.68 µs | 1.31 µs | 20.9 / 11.3 KB | 352 chars |
| TOML | 5.66 µs | 5.08 µs | 2.79 µs | 21.1 / 11.1 KB | 265 chars |

## Compared with `6207cbc`

| Benchmark | `6207cbc` | 3.1.0 | Change |
|---|---|---|---|
| Properties dump, flat | 3.18 µs | 3.56 µs | +12% |
| Properties load, flat | 4.23 µs | 4.18 µs | −1% |
| YAML dump, flat | 3.44 µs | 15.17 µs | 4.4× slower |
| YAML dump, nested | 11.19 µs | 56.40 µs | 5.0× slower |
| YAML dump, large | 0.75 ms | 5.37 ms | 7.2× slower |
| YAML load, flat | 16.20 µs | 16.13 µs | 0% |
| YAML load, nested | 63.79 µs | 63.55 µs | 0% |
| YAML load, large | 8.87 ms | 8.93 ms | +1% |
| TOML dump, flat | 4.83 µs | 3.81 µs | −21% |
| TOML dump, nested | 9.77 µs | 10.48 µs | +7% |
| TOML dump, large | 0.63 ms | 0.54 ms | −14% |
| TOML load, flat | 3.98 µs | 3.48 µs | −13% |
| TOML load, nested | 15.27 µs | 12.86 µs | −16% |
| TOML load, large | 1.55 ms | 1.33 ms | −14% |

The output differs between the versions: `6207cbc` wrote 220 chars of `.properties`, 241, 920 and
87,023 chars of YAML and 253, 869 and 78,217 chars of TOML (flat, nested, large).

## Notes

- **YAML dumping** is the one regression. `6207cbc` wrote YAML with a hand-written writer; 3.1.0
  builds a SnakeYAML node tree and lets SnakeYAML emit it, which handles quoting, escaping and
  indentation correctly but is slower, increasingly so for large configs.
- **YAML loading** is almost entirely SnakeYAML's parser (the parser column).
- **TOML** is written by `TomlValueWriter` in 3.1.0 and by Jackson's TOML generator in `6207cbc`.
  It is faster despite also writing the comments of options inside sections. Config objects and
  maps are written as tables, so the nested config's output is 11% smaller than `6207cbc`'s.
- **`.properties`** dumping escapes keys and values completely in 3.1.0, which `6207cbc` did not;
  that is the 12% slower dump.
- **JSON** loading takes 2.4 to 2.9 times the parser's time: Gson is fast, so the reflection and value
  conversion of AnnotationConfig make up most of a load.
- **JSON5** is written by `Json5ValueWriter` and dumps as fast as JSON, the large config 21% faster
  (0.72 against 0.91 ms) with 7% less output, as identifier keys are written without quotes. It is
  read by Gson in lenient mode, whose parser is up to 15% slower than for JSON (flat config); loads
  are within 7% of JSON's.
- **Dotted keys**: the dotted config's dump and load take 1.4 to 1.5 times as long as the flat
  config's, which has as many options, in JSON, JSON5 and TOML (TOML: 5.66 against 3.81 µs dump,
  5.08 against 3.48 µs load), and allocate 30-60% more. Dumping pays for `DottedResolver#boxTo`
  and the merging of boxed keys in core, loading for `DottedResolver#unbox` and for the parser
  building nested maps instead of one flat one (TOML's parser: 2.79 against 2.00 µs). In YAML both
  take about 1.25 times as long, mostly in SnakeYAML (its parser alone: 17.41 against 13.89 µs).
- **Big numbers in maps and lists** are kept exact instead of being turned into `double` and `int`.
  Gson and Jackson's TOML parser read decimals as `BigDecimal`; for `double` / `float` fields it is
  converted with `doubleValue()` / `floatValue()` rather than through its text, which kept the
  large config's JSON and TOML loads from getting slower than before the change.
