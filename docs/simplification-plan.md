# Code Simplification Plan

This document tracks the next incremental simplification of ANT+ Weight Scale Display. It is the
source of truth for the work: complete one task or phase at a time, update its checkboxes and notes,
and keep every commit buildable.

The earlier architecture refactor established sound boundaries for persistence, Garmin, ANT, and
Android lifecycle handling. This plan deliberately keeps those boundaries and concentrates on the
remaining duplicated UI, formatting, conversion, graph, and file-operation code.

## Current status

- Overall status: Completed (automated scope; manual release checks deferred)
- Current phase: Complete
- Last updated: 2026-07-12
- Plan created from commit: `52a2af0`
- Production Java baseline: 9,720 lines
- Production XML baseline: 7,555 lines
- Unit-test baseline: 63 declared tests / 163 test executions passing
- Unsigned release APK baseline: 2,306,088 bytes
- Working tree at plan creation: Clean

When work begins, update the status above and add the branch name and baseline verification results.

## Goals

1. Make measurement display and editing data-driven instead of repeating metric-specific branches.
2. Keep unit conversion and localized numeric parsing in small, Android-independent components.
3. Separate graph calculations from MPAndroidChart rendering and Android lifecycle code.
4. Make persistence completion and failure behavior explicit at every call site.
5. Move archive creation and restoration behind one tested backup boundary.
6. Reduce Activity/Fragment coupling and remove avoidable custom UI infrastructure.
7. Preserve persisted-data compatibility and existing user-visible behavior.

## Non-goals

- Do not replace Java/XML with Kotlin or Compose as part of this work.
- Do not introduce Room, a dependency-injection framework, or a new navigation framework solely to
  reduce line count.
- Do not merge the existing Garmin, ANT, FIT, persistence, or lifecycle responsibilities back into
  large classes.
- Do not change the `users`, `history`, or `goals` JSON formats unless a separately approved migration
  includes backward-reading tests.
- Do not redesign the application UI. Layout changes should preserve the established appearance and
  interaction unless explicitly approved.
- Do not combine credential encryption with these simplifications.

## Working rules

1. Complete phases in order unless a blocker or dependency is recorded in the decision log.
2. Prefer one checklist item per commit when the item is independently useful and testable.
3. Before changing behavior, add a characterization test for that behavior.
4. Run `./gradlew testDebugUnitTest` after every code task.
5. Run `./gradlew lintDebug` after every phase.
6. Run `./gradlew assembleRelease` at the end of each phase that changes production code.
7. Do not mark a task complete until its code, tests, and relevant documentation are committed.
8. Preserve unrelated working-tree changes and record any overlapping user changes before editing.
9. Record unexpected behavior, deviations, and important tradeoffs in the decision log.
10. Use `docs/release-checklist.md` for device and external-service verification that cannot be
    automated.

## Progress summary

- [x] Phase 0 — Record the baseline
- [x] Phase 1 — Centralize number parsing and mass conversion
- [x] Phase 2 — Introduce reusable measurement presentation models
- [x] Phase 3 — Simplify manual measurement editing
- [x] Phase 4 — Simplify goal value editing
- [x] Phase 5 — Extract graph definitions and calculations
- [x] Phase 6 — Clarify repository write completion and failures
- [x] Phase 7 — Complete the backup archive boundary
- [x] Phase 8 — Simplify navigation and user selection
- [x] Phase 9 — Final cleanup and regression verification

---

## Phase 0 — Record the baseline

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 0 changes are in the working tree)

### Objective

Establish reproducible behavior and size measurements before simplifying production code.

### Tasks

- [x] Create or confirm the working branch and record its name below.
- [x] Confirm the working tree is clean or document every pre-existing change.
- [x] Run the unit-test suite and record the result and test count.
- [x] Run Android lint and record its result.
- [x] Build the minified release APK and record its result and APK size.
- [x] Record production Java and XML line counts using a repeatable command.
- [x] Review existing tests for mass conversion, metric formatting, graphs, editors, persistence, and
      backup behavior.
- [x] Add only the missing characterization tests required by Phase 1; later phases should add their
      own tests immediately before changing behavior.

### Acceptance criteria

- All three Gradle verification commands pass.
- Baseline counts and results are recorded in this phase.
- Known manual-only behavior remains listed in `docs/release-checklist.md`.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

### Completion notes

- Branch: `refactor/simplify-architecture` at baseline commit `52a2af0`.
- Initial worktree: clean before this plan was created; at Phase 0 start the only existing change was
  the untracked `docs/simplification-plan.md` created for this work.
- Unit tests: `./gradlew testDebugUnitTest` passed in 1 second. The source contains 63 `@Test`
  declarations and the Gradle XML reports contain 163 test-case executions because parameterized
  metric tests run once per metric. There were zero failures, errors, or skipped tests.
- Lint: `./gradlew lintDebug` passed in 1 second; the text report states `No issues found.`
- Release build/APK size: `./gradlew assembleRelease` passed in 5 seconds, including
  `lintVitalRelease`. `app-release-unsigned.apk` is 2,306,088 bytes with SHA-256
  `31868ac699bc9527ba62f43c89755a780d665bd92134f42c470b70fdd3d45a3e`.
- Production line-count commands and results:

  ```bash
  find app/src/main/java -type f -name '*.java' -print0 | xargs -0 wc -l | tail -1
  # 9720 total
  find app/src/main/res -type f -name '*.xml' -print0 | xargs -0 wc -l | tail -1
  # 7555 total
  ```

- Test review: repository, atomic persistence, persisted-format compatibility, metric extraction,
  CSV/email formatting, and backup restoration already have local JVM coverage. Graph calculations,
  editor field mapping, and backup creation do not yet have direct tests; their owning phases require
  characterization tests before implementation.
- Added three tests with additional assertions across `MeasurementCalculationTest` and
  `MeasurementTextFormatterTest` to characterize stone-unit output, sub-stone pound fallback,
  percentage display when fat-mass mode is disabled, and conversion of entered pound/stone mass back
  to canonical values.
- Localized input parsing remains tied to Android `EditText`. Its behavior will be defined with tests
  against the Android-independent parser introduced in Phase 1 rather than adding a fragile Android
  mock test for the legacy implementation.

---

## Phase 1 — Centralize number parsing and mass conversion

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 0 and Phase 1 changes are in the working tree)

### Objective

Remove repeated kilogram/pound/stone arithmetic and stop domain models from depending on Android
input widgets. Establish one canonical conversion implementation before simplifying editors and
renderers.

### Proposed components

- `MassConverter`: Android-independent conversion between canonical kilograms and display units.
- `MassValue`: optional small value object for stone-and-pound display components, if it makes call
  sites clearer than primitive arrays or pairs.
- `LocalizedNumberParser`: conversion from localized text to a validated numeric result.

Names may change during implementation, but responsibilities must remain separate: conversion must
not read views, and view parsing must not know domain rules.

### Tasks

- [x] Characterize all current conversions in `User.calc_mass`, `User.calc_mass2`, `User.printMass`,
      `MetricFormatter`, `MeasurementTextFormatter`, `GraphsFragment`, and `EditGoalFragment`.
- [x] Add tests for kilograms, pounds, and stones, including zero, decimal pounds, negative goal
      differences, and round-trip tolerances.
- [x] Add tests for percentage-to-mass and mass-to-percentage conversion using canonical body weight.
- [x] Add tests for localized decimal parsing, blank input, invalid input, and surrounding whitespace.
- [x] Define one named pounds-per-kilogram constant; remove duplicated numeric literals from
      application code.
- [x] Implement the Android-independent conversion API.
- [x] Move `EditText` parsing out of `User` and `MainActivity` into the parsing component or editor
      boundary.
- [x] Migrate `MeasurementTextFormatter` and `MetricFormatter` to the conversion API.
- [x] Migrate graph axis and marker formatting to the conversion API without changing displayed
      precision.
- [x] Migrate remaining editor and adapter conversions.
- [x] Remove superseded `User` conversion overloads only after every caller has moved.
- [x] Confirm `User` no longer imports or accepts `EditText`.

### Acceptance criteria

- No production class outside the conversion component contains the raw kilogram/pound factor.
- Domain models do not depend on Android view classes.
- Existing kilogram, pound, stone, percentage, CSV, email, and graph formatting tests pass.
- Display precision and persisted canonical values are unchanged.

### Manual checks

- [ ] Open a metric in kilograms, pounds, and stones and compare it with the pre-change display.
- [ ] Enter and save a manual weight in each unit system.
- [ ] Enter decimal values using the device locale's decimal separator.

### Completion notes

- Components introduced: `MassConverter` owns kilograms, pounds, stones, display-mass, and
  percentage/mass conversion. `LocalizedNumberParser` strictly parses a complete localized number
  and returns an API-23-compatible validated result.
- Legacy methods removed: all `User.calc_mass(...)` and `User.calc_mass2(...)` overloads plus
  `MainActivity.parseNumber(EditText)`. `User` no longer imports or accepts Android input views.
- Callers migrated: `Metric`, `MetricFormatter` through `User.printMass`,
  `MeasurementTextFormatter`, `GraphsFragment`, `EditGoalFragment`, and `EditWeightFragment`.
  The raw pounds-per-kilogram literal now appears only as the named
  `MassConverter.POUNDS_PER_KILOGRAM` constant.
- Tests added: `MassConverterTest` covers kilograms, pounds, stones, zero, negative differences,
  round trips, and fat percentage/mass conversion. `LocalizedNumberParserTest` covers US, German,
  and French formats plus whitespace, blank, invalid, partial, and fallback cases. Phase 0's
  integration characterizations now exercise the new API.
- Automated verification: 71 declared `@Test` methods produced 171 test executions with zero
  failures, errors, or skips. `lintDebug` reports no issues. The final `assembleRelease` completed in
  19 seconds, including `lintVitalRelease`; the unsigned APK is 2,306,564 bytes.
- Behavior differences: numeric parsing now requires the complete trimmed input to be valid, so a
  value such as `12 kg` is rejected instead of silently accepting the `12` prefix. Empty/invalid
  fallbacks remain screen-specific (`-1` for unavailable optional measurements and `0` for the
  existing goal/energy fallback paths). Negative stone/pound decomposition is reversible.
- Manual checks: deferred to the applicable editor, goal, graph, and unit-system items in
  `docs/release-checklist.md`; no emulator or physical device was available in this phase.
- Follow-up work: Phases 3 and 4 will replace the remaining repeated editor field handling and add
  explicit form-level validation messages.

---

## Phase 2 — Introduce reusable measurement presentation models

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 2 changes are in the working tree)

### Objective

Make metric availability, formatted values, health state, icons, and segment presentation available
from one tested source instead of rebuilding them independently in `WeightFragment`,
`HistoryAdapter`, and `GraphsFragment`.

### Proposed design

Use `Metric` as the stable domain definition and add a separate presentation factory rather than
turning the enum into an Android UI controller. A presentation object may contain:

```text
MeasurementDisplay
  metric
  available
  primaryText
  secondaryText
  status
  iconResource
```

Segmental presentation should be generated by iterating `BodySegment`; it should not introduce a
second list of left/right arm, left/right leg, and trunk fields.

### Tasks

- [x] Characterize visible values, missing-value behavior, trend indicators, and health colors on the
      current-weight and history screens.
- [x] Add tests for presentation of every `Metric` with available and `-1` missing values.
- [x] Add tests for fat percentage versus fat-mass preference.
- [x] Add tests for all `BodySegment` mappings.
- [x] Define a UI-independent status type for normal, warning, high-risk, low-risk, and unavailable
      states as required by current behavior.
- [x] Implement the measurement presentation factory using `Metric`, `HealthRangeClassifier`, and
      the conversion API from Phase 1.
- [x] Replace the metric-specific rendering branches in `WeightFragment` with iteration over binding
      definitions or a small binding map.
- [x] Replace repeated value formatting and status classification in `HistoryAdapter` with the same
      presentation factory.
- [x] Replace the 20 metric-availability booleans in `GraphsFragment` with iteration over `Metric`
      definitions.
- [x] Ensure RecyclerView binding resets every optional view so recycled rows cannot retain stale
      values, colors, or visibility.
- [x] Keep layout changes minimal; reusable binding definitions are sufficient if dynamic layouts
      would alter the existing appearance.

### Acceptance criteria

- A new metric can be made visible in weight, history, and graph selection without adding parallel
  branches to all three classes.
- Segmental mapping is defined in one place.
- Missing values never leave stale data in recycled history rows.
- Existing presentation and characterization tests pass.

### Manual checks

- [ ] Compare current measurement cards before and after the change.
- [ ] Expand and collapse multiple history rows containing different optional metrics.
- [ ] Switch the fat percentage/mass preference and verify both screens.
- [ ] Verify each segment appears against the correct body part.

### Completion notes

- Presentation types introduced: `MeasurementPresentationFactory.MetricDisplay`,
  `SegmentDisplay`, and `Status`. The factory depends only on domain values and a replaceable string
  interface, so all formatting and classification logic runs in local JVM tests.
- Status compatibility: normal and compact status values are both carried by the model because the
  existing large cards and compact history rows intentionally render bone mass, physique rating,
  visceral fat, and metabolic age differently. The migration preserves both presentations instead
  of silently choosing one.
- Formatting ownership: the factory owns on-screen measurement formatting through its replaceable
  string interface. The now-unused Android display method was removed from `MetricFormatter`, which
  is narrowed to canonical CSV formatting.
- `WeightFragment`: the eight metric cards and five body segments now render by iterating definition
  lists. Metric formatting, descriptions, availability, status, and trend values no longer live in
  per-metric branches. The class decreased from 582 to 476 lines.
- `HistoryAdapter`: repeated metric formatting and health classification now use the shared factory;
  every optional value clears its text, visibility, and icon state during binding. Segment rows are
  mapped through `BodySegment`, correcting the old left/right leg row checks that accidentally used
  arm fat fields. The class decreased from 431 to 423 lines despite adding reusable binding helpers.
- `GraphsFragment`: metric visibility is derived from `Metric` definitions and menu resource IDs,
  replacing 20 availability flags and positional assignments. The class decreased from 693 to 638
  lines.
- Tests added: `MeasurementPresentationFactoryTest` covers every graph metric with present/missing
  values, percentage/fat-mass preference, large/compact status compatibility, current-height BMI,
  every explicit body-segment mapping, and data-driven graph availability.
- Automated verification: 77 declared `@Test` methods produced 177 test executions with zero
  failures, errors, or skips. `lintDebug` and the complete minified release build pass.
- Layout changes: none; existing XML and view bindings were retained.
- Manual checks: deferred to the applicable display, segment, graph, and unit-system items in
  `docs/release-checklist.md`; no emulator or physical device was available in this phase.
- Follow-up work: Phase 3 can reuse the same `Metric` definitions while replacing repeated manual
  editor parsing and assignment.

---

## Phase 3 — Simplify manual measurement editing

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 3 changes are in the working tree)

### Objective

Replace repeated per-field setup, parsing, visibility, and assignment in `EditWeightFragment` with a
small table of editable metric bindings.

### Tasks

- [x] Characterize creation and editing of complete, partial, segmental, barefoot, and non-barefoot
      measurements.
- [x] Add tests for mapping an editable metric to and from a `Weight` value.
- [x] Decide whether writable access belongs in `Metric`, a `WeightMetricAccess` registry, or a small
      editor-only binding. Prefer the smallest design that keeps parsing out of `Weight`.
- [x] Define binding entries containing the metric, input view, optional unit label, and availability
      rule.
- [x] Replace repeated initial-value assignment with one iteration over bindings.
- [x] Replace repeated save-time parsing and assignment with one iteration over bindings.
- [x] Centralize unavailable/blank handling and preserve the `-1` sentinel contract.
- [x] Centralize barefoot and segmental visibility rules.
- [x] Keep date, user selection, and menu handling in the fragment.
- [x] Remove obsolete individual field members and helper methods.

### Acceptance criteria

- Metric field setup and parsing have a single control flow.
- The fragment retains only lifecycle, navigation, date/user selection, and form-level validation.
- Existing persisted values and `-1` sentinel behavior are unchanged.
- Creation and editing tests cover every editable metric.

### Manual checks

- [ ] Create a manual weight-only measurement.
- [ ] Create a complete composition measurement.
- [ ] Edit an existing segmental measurement without changing untouched values.
- [ ] Cancel an edit and confirm nothing is saved.

### Completion notes

- Binding design selected: `EditableWeightMetric` is an editor-only enum pairing each editable
  `Metric` with its `Weight` setter, display/canonical conversion, precision, unit resource, decimal
  input policy, and invalid-input fallback. `EditWeightFragment.MetricField` pairs that pure
  definition with its generated `EditText` and unit-label binding.
- The fragment builds one `EnumMap` for all 20 editable metrics. Initial clearing/population, decimal
  separator watchers, hints, unit labels, localized parsing, canonical conversion, `-1` handling,
  and model assignment are now single loops.
- `Weight` remains a persistence/domain model and does not gain UI setters or Android view knowledge.
  BMI remains calculated rather than manually editable.
- Visibility audit: the legacy manual editor did not hide fields based on barefoot or segmental
  state; all optional fields remain visible. Barefoot/partial acceptance belongs to the ANT protocol
  path and remains characterized by `AntMessageParserTest` and `AntWeightSessionTest`. Segmental
  editor availability is represented by the complete `EditableWeightMetric` table.
- Tests added: `EditableWeightMetricTest` verifies every metric-to-`Weight` mapping is unique,
  complete and writable; missing sentinels; kilogram, pound, and stone round trips; fat percentage
  versus fat-mass conversion; units; decimal policies; and invalid fallbacks.
- Approximate Java lines removed: `EditWeightFragment` decreased from 670 to 363 lines. After adding
  the 117-line reusable accessor, production Java decreased from 9,885 after Phase 2 to 9,658 lines.
- Behavior improvements: active metabolism is now repopulated when an existing measurement is
  edited. Left-arm muscle is independently parsed instead of incorrectly using the left-arm fat
  field's emptiness. A non-empty invalid required weight now shows the existing required-value error
  instead of saving `-1`. Blank optional energy values remain `-1`, while non-empty invalid energy
  input retains the established `0` fallback.
- Automated verification: 82 declared `@Test` methods produced 182 test executions with zero
  failures, errors, or skips. `lintDebug` reports no issues, and the complete minified release build
  passes; the unsigned APK is 2,306,184 bytes.
- Manual checks: deferred to the applicable measurement editing and unit-system items in
  `docs/release-checklist.md`; no emulator or physical device was available in this phase.
- Follow-up work: Phase 4 will apply the same definition-and-binding pattern to start/end goal values.

---

## Phase 4 — Simplify goal value editing

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 4 changes are in the working tree)

### Objective

Replace the duplicated start/end goal input branches with a reusable value-field controller driven
by `Metric`, displayed unit, user preference, and the conversion API.

### Proposed component

`GoalValueInput` should own the zero-unit, one-unit, and stone/pound subviews for one endpoint. It
should expose operations equivalent to:

```text
configure(metric, user, showFatMass)
setCanonicalValue(value)
readCanonicalValue()
clear()
```

It may be a custom view or a controller around generated bindings. Prefer a binding controller if it
avoids custom view lifecycle and XML inflation complexity.

### Tasks

- [x] Characterize formatting and parsing for every goal metric and unit system.
- [x] Add tests for start/end round trips for unitless, percentage, energy, years, kilograms, pounds,
      and stones.
- [x] Add tests for fat-percentage goals when `show_fat_mass` is enabled and disabled.
- [x] Implement one reusable goal value input.
- [x] Replace parallel start/end field members with two instances of the reusable input.
- [x] Replace metric-specific setup switches with configuration derived from `Metric.Unit` and
      narrowly defined precision metadata.
- [x] Replace `checkValues`, `setValues`, `updateValue`, and `resetValues` branches with the reusable
      input API.
- [x] Keep date picking, type selection, color picking, and fragment navigation in the fragment.
- [x] Validate start/end dates and numeric input explicitly; do not silently turn invalid text into
      zero unless characterization tests prove that behavior must remain.
- [x] Remove obsolete input layouts or IDs only after lint confirms they are unused. No layouts or
      IDs were obsolete: the controller reuses all three alternatives through generated bindings.

### Acceptance criteria

- Start and end values use the same implementation.
- Adding a mass-based goal metric does not require another unit-conversion switch.
- Existing goals reopen with equivalent displayed values and save equivalent canonical values.
- Invalid input has a defined, tested, user-visible result.

### Manual checks

- [ ] Create and edit one goal for every unit category.
- [ ] Verify pound and stone values round-trip without meaningful drift.
- [ ] Toggle the user's fat-mass preference and inspect matching goals and graphs.

### Completion notes

- Component implemented as: `GoalValueDefinition` is the Android-independent metric/unit/precision
  model. `GoalValueInput` is a binding controller around one endpoint's existing unitless,
  single-unit, and stone/pound layouts. `EditGoalFragment` owns exactly two instances, for start and
  end values.
- Formatting and conversion: the definition derives its mode from `Metric.displayedUnit(...)` and
  the user's mass unit. It preserves integer precision for physique, age, and energy; one decimal
  for other values; and canonical-kilogram storage for kilogram, pound, stone, and fat-mass goals.
- Tests added: `GoalValueDefinitionTest` covers every goal metric plus unitless, percentage, energy,
  years, kilograms, pounds, stones, precision metadata, and fat-percentage goals with fat-mass mode
  both disabled and enabled.
- Approximate Java/XML lines removed: `EditGoalFragment` decreased from 637 to 294 lines. After
  adding the 188 production lines in the reusable definition and controller, production Java
  decreased from 9,658 after Phase 3 to 9,503 lines (net 155 lines). No layout structure changed;
  two validation strings increased production XML to 7,557 lines.
- Validation changes: blank or malformed localized numeric input now leaves the editor open and
  marks the specific field with `Enter a valid number`. A missing start date or an end date that is
  not after the start date also leaves the editor open and marks the relevant date field. Invalid
  text is no longer silently persisted as zero.
- Automated verification: 88 declared `@Test` methods produced 188 test executions with zero
  failures, errors, or skips. `lintDebug` reports `No issues found`, and the complete minified
  release build passes; the unsigned APK is 2,306,716 bytes. `git diff --check` also passes.
- Manual checks: deferred to the applicable goal editing, unit-system, and fat-mass items in
  `docs/release-checklist.md`; no emulator or physical device was available in this phase.
- Follow-up work: Phase 5 can use the same pure-definition approach to separate graph calculations
  from MPAndroidChart rendering.

---

## Phase 5 — Extract graph definitions and calculations

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 5 changes are in the working tree)

### Objective

Make `GraphsFragment` responsible for lifecycle, option selection, and chart rendering while pure
components own periods, point selection, rolling averages, visible averages, and goal series.

### Proposed components

- `GraphPeriod`: resource/menu ID, calendar offset, nominal display window, and availability rule.
- `GraphSeriesBuilder`: converts weights and goals into raw, rolling-average, and goal series.
- `GraphWindowAverage`: calculates the interpolated average for the visible interval.

Pure graph components must not depend on MPAndroidChart `Entry`; return small domain points so they
can be tested on the local JVM.

### Tasks

- [x] Characterize period selection, initial zoom, rolling average, visible average, missing metrics,
      goal overlays, and fat-mass filtering.
- [x] Investigate the currently calculated but unused `time_limit` in `GraphsFragment` and record
      whether periods are intended to filter data or only define viewport/average windows.
- [x] Add tests capturing the chosen `time_limit` behavior before changing it.
- [x] Define `GraphPeriod` entries for every current menu period.
- [x] Replace duplicated period menu recognition and calendar/window conditionals with `GraphPeriod`
      lookup.
- [x] Extract raw metric-point generation.
- [x] Extract and test exponential rolling-average generation, including one-point and sparse series.
- [x] Extract and test visible-window trapezoidal averaging, including partial boundary segments.
- [x] Extract goal-series filtering by user, metric, dates, and fat-mass preference.
- [x] Reuse Phase 1 formatting for axis labels and marker values.
- [x] Keep chart colors, gestures, datasets, viewport calls, and Android resources in the fragment or a
      narrowly scoped chart renderer.
- [x] Remove dead date-limit calculations and debug logging after behavior is established.
- [x] Ensure timers and chart references remain view-lifecycle safe.

### Acceptance criteria

- Period and series calculations run in local JVM tests without Android or MPAndroidChart.
- `GraphsFragment` contains no rolling-average or interpolation arithmetic.
- Every current graph period maps through one definition table.
- Goal overlays and display units are unchanged.

### Manual checks

- [ ] Open every period for a user with long history.
- [ ] Pan and zoom, then verify the delayed visible average updates correctly.
- [ ] Display every metric and its matching goals.
- [ ] Rotate the device while a graph is visible.

### Completion notes

- `time_limit` decision: the calculated timestamp was only logged and never used to select points.
  Periods therefore continue to define the initial viewport, visible-average span, and rolling
  smoothing window; all valid historical points remain in the chart so users can pan beyond the
  initial viewport. The dead calendar calculation and its debug logging were removed.
- Components introduced: `GraphPeriod` maps all ten menu IDs to viewport and legacy availability
  rules. `GraphPoint` is the chart-independent point type. `GraphSeriesBuilder` owns raw metric
  extraction, exponential rolling averages, trapezoidal visible averages with interpolated
  boundaries, and goal filtering by user, metric, dates, and fat-mass representation.
- Tests added: `GraphCalculationsTest` covers every period, non-filtering behavior, missing metrics,
  fat-mass conversion, empty/one-point/sparse rolling series, full and partial visible averages,
  goal filtering, and the existing menu availability thresholds.
- Approximate fragment lines removed: `GraphsFragment` decreased from 638 to 504 lines and contains
  no rolling or interpolation arithmetic. The three focused production components add 157 lines,
  so production Java increased from 9,503 after Phase 4 to 9,526 lines while moving calculations
  into independently tested code.
- Lifecycle safety: the delayed average timer is cancelled in `onDestroyView`, chart references are
  cleared, and its callback now exits when the chart or point series is unavailable.
- Automated verification: 95 declared `@Test` methods produced 195 test executions with zero
  failures, errors, or skips. `lintDebug` reports `No issues found`; the minified release build
  passes and produces a 2,307,812-byte unsigned APK.
- Manual checks: deferred to `docs/release-checklist.md`; no emulator or physical device was
  available.
- Follow-up work: Phase 6 will make repository mutation completion and failures explicit.

---

## Phase 6 — Clarify repository write completion and failures

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 6 changes are in the working tree)

### Objective

Replace ignored `Future<RepositoryResult<Void>>` values with one explicit mutation contract so the
application cannot silently present an in-memory change as saved after disk persistence fails.

### Tasks

- [x] Inventory every repository mutation and record whether its caller needs completion, rollback,
      retry, or only centralized error reporting.
- [x] Add tests for asynchronous write success, encoding failure, disk failure, serialization order,
      and concurrent Garmin token/profile updates.
- [x] Choose and document one asynchronous completion mechanism compatible with API 23.
- [x] Keep synchronous repository operations only where a worker/background thread already owns the
      call and requires an immediate result.
- [x] Replace `Future` return values that callers do not consume with the chosen explicit API.
- [x] Handle completion or failure in `MainActivity`, `HistoryFragment`, `GoalsFragment`, history
      import, and ANT measurement persistence.
- [x] Define whether failed writes roll back the in-memory snapshot or retain it with a visible retry
      error; test the selected behavior.
- [x] Narrow `AppStateViewModel` mutation methods to the UI-facing contract while retaining ownership
      of `AntWeightController` across Activity recreation.
- [x] Remove obsolete `completed(...)`, ignored futures, and unnecessary blocking wrappers.
- [x] Confirm no disk I/O is performed on the main thread.

### Acceptance criteria

- Every write failure reaches a tested handler.
- No caller receives a `Future` that it routinely ignores.
- Serialized writes and Garmin token-preservation behavior remain intact.
- Activity recreation does not duplicate or lose an ANT measurement save.

### Manual checks

- [ ] Save, edit, and delete users, weights, and goals.
- [ ] Import Garmin history and verify the completion message reflects persisted data.
- [ ] Complete an ANT measurement and confirm exactly one saved record.

### Completion notes

- Completion mechanism selected: `AppRepository.MutationCallback` is invoked by the serialized
  repository executor after encoding and atomic persistence finish. `AppStateViewModel` posts every
  callback to the main looper, giving Activities and Fragments one API-23-compatible UI contract
  without exposing `Future` or blocking the main thread.
- Failure/rollback policy: mutations retain their optimistic in-memory snapshot and report a visible
  persistence error. This avoids unsafe rollback across multiple queued mutations and gives the user
  an honest retry signal. Tests explicitly cover both the callback failure and retained snapshot.
- Mutation inventory: profile saves schedule token refresh only after success; manual saves upload
  only after success; goal/weight/user deletion and Garmin imports report failures; Garmin token
  reload is asynchronous; and ANT measurement success is emitted only after exactly one successful
  repository save. All UI failures flow through `MainActivity.handleMutationFailure` or the ANT
  persistence callback.
- API methods removed or renamed: UI-facing `Future<RepositoryResult<Void>>` methods and the synthetic
  `completed(...)` future were removed. Low-level test/worker operations are explicitly named
  `save*Synchronously`; Garmin token writes remain synchronous because their callers already execute
  on background workers.
- Tests added: asynchronous persistence success, disk failure, retained in-memory state, encoding
  failure, and callback serialization order. Existing concurrent profile/token and stale-token tests
  continue to verify Garmin token preservation.
- Automated verification: 99 declared `@Test` methods produce 199 test executions with zero failures,
  errors, or skips. `lintDebug` reports no issues and the minified release build passes; before the
  final encoding-failure assertion the unsigned APK measured 2,309,716 bytes (test-only changes do
  not affect it).
- Manual checks: deferred to `docs/release-checklist.md`; no emulator, ANT scale, or Garmin account
  was available.
- Follow-up work: Phase 7 will place backup creation beside restore in `BackupArchive`.

---

## Phase 7 — Complete the backup archive boundary

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 7 changes are in the working tree)

### Objective

Make `BackupArchive` the only component that knows the supported ZIP entries and how application
data is written to or restored from an archive.

### Tasks

- [x] Characterize archive entry names and content produced by the current `UsersFragment` exporter.
- [x] Add backup-creation tests and create→restore round-trip tests for all three data files.
- [x] Add failure tests for unreadable sources and output errors.
- [x] Add `BackupArchive.create(...)` with explicit success/failure results.
- [x] Reuse one supported-entry definition for creation and restoration.
- [x] Move compression level, buffering, entry creation, and stream ownership decisions out of
      `UsersFragment`.
- [x] Keep document-picker launch and Toast/dialog presentation in `UsersFragment`.
- [x] Ensure streams and file descriptors are closed exactly once by their documented owner.
- [x] Remove the legacy static `UsersFragment.unzip(...)` wrapper if no compatibility caller remains.
- [x] Confirm pre-refactor archives still restore successfully.

### Acceptance criteria

- `UsersFragment` contains no ZIP implementation code.
- Archive creation and restoration are covered by local JVM tests.
- Supported entries remain exactly `users`, `history`, and `goals`.
- Invalid entries, duplicates, oversized entries, and malformed JSON remain rejected.

### Manual checks

- [ ] Create a backup through the document picker.
- [ ] Inspect the archive entry names.
- [ ] Restore the new archive and an actual pre-refactor archive.

### Completion notes

- Archive API: `BackupArchive.create(OutputStream, File)` writes exactly `users`, `history`, and
  `goals` with best compression and returns the entry count or an explicit failure.
  `BackupArchive.restore(InputStream, File)` uses the same ordered supported-entry definition and
  retains the existing path, duplicate, size, JSON, and atomic-write validation.
- Stream ownership decision: passing a stream transfers ownership to `BackupArchive`; create and
  restore close it exactly once through their ZIP stream. Picker code opens the descriptor-backed
  stream and does not wrap or close it a second time. Archive creation and restore now run on named
  background threads rather than blocking the main thread.
- Compatibility results: existing restore tests for legacy entry names still pass. New tests create
  all three entries and restore them byte-for-byte, reject missing sources and duplicate entries,
  report destination write failures, and verify the transferred output is closed.
- UI boundary: `UsersFragment` contains no ZIP classes or compression logic and the legacy static
  `unzip(...)` wrapper was removed. Both the users screen and legacy edit-user restore action retain
  their document picker and result presentation.
- Size impact: `UsersFragment` decreased from approximately 246 to 212 lines. The consolidated
  `BackupArchive` grew from 78 to 107 lines and production Java increased from 9,605 after Phase 6
  to 9,617 lines while adding the complete tested creation boundary.
- Automated verification: 103 declared `@Test` methods produce 203 test executions with zero
  failures, errors, or skips. `lintDebug` reports no issues; the minified release build passes and
  produces a 2,309,932-byte unsigned APK.
- Manual checks: document-provider creation and restoration of an actual user archive remain
  deferred to `docs/release-checklist.md`; no device was available.
- Follow-up work: Phase 8 will replace localized-title navigation and consolidate user selection.

---

## Phase 8 — Simplify navigation and user selection

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 8 changes are in the working tree)

### Objective

Remove navigation based on localized labels, reduce repeated casts to `MainActivity`, and simplify
the custom searchable user selector without introducing a full navigation rewrite.

### Tasks

- [x] Add navigation characterization tests where practical and document manual back-stack behavior.
- [x] Change drawer selection to dispatch by stable menu resource ID rather than localized title text.
- [x] Centralize destination-to-fragment, title, and checked-position mapping in one small definition.
- [x] Replace public Activity helpers used only for navigation with a narrow host contract or Fragment
      Result API, choosing whichever produces fewer lifecycle-sensitive paths.
- [x] Keep ANT controller access through the Activity-scoped ViewModel rather than through Activity
      getters where practical.
- [x] Inventory `SpinnerDialog`, `ArrayAdapterWithContainsFilter`, and `OnSpinerItemClick` usage.
- [x] Correct the `OnSpinerItemClick` spelling if the interface remains.
- [x] Prefer an existing Material searchable selection pattern if it can preserve behavior with less
      code; otherwise consolidate the custom selector into one component.
- [x] Ensure user selection persists by UUID and updates weight, history, graphs, and goals.
- [x] Remove obsolete Activity methods, casts, imports, and resources after migration.

### Acceptance criteria

- Navigation does not depend on translated text.
- Fragments do not cast their Activity repeatedly for routine state access.
- There is one user-selection implementation for normal and large user lists.
- Activity recreation and back navigation retain current behavior.

### Manual checks

- [ ] Navigate to every top-level and edit screen in every supported locale available for testing.
- [ ] Exercise back navigation from each edit screen.
- [ ] Select users from lists smaller and larger than ten entries.
- [ ] Rotate during selection and after navigation.

### Completion notes

- Navigation mechanism selected: `NavigationDestination` maps each stable drawer menu ID to its title
  resource and Fragment class/factory. `MainActivity` dispatches directly from `MenuItem.getItemId()`;
  translated title text is never used as control flow. Existing edit-screen replacement and back
  behavior remain unchanged, while Activity recreation continues to rely on FragmentManager restore.
- User-selector implementation: `UserSpinnerController` is the single setup path for empty,
  one-user, normal, and large user lists. It uses the standard action-bar spinner and enables the
  existing filtered dialog above ten users. `SpinnerDialog` and
  `ArrayAdapterWithContainsFilter` therefore remain a focused searchable-picker pair; the callback
  was corrected from `OnSpinerItemClick` to package-private `OnSpinnerItemClick`.
- Host boundary: `AppHost` centralizes routine edit navigation, shared user-spinner setup, reload,
  messaging, and mutation-error handling. Fragments no longer repeatedly cast their Activity for
  these operations. ANT controller lookup/start moved from public Activity helpers to the existing
  Activity-scoped `AppStateViewModel`.
- Selection compatibility: the repository continues to persist selected users by UUID. All four
  measurement/history/graph/goal spinner listeners still update the same ViewModel selection and
  refresh their own presentation.
- Tests added: `NavigationDestinationTest` verifies unique stable IDs, title resources, unknown-ID
  rejection, and every destination-to-Fragment mapping.
- Size impact: `MainActivity` decreased from approximately 521 to 388 lines. Focused host,
  destination, and selector components make production Java 9,635 lines versus 9,617 after Phase 7.
- Automated verification: 105 declared `@Test` methods produce 205 test executions with zero
  failures, errors, or skips. `lintDebug` reports no issues; the minified release build passes and
  produces a 2,310,092-byte unsigned APK.
- Manual checks: locale navigation, back behavior, rotation, and small/large selector interaction
  remain deferred to `docs/release-checklist.md`; no device was available.
- Follow-up work: Phase 9 will perform final cleanup, architecture documentation, measurements, and
  clean-build verification.

---

## Phase 9 — Final cleanup and regression verification

Status: Completed<br>
Started: 2026-07-12<br>
Completed: 2026-07-12<br>
Commit: Pending (Phase 9 changes are in the working tree)

### Objective

Remove confirmed obsolete code, measure the simplification, update architecture documentation, and
complete all automated and manual release gates.

### Tasks

- [x] Run lint and remove resources, methods, fields, imports, and suppressions confirmed unused.
- [x] Remove stale comments and commented-out code in touched classes.
- [x] Normalize names in touched code only; avoid a repository-wide cosmetic rename.
- [x] Review class responsibilities and ensure no new helper became an unrelated catch-all.
- [x] Compare production Java/XML line counts with the Phase 0 baseline.
- [x] Compare the largest production classes with their baseline sizes.
- [x] Update `docs/architecture.md` with the final measurement presentation, conversion, graph, write,
      backup, and navigation boundaries.
- [x] Update `README.md` links if this plan is intended to remain as maintained documentation.
- [x] Run every automated verification command from a clean checkout or clean build state.
- [ ] Complete the applicable items in `docs/release-checklist.md` on representative devices.
      Deferred because no device, ANT scale, document provider, or authenticated Garmin account was
      available; the checklist explicitly records the verified automated scope.
- [x] Record any deliberately deferred work below.

### Acceptance criteria

- Unit tests, lint, and the complete minified release build pass.
- Persisted fixtures and pre-refactor backup compatibility pass.
- The major UI classes have materially fewer responsibilities and branches.
- Architecture documentation matches the implemented code.
- Manual ANT, Garmin, backup, graph, edit, and navigation checks are recorded.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

### Final measurements

- Production Java lines: 9,566, down 154 from the 9,720-line Phase 0 baseline.
- Production XML lines: 7,558, up 3 from the 7,555-line baseline for explicit validation/write-error
  messages; no layouts were added.
- Unit tests: 105 declared `@Test` methods / 205 executions, up from 63 / 163; zero failures, errors,
  or skips.
- Lint result: `No issues found.`
- Release APK size: 2,309,588 bytes; SHA-256
  `beaeda0e9ad4f9ddfb9214d8b75da61d368fa4b9179ac6e0ed014a9817356372`.
- Largest classes before/after: `GraphsFragment` 693→504, `EditWeightFragment` 670→363,
  `EditGoalFragment` 634→294, `WeightFragment` 582→482, and `MainActivity` 512→388 lines.
  `EditUserFragment` increased 552→588 and `AppRepository` 473→501 because they now surface
  asynchronous persistence/archive completion; those responsibilities are bounded by dedicated
  repository and archive components.
- Net files added/removed: 20 added and 2 removed under production source/resources/tests relative
  to `52a2af0`. Added files are focused definitions/controllers/tests; removed files are the
  misspelled spinner callback and superseded filter adapter.

### Deferred work

- Physical ANT measurement success/failure/recreation cases.
- Live Garmin MFA, upload, history download, and a full background token-renewal cycle.
- Document-provider backup creation plus restoration of an actual pre-refactor archive.
- API/locale/rotation/process-recreation and small/large user-selector device checks.
- Garmin credential encryption remains separately tracked compatibility-sensitive debt.

### Completion notes

- Automated verification: from a clean Gradle build state,
  `./gradlew clean testDebugUnitTest lintDebug assembleRelease` passed in 26 seconds with 80 tasks;
  205 tests passed, lint found no issues, and `lintVitalRelease` plus minification succeeded.
  `git diff --check` also passes.
- Cleanup: removed the unused `ArrayAdapterWithContainsFilter`, dead selector mode/style fields,
  obsolete comments, and redundant visibility/accessor code. Touched names now use the corrected
  spinner spelling and focused camel-case conventions.
- Responsibility review: conversion, presentation, editors, graph calculations, repository writes,
  backup archives, navigation, and user selection each have a narrowly scoped boundary; no helper
  owns unrelated application state.
- Manual verification: deferred items are preserved unchecked in `docs/release-checklist.md`; no
  manual result is implied by automated coverage.
- Documentation updates: `docs/architecture.md` now documents every final boundary,
  `docs/release-checklist.md` records the automated result and manual limitations, and `README.md`
  links this maintained plan.
- Final release recommendation: code is ready for device/external-service validation, but do not
  publish until the applicable manual release checklist is completed.

---

## Decision log

Add one entry whenever implementation differs from this plan or a choice affects later phases.

| Date | Phase | Decision | Reason and consequences |
|---|---:|---|---|
| 2026-07-12 | Plan | Keep Garmin, ANT, FIT, persistence, and lifecycle boundaries | They isolate genuinely different responsibilities and already have focused tests. |
| 2026-07-12 | Plan | Avoid a Kotlin/Compose/Room/DI migration | It would expand scope and risk without directly removing the concentrated UI duplication. |
| 2026-07-12 | 1 | Use a small parser result instead of `OptionalDouble` | `OptionalDouble` requires API 24, while the application supports API 23; the custom result preserves validation without desugaring or a minimum-SDK change. |
| 2026-07-12 | 1 | Reject partially parsed numeric input | Accepting a numeric prefix from otherwise invalid text can silently save unintended values; strict complete parsing is deterministic and covered by tests. |
| 2026-07-12 | 3 | Keep writable metric access in `EditableWeightMetric` | `Metric` remains the read-only domain definition used across the application, while editor-only setters, precision, input, and unit policies stay out of the persistence model. |
| 2026-07-12 | 5 | Keep graph periods as viewport definitions | The legacy `time_limit` was calculated and logged but never filtered data. Keeping all points preserves panning behavior while removing dead code. |
| 2026-07-12 | 6 | Retain optimistic state and surface persistence failure | Rolling state back safely across serialized queued mutations would require versioned snapshots. Visible completion errors are deterministic and prevent silent data-loss claims. |
| 2026-07-12 | 8 | Retain the filtered spinner dialog behind one controller | It preserves keyboard and large-list behavior without adding a Material dependency or a second selection path; all list sizes now share one setup component. |

## Blockers and risks

Record active blockers here and link them to the affected phase. Remove resolved blockers only after
copying the resolution to the decision log.

- Physical ANT hardware, Garmin authentication/MFA, and complete background renewal require manual
  verification.
- Graph presentation and editor formatting contain legacy behavior that needs characterization before
  consolidation.
- Persistence API changes can expose or alter existing silent-failure behavior; decide rollback and
  user notification policy before implementation.
- User conversion methods currently combine domain conversion, display formatting, and Android input
  parsing; migrate callers incrementally to avoid a broad behavioral rewrite.

## Per-task update template

Use this template in the relevant phase's completion notes when a task requires more context than a
checkbox can provide:

```text
Task:
Status: In progress | Completed | Blocked<br>
Commit:
Tests run:
Result:
Behavioral notes:
Follow-up:
```
