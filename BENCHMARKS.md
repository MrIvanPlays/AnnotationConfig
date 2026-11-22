# Benchmarks

Dump and load times of AnnotationConfig 3.1.0 (commit `92f17a8`), compared with commit `6207cbc`
(3.0.4-SNAPSHOT, right after the 3.0.3 release). Lower is better.

## Setup

- AMD Ryzen 7 7730U (8 cores, 16 threads), 15 GB RAM, Windows 11, OpenJDK 25.0.4, `-Xms1g -Xmx1g`
- Libraries: SnakeYAML 2.7, Gson 2.15.0, Jackson TOML 2.23.0 (`6207cbc`: Jackson 2.22.4)
- Hand-written harness, not JMH: every benchmark runs in its own JVM, is warmed up for 3 seconds,
  then measured in 10 rounds of at least 100 ms each. The tables show the median time per
  operation; allocation is measured per thread.
- **Dump** writes a config object to an in-memory `Writer`, **load** reads one from an in-memory
  `Reader` into a new object. **Parser** is the format's library alone reading the same text into
  a `Map` (`java.util.Properties`, SnakeYAML, Gson, Jackson's `TomlMapper`): the part of a load that
  is not AnnotationConfig.
- These are warmed-up numbers. An application usually loads its config once at startup, before the
  JIT has compiled anything; that first call is dominated by class loading and is not measured.
- Differences below ~10% are within noise, for the large YAML config up to ~20%.

### Configs

- **Flat**: 12 scalar options (strings, ints, a long, doubles, booleans), two with comments.
- **Nested**: two `@ConfigObject` sections, one with a nested section, comments, a list of strings,
  a list of 5 objects and a map with 10 entries.
- **Large**: a list of 1000 objects with 4 fields each, a map with 200 entries and an `int[]` of
  1000 numbers.

`.properties` only supports flat options, so it is only measured with the flat config. The JSON
module did not exist at `6207cbc`.

## Results

### Flat config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| Properties | 3.58 µs | 4.33 µs | 2.67 µs | 16.0 / 24.9 KB | 231 chars |
| YAML | 15.48 µs | 17.33 µs | 15.31 µs | 39.3 / 36.8 KB | 226 chars |
| JSON | 3.78 µs | 2.82 µs | 1.16 µs | 15.9 / 8.8 KB | 255 chars |
| TOML | 4.47 µs | 4.09 µs | 2.24 µs | 16.2 / 7.5 KB | 263 chars |

### Nested config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 55.49 µs | 63.58 µs | 55.84 µs | 113.8 / 129.2 KB | 851 chars |
| JSON | 11.06 µs | 9.63 µs | 3.54 µs | 35.5 / 29.3 KB | 1,085 chars |
| TOML | 10.31 µs | 14.16 µs | 9.01 µs | 37.7 / 30.8 KB | 918 chars |

### Large config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 5.74 ms | 7.85 ms | 7.30 ms | 9.5 / 13.2 MB | 82,819 chars |
| JSON | 0.90 ms | 1.05 ms | 0.41 ms | 2.5 / 2.9 MB | 119,848 chars |
| TOML | 0.53 ms | 1.45 ms | 0.91 ms | 2.2 / 3.1 MB | 78,417 chars |

## Compared with `6207cbc`

| Benchmark | `6207cbc` | 3.1.0 | Change |
|---|---|---|---|
| Properties dump, flat | 3.33 µs | 3.58 µs | +7% |
| Properties load, flat | 4.65 µs | 4.33 µs | −7% |
| YAML dump, flat | 3.73 µs | 15.48 µs | 4.2× slower |
| YAML dump, nested | 12.16 µs | 55.49 µs | 4.6× slower |
| YAML dump, large | 0.77 ms | 5.74 ms | 7.5× slower |
| YAML load, flat | 16.90 µs | 17.33 µs | +3% |
| YAML load, nested | 65.54 µs | 63.58 µs | −3% |
| YAML load, large | 8.78 ms | 7.85 ms | −11% |
| TOML dump, flat | 5.05 µs | 4.47 µs | −11% |
| TOML dump, nested | 9.59 µs | 10.31 µs | +8% |
| TOML dump, large | 0.62 ms | 0.53 ms | −15% |
| TOML load, flat | 4.37 µs | 4.09 µs | −6% |
| TOML load, nested | 15.91 µs | 14.16 µs | −11% |
| TOML load, large | 1.60 ms | 1.45 ms | −9% |

## Notes

- **YAML dumping** is the one regression. `6207cbc` wrote YAML with a hand-written writer; 3.1.0
  builds a SnakeYAML node tree and lets SnakeYAML emit it, which handles quoting, escaping and
  indentation correctly but is slower, increasingly so for large configs.
- **YAML loading** is almost entirely SnakeYAML's parser (the parser column).
- **TOML** is written by `TomlValueWriter` in 3.1.0 and by Jackson's TOML generator in `6207cbc`.
  It is faster despite now also writing the comments of options inside sections; the nested
  config's output is 6% larger because of them.
- **JSON** loading is about 2.5 times the parser's time: Gson is fast, so the reflection and value
  conversion of AnnotationConfig make up most of a load.
- **`.properties`** dumping escapes keys and values completely in 3.1.0, which `6207cbc` did not.
