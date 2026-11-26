# Changelog

Versions follow this rule: a bug or security fix increments `0.0.X`, and new functionality
or new platform support increments `0.X.0`.

## [0.3.0] - 2026-11-26

- Exact, translation-invariant `PatternClassifier` backed by `HashLife` on an unbounded plane:
  classifies any finite pattern under any non-`B0` life-like rule as `Extinct`, `StillLife`,
  `Oscillator`, `Spaceship` (with exact period, `(dx, dy)` displacement, and reduced velocity such
  as `c/4 diagonal` or `c/2 orthogonal`), or `Unsettled`.
- New CLI command `silica classify [--pattern NAME | --rle FILE] [--rule RULE] [--max-generations N] [--max-cells N] [--json]`.
- Expanded built-in `Patterns` catalog with `BEACON`, `TOAD`, `LWSS` (lightweight spaceship),
  `PENTADECATHLON` (period-15 oscillator), and HighLife `REPLICATOR`.
- Upgraded Gradle wrapper to `9.9.2`, `actions/setup-java` to `6.3.0`, and enabled automatic
  `main` push & pull-request CI and repository guard triggers.

## [0.2.0] - 2026-11-20

- Every Life engine now runs any life-like `B/S` rule. `BitLife` keeps its Conway shortcut and
  evaluates other rules with a four-bit bit-sliced neighbor count. `HashLife` takes a rule too,
  and rejects `B0` rules, which an unbounded empty plane cannot represent.
- `Rule.notation` gives canonical `B/S` text, `Rule.parse` also accepts the older `S/B` form such
  as `23/3`, and masks outside nine bits are rejected.
- RLE import and export: `Rle.read` returns the header size, rule, name and comments, and
  `Rle.write` produces canonical, line-wrapped RLE. Run lengths, coordinates and the number of
  live cells are bounded, so hostile files fail with a clear error.
- Hashlife queries: `advance(BigInteger)` for any generation count, `boundingBox()`,
  `populationIn(...)` for exact rectangle counts beyond 64 bits, `cells()` and `toRle()`.
- Acorn and diehard join the built-in patterns, with tests against their published lifetimes.
- Command line: `--rle`, `--rule`, `--out`, `--edge` and `--json` options, Hashlife runs by any
  generation count with `--every` sampling, and new `rle` and `bench` commands. Unknown options
  are now rejected.

## 0.1.0

First release.

- Conway's Game of Life and any life-like rule, with double-buffered and bit-parallel engines.
- Hashlife, for advancing patterns across astronomically long spans of time.
- A falling-sand world with sand, water, oil, acid, stone, wood, fire, smoke and steam.
- Chunk sleeping that is proven not to change the outcome of a simulation.
- A command line and a desktop window, with no dependencies beyond the JDK.
