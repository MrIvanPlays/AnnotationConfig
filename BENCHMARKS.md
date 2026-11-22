# Benchmarks

Dump and load times of AnnotationConfig 3.1.0 (commit `2b2a4a5`), and a comparison with commit
`6207cbc` (3.0.4-SNAPSHOT, right after the 3.0.3 release). Lower is better.

## Setup

- AMD Ryzen 7 7730U (8 cores, 16 threads), 15 GB RAM, Windows 11, OpenJDK 25.0.4.1 (25.0.4 for the
  comparison with `6207cbc`), `-Xms1g -Xmx1g`
- Libraries: SnakeYAML 2.7, Gson 2.15.0, Jackson TOML 2.23.0 (`6207cbc`: Jackson 2.22.4)
- Hand-written harness, not JMH: every benchmark runs in its own JVM, is warmed up for 3 seconds,
  then measured in 10 rounds of at least 100 ms each. The tables show the median time per
  operation; allocation is measured per thread.
- **Dump** writes a config object to an in-memory `Writer`, **load** reads one from an in-memory
  `Reader` into a new object. **Parser** is the format's library alone reading the same text into
  a `Map` (`java.util.Properties`, SnakeYAML, Gson, Gson in lenient mode for JSON5, Jackson's
  `TomlMapper`): the part of a load that is not AnnotationConfig.
- Every benchmark ran twice, in two passes in alternating order. Benchmarks whose passes differed by
  more than 10% ran twice more; the tables show the median of all runs.
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

Measured at commit `2b2a4a5`, all formats in one run.

### Flat config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| Properties | 3.56 µs | 4.29 µs | 2.80 µs | 16.0 / 24.9 KB | 231 chars |
| YAML | 16.75 µs | 16.41 µs | 14.48 µs | 39.3 / 36.8 KB | 226 chars |
| JSON | 3.66 µs | 2.64 µs | 1.02 µs | 15.8 / 8.8 KB | 255 chars |
| JSON5 | 3.48 µs | 2.96 µs | 1.17 µs | 15.9 / 8.8 KB | 259 chars |
| TOML | 3.96 µs | 3.88 µs | 2.27 µs | 16.2 / 7.4 KB | 263 chars |

### Nested config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 56.45 µs | 67.19 µs | 60.18 µs | 113.8 / 131.9 KB | 851 chars |
| JSON | 11.65 µs | 10.32 µs | 3.70 µs | 35.5 / 29.5 KB | 1,085 chars |
| JSON5 | 11.14 µs | 10.46 µs | 4.29 µs | 37.8 / 29.4 KB | 1,104 chars |
| TOML | 10.74 µs | 14.82 µs | 9.49 µs | 37.7 / 31.5 KB | 918 chars |

### Large config

| Format | Dump | Load | Parser | Allocated (dump / load) | Output |
|---|---|---|---|---|---|
| YAML | 5.34 ms | 7.83 ms | 7.18 ms | 9.5 / 13.2 MB | 82,819 chars |
| JSON | 0.90 ms | 1.08 ms | 0.42 ms | 2.5 / 3.0 MB | 119,848 chars |
| JSON5 | 0.72 ms | 1.13 ms | 0.47 ms | 2.2 / 3.0 MB | 111,442 chars |
| TOML | 0.54 ms | 1.54 ms | 0.90 ms | 2.2 / 3.3 MB | 78,417 chars |

## Compared with `6207cbc`

Measured in an earlier run, at commit `92f17a8`, with OpenJDK 25.0.4. The results above are within
~10% of its 3.1.0 column, except TOML's flat dump: 3.96 µs above against 4.47 µs here.

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
- **JSON5** is written by `Json5ValueWriter` and dumps as fast as JSON, the large config 20% faster
  (0.72 against 0.90 ms) with 7% less output, as identifier keys are written without quotes. It is
  read by Gson in lenient mode, whose parser is 13-16% slower than for JSON; a load takes up to 12%
  longer than JSON's (flat config), the other configs within 5%.
- **Big numbers in maps and lists** (commit `2b2a4a5`) are kept exact instead of being turned into
  `double` and `int`. Against the commit before (`bdcacc3`), every dump and load is within 5%,
  except two. The large config's JSON and TOML loads were 6% slower, with 2% and 5% more
  allocation: Gson and Jackson's TOML parser read decimals as `BigDecimal`, which then reached
  `double` fields of objects in lists as `BigDecimal` and were converted through their text. A
  `BigDecimal` is now converted with `doubleValue()` / `floatValue()` (correctly rounded since
  Java 21, so the values are unchanged). That brings the large TOML load back to `bdcacc3`'s
  allocation and the large JSON load below it (3,214 and 2,894 KB); in a noisier run of five
  rounds each, the large JSON, JSON5 and TOML loads took 11%, 13% and 7% less time than before
  the change.
  The large config's TOML dump measured 12% faster, but `bdcacc3`'s two runs of it differed by 8%,
  and the writer did not change.
- **`\n` line endings** (commit `c7b1bd8`): the TOML and `.properties` writers end lines with `\n`
  instead of the OS line separator. Their dumps are within 4% of `2b2a4a5` with the same
  allocation. On Windows their output is one character shorter per line: 238, 889 and 78,213 chars
  for TOML and 206 for `.properties`, against the 263, 918, 78,417 and 231 in the tables above.
- **`.properties`** dumping escapes keys and values completely in 3.1.0, which `6207cbc` did not.
