# Repository performance gates

Whole-file JSON remains appropriate only while representative data stays inside these release
budgets. Measure a minified build on a supported low-memory API 23–28 device, with fixed fixtures,
five warm-ups, and 30 recorded repetitions. Report median and p95; do not use host JVM timings as
release evidence.

| Operation | 10,000 weights + proportional goals | 50,000 weights + proportional goals |
|---|---:|---:|
| Cold repository reload | p95 ≤ 500 ms | p95 ≤ 2,500 ms |
| Add/edit/delete committed callback | p95 ≤ 750 ms | p95 ≤ 4,000 ms |
| Batched history import commit | p95 ≤ 1,000 ms | p95 ≤ 5,000 ms |
| Selected-profile snapshot | p95 ≤ 16 ms | p95 ≤ 16 ms |
| Additional peak heap during any case | ≤ 24 MiB | ≤ 64 MiB |

Record encoded bytes and bytes written as diagnostics. Any schema change must still round-trip the
compatibility fixtures byte-for-field, and failure-injection/token-merge tests must pass. If the
50,000-record gates fail on the release device, profile before changing storage; consider a record
store only when full-file encoding or atomic replacement is the measured bottleneck.

`RepositoryScaleCharacterizationTest` exercises 100, 1,000, 10,000, and 50,000 weights (plus
proportional goals) on the host and prints write/read time and encoded bytes. It is a repeatable
regression fixture, not a substitute for the device protocol above.

One non-warmed host characterization on 2026-08-11 produced:

| Weights | Write | Read | Encoded bytes |
|---:|---:|---:|---:|
| 100 | 29.930 ms | 8.119 ms | 11,802 |
| 1,000 | 26.363 ms | 11.391 ms | 118,002 |
| 10,000 | 47.259 ms | 38.914 ms | 1,180,002 |
| 50,000 | 163.182 ms | 136.114 ms | 5,900,002 |

These values confirm the fixture executes at every planned scale; they are not p95 results and do
not satisfy the release-device gate.
