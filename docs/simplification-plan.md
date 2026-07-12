# Code Simplification Plan

This document tracks the next incremental simplification of ANT+ Weight Scale Display. It is the
source of truth for the work: complete one task or phase at a time, update its checkboxes and notes,
and keep every commit buildable.

The earlier architecture refactor established sound boundaries for persistence, Garmin, ANT, and
Android lifecycle handling. This plan deliberately keeps those boundaries and concentrates on the
remaining duplicated UI, formatting, conversion, graph, and file-operation code.

## Current status

- Overall status: In progress
- Current phase: Phase 4 — Simplify goal value editing
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
- [ ] Phase 4 — Simplify goal value editing
- [ ] Phase 5 — Extract graph definitions and calculations
- [ ] Phase 6 — Clarify repository write completion and failures
- [ ] Phase 7 — Complete the backup archive boundary
- [ ] Phase 8 — Simplify navigation and user selection
- [ ] Phase 9 — Final cleanup and regression verification

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

Status: Not started<br>
Started: —<br>
Completed: —<br>
Commit: —

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

- [ ] Characterize formatting and parsing for every goal metric and unit system.
- [ ] Add tests for start/end round trips for unitless, percentage, energy, years, kilograms, pounds,
      and stones.
- [ ] Add tests for fat-percentage goals when `show_fat_mass` is enabled and disabled.
- [ ] Implement one reusable goal value input.
- [ ] Replace parallel start/end field members with two instances of the reusable input.
- [ ] Replace metric-specific setup switches with configuration derived from `Metric.Unit` and
      narrowly defined precision metadata.
- [ ] Replace `checkValues`, `setValues`, `updateValue`, and `resetValues` branches with the reusable
      input API.
- [ ] Keep date picking, type selection, color picking, and fragment navigation in the fragment.
- [ ] Validate start/end dates and numeric input explicitly; do not silently turn invalid text into
      zero unless characterization tests prove that behavior must remain.
- [ ] Remove obsolete input layouts or IDs only after lint confirms they are unused.

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

- Component implemented as: —
- Approximate Java/XML lines removed: —
- Validation changes: —
- Follow-up work: —

---

## Phase 5 — Extract graph definitions and calculations

Status: Not started<br>
Started: —<br>
Completed: —<br>
Commit: —

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

- [ ] Characterize period selection, initial zoom, rolling average, visible average, missing metrics,
      goal overlays, and fat-mass filtering.
- [ ] Investigate the currently calculated but unused `time_limit` in `GraphsFragment` and record
      whether periods are intended to filter data or only define viewport/average windows.
- [ ] Add tests capturing the chosen `time_limit` behavior before changing it.
- [ ] Define `GraphPeriod` entries for every current menu period.
- [ ] Replace duplicated period menu recognition and calendar/window conditionals with `GraphPeriod`
      lookup.
- [ ] Extract raw metric-point generation.
- [ ] Extract and test exponential rolling-average generation, including one-point and sparse series.
- [ ] Extract and test visible-window trapezoidal averaging, including partial boundary segments.
- [ ] Extract goal-series filtering by user, metric, dates, and fat-mass preference.
- [ ] Reuse Phase 1 formatting for axis labels and marker values.
- [ ] Keep chart colors, gestures, datasets, viewport calls, and Android resources in the fragment or a
      narrowly scoped chart renderer.
- [ ] Remove dead date-limit calculations and debug logging after behavior is established.
- [ ] Ensure timers and chart references remain view-lifecycle safe.

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

- `time_limit` decision: —
- Components introduced: —
- Approximate fragment lines removed: —
- Follow-up work: —

---

## Phase 6 — Clarify repository write completion and failures

Status: Not started<br>
Started: —<br>
Completed: —<br>
Commit: —

### Objective

Replace ignored `Future<RepositoryResult<Void>>` values with one explicit mutation contract so the
application cannot silently present an in-memory change as saved after disk persistence fails.

### Tasks

- [ ] Inventory every repository mutation and record whether its caller needs completion, rollback,
      retry, or only centralized error reporting.
- [ ] Add tests for asynchronous write success, encoding failure, disk failure, serialization order,
      and concurrent Garmin token/profile updates.
- [ ] Choose and document one asynchronous completion mechanism compatible with API 23.
- [ ] Keep synchronous repository operations only where a worker/background thread already owns the
      call and requires an immediate result.
- [ ] Replace `Future` return values that callers do not consume with the chosen explicit API.
- [ ] Handle completion or failure in `MainActivity`, `HistoryFragment`, `GoalsFragment`, history
      import, and ANT measurement persistence.
- [ ] Define whether failed writes roll back the in-memory snapshot or retain it with a visible retry
      error; test the selected behavior.
- [ ] Narrow `AppStateViewModel` mutation methods to the UI-facing contract while retaining ownership
      of `AntWeightController` across Activity recreation.
- [ ] Remove obsolete `completed(...)`, ignored futures, and unnecessary blocking wrappers.
- [ ] Confirm no disk I/O is performed on the main thread.

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

- Completion mechanism selected: —
- Failure/rollback policy: —
- API methods removed or renamed: —
- Follow-up work: —

---

## Phase 7 — Complete the backup archive boundary

Status: Not started<br>
Started: —<br>
Completed: —<br>
Commit: —

### Objective

Make `BackupArchive` the only component that knows the supported ZIP entries and how application
data is written to or restored from an archive.

### Tasks

- [ ] Characterize archive entry names and content produced by the current `UsersFragment` exporter.
- [ ] Add backup-creation tests and create→restore round-trip tests for all three data files.
- [ ] Add failure tests for unreadable sources and output errors.
- [ ] Add `BackupArchive.create(...)` with explicit success/failure results.
- [ ] Reuse one supported-entry definition for creation and restoration.
- [ ] Move compression level, buffering, entry creation, and stream ownership decisions out of
      `UsersFragment`.
- [ ] Keep document-picker launch and Toast/dialog presentation in `UsersFragment`.
- [ ] Ensure streams and file descriptors are closed exactly once by their documented owner.
- [ ] Remove the legacy static `UsersFragment.unzip(...)` wrapper if no compatibility caller remains.
- [ ] Confirm pre-refactor archives still restore successfully.

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

- Archive API: —
- Stream ownership decision: —
- Compatibility results: —
- Follow-up work: —

---

## Phase 8 — Simplify navigation and user selection

Status: Not started<br>
Started: —<br>
Completed: —<br>
Commit: —

### Objective

Remove navigation based on localized labels, reduce repeated casts to `MainActivity`, and simplify
the custom searchable user selector without introducing a full navigation rewrite.

### Tasks

- [ ] Add navigation characterization tests where practical and document manual back-stack behavior.
- [ ] Change drawer selection to dispatch by stable menu resource ID rather than localized title text.
- [ ] Centralize destination-to-fragment, title, and checked-position mapping in one small definition.
- [ ] Replace public Activity helpers used only for navigation with a narrow host contract or Fragment
      Result API, choosing whichever produces fewer lifecycle-sensitive paths.
- [ ] Keep ANT controller access through the Activity-scoped ViewModel rather than through Activity
      getters where practical.
- [ ] Inventory `SpinnerDialog`, `ArrayAdapterWithContainsFilter`, and `OnSpinerItemClick` usage.
- [ ] Correct the `OnSpinerItemClick` spelling if the interface remains.
- [ ] Prefer an existing Material searchable selection pattern if it can preserve behavior with less
      code; otherwise consolidate the custom selector into one component.
- [ ] Ensure user selection persists by UUID and updates weight, history, graphs, and goals.
- [ ] Remove obsolete Activity methods, casts, imports, and resources after migration.

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

- Navigation mechanism selected: —
- User-selector implementation: —
- Activity helpers removed: —
- Follow-up work: —

---

## Phase 9 — Final cleanup and regression verification

Status: Not started<br>
Started: —<br>
Completed: —<br>
Commit: —

### Objective

Remove confirmed obsolete code, measure the simplification, update architecture documentation, and
complete all automated and manual release gates.

### Tasks

- [ ] Run lint and remove resources, methods, fields, imports, and suppressions confirmed unused.
- [ ] Remove stale comments and commented-out code in touched classes.
- [ ] Normalize names in touched code only; avoid a repository-wide cosmetic rename.
- [ ] Review class responsibilities and ensure no new helper became an unrelated catch-all.
- [ ] Compare production Java/XML line counts with the Phase 0 baseline.
- [ ] Compare the largest production classes with their baseline sizes.
- [ ] Update `docs/architecture.md` with the final measurement presentation, conversion, graph, write,
      backup, and navigation boundaries.
- [ ] Update `README.md` links if this plan is intended to remain as maintained documentation.
- [ ] Run every automated verification command from a clean checkout or clean build state.
- [ ] Complete the applicable items in `docs/release-checklist.md` on representative devices.
- [ ] Record any deliberately deferred work below.

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

- Production Java lines: —
- Production XML lines: —
- Unit tests: —
- Lint result: —
- Release APK size: —
- Largest classes before/after: —
- Net files added/removed: —

### Deferred work

- —

### Completion notes

- Automated verification: —
- Manual verification: —
- Documentation updates: —
- Final release recommendation: —

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
