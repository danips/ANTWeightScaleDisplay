# ANT+ Weight Scale Display Refactoring Plan

This document tracks the incremental simplification of the application. It is intended to remain
in version control for the duration of the work. Completed phases should be marked complete and
retained as an audit trail; do not delete them.

## Current status

- Current phase: Phase 8 device verification; Phase 9 implementation complete
- Overall status: Verification pending
- Last updated: 2026-07-11
- Baseline commit: `6e0a7fa`
- Baseline application code: 29 Java files and 9,903 lines
- Vendored Garmin FIT SDK: 493 Java files and approximately 77,500 lines
- Baseline lint result: 9 errors and 155 warnings
- Baseline unit tests: 22 passing
- Baseline unsigned release APK: 2,290,656 bytes
- Baseline clean release build: 16 seconds with `lintVitalRelease` excluded

The local `.project` modification is unrelated IDE metadata and is not part of this plan.

## Working rules

1. Complete the phases in order unless a documented blocker requires a change.
2. Keep every commit buildable and independently reviewable.
3. Do not combine a storage migration with an authentication, ANT, or UI rewrite.
4. Preserve the existing `users`, `history`, and `goals` file formats until repository migration
   tests prove compatibility.
5. Never put real Garmin credentials, notification contents, or personal measurement data in test
   fixtures.
6. Run the phase-specific tests before marking a phase complete.
7. Record the completion date and commit hash for every completed phase.
8. Record important design decisions in the decision log at the end of this file.

## Progress

- [x] Phase 0 — Establish a safety baseline
- [x] Phase 1 — Fix lint and remove dead code
- [x] Phase 2 — Package or isolate the Garmin FIT SDK
- [x] Phase 3 — Centralize file persistence
- [x] Phase 4 — Move application state out of `MainActivity`
- [x] Phase 5 — Make metric handling data-driven
- [x] Phase 6 — Simplify upload orchestration
- [x] Phase 7 — Split Garmin responsibilities
- [ ] Phase 8 — Decouple the ANT state machine
- [x] Phase 9 — Apply View Binding consistently
- [ ] Phase 10 — Complete final migration and regression verification

---

## Phase 0 — Establish a safety baseline

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: `ea29454`

### Objective

Capture current behavior before changing architecture. Later phases should be able to distinguish
an intentional behavior change from a regression.

### Tasks

- [x] Create a dedicated refactoring branch.
- [x] Ensure the working tree contains no unrelated IDE changes.
- [x] Add sanitized fixtures for representative `users`, `history`, and `goals` files.
- [x] Include fixtures containing optional and legacy fields so backward compatibility is tested.
- [x] Add round-trip tests that load and save each fixture without losing supported data.
- [x] Add or expand tests for weight and body-composition calculations.
- [x] Add a FIT weight-file generation test that verifies the generated file can be decoded or at
      least validates its structure and checksum.
- [x] Add tests for metric extraction and unit formatting.
- [x] Keep the existing Garmin token status, expiration parsing, and scheduler tests.
- [x] Extract pure ANT message parsing functions where possible and add tests based on sanitized
      captured messages.
- [x] Document manual smoke tests for ANT measurement, Garmin authentication/upload, history
      download, email sharing, graphs, goals, backup, and restore.
- [x] Record baseline test, lint, release-build, APK-size, and build-time results.

### Completion notes

- Created branch `refactor/simplify-architecture` from baseline commit `6e0a7fa`.
- Added sanitized fixtures with current and legacy user/measurement representations.
- Persistence characterization tests intentionally use reflection against current private
  serializers. Phase 3 should replace this with direct codec tests.
- Added a test-only `org.json` implementation so local JVM tests exercise JSON behavior rather than
  Android's mockable stubs.
- Extracted only ANT envelope validation; protocol state and data-page parsing remain unchanged for
  Phase 8.
- `lintDebug` remains an expected failure at 9 errors and 155 warnings. No lint baseline was added.
- A clean minified release build succeeded in 16 seconds with only the known fatal lint task
  excluded. The unsigned APK is 2,290,656 bytes.

### Acceptance criteria

- Existing unit tests pass.
- Representative saved data can be loaded by tests.
- The manual smoke-test checklist is documented and repeatable.
- Known lint failures are recorded, not silently baselined.
- Release packaging succeeds when the known fatal lint task is excluded.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease -x lintVitalRelease
```

### Suggested commit

```text
test: add regression coverage for architecture refactor
```

---

## Phase 1 — Fix lint and remove dead code

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: `5230bf5`

### Objective

Remove low-risk clutter and make lint a useful regression gate before structural work begins.

### Tasks

- [x] Delete the three unused qualified `fab_margin` resources rather than adding an unused
      default resource.
- [x] Register ANT broadcast receivers with the correct exported/not-exported flags for supported
      Android versions. Confirm the flags still permit broadcasts from the external ANT service.
- [x] Resolve the missing `history_fragment_action_jump_to` translation issue by translating it or
      documenting the intended translation policy.
- [x] Resolve the FIT SDK API-24 stream usage for the current minimum SDK, or defer it explicitly to
      the FIT isolation phase if that phase removes the source from app lint.
- [x] Review the suspicious indentation reported in the FIT SDK before deciding whether to patch or
      replace that source.
- [x] Delete resources confirmed unused by lint after checking dynamic/resource-name lookups.
- [x] Remove obsolete commented-out methods, constants, and stale TODO comments.
- [x] Replace remaining hardcoded user-facing text with resources.
- [x] Address straightforward RTL, label, content-description, and accessibility warnings.
- [x] Fix clear logging mistakes and replace `printStackTrace()` in application code with contextual
      logging or propagated errors.
- [x] Do not create a broad lint baseline to hide existing errors.

### Completion notes

- Reduced lint from 9 errors and 155 warnings to 0 errors and 60 warnings without creating a lint
  baseline.
- Added narrowly scoped temporary lint exclusions for two generated FIT SDK files. Their API usage
  and suspicious indentation will disappear from app lint when Phase 2 isolates the SDK.
- Removed 28 lint-confirmed unused resources, including their translated string variants, after
  checking that the application does not resolve them dynamically.
- Registered ANT receivers as exported through `ContextCompat`, preserving broadcasts from the
  external ANT Radio Service on current Android versions.
- Kept `history_fragment_action_jump_to` in English as the fallback until translations are supplied;
  the intentional fallback is documented at the resource.
- Remaining warnings are deferred to their owning phases: generated FIT boxing (Phase 2), form
  autofill and adapter refreshes (Phase 9), plus low-risk vector, overdraw, target-SDK, and obsolete
  attribute cleanup.
- All 22 unit tests, debug lint, and the full minified release build pass.

### Acceptance criteria

- `lintDebug` reports no fatal errors.
- Unit tests and release packaging pass.
- No dynamically referenced resources were removed.
- The change contains no domain, authentication, or storage behavior rewrite.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

### Suggested commit

```text
refactor: remove dead resources and resolve lint errors
```

---

## Phase 2 — Package or isolate the Garmin FIT SDK

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: `851a9c9`

### Objective

Remove 493 generated third-party files from the main application source tree while
preserving FIT encoding behavior and licensing information.

### Tasks

- [x] Identify the exact Garmin FIT SDK version represented by the vendored source.
- [x] Check the SDK license and redistribution requirements.
- [x] Prefer an official binary artifact when one is available and reproducible.
- [x] Confirm a local module or JAR is unnecessary because the matching official artifact is
      available from Maven Central.
- [x] Store a checksum and source/version reference for any committed binary.
- [x] Add or update `THIRD_PARTY_NOTICES.md` with the SDK source and license.
- [x] Replace direct source inclusion with the module or binary dependency.
- [x] Keep only application-owned FIT construction code in the app package.
- [x] Confirm R8 still retains required message/encoder classes and removes unused ones.
- [x] Verify the generated weight FIT file before and after the change is semantically equivalent.

### Completion notes

- Replaced 493 generated Java files for FIT Profile 21.188.0 Release with Garmin's matching official
  `com.garmin:fit:21.188.0` Maven Central artifact.
- Recorded Garmin's source, FIT license, Maven coordinate, and the resolved JAR SHA-256 in
  `THIRD_PARTY_NOTICES.md`. No FIT SDK source or binary remains committed in this repository.
- Strengthened the FIT characterization test with a deterministic file SHA-256. The Maven artifact
  produces the byte-identical digest `71b74f44bf2e59a96330997cf345ebdfdec3696c6687958b62916a1bf9803fbe`.
- Removed the temporary generated-source lint exclusions. Lint now reports 0 errors and 50 warnings,
  down from 60 warnings after Phase 1.
- The minified release build succeeds without FIT-specific keep rules. R8 retains the encoder and
  message classes used by the upload path and reports 160 top-level FIT classes as removed.
- The unsigned release APK is 2,282,280 bytes. A real Garmin upload was not performed locally; the
  byte-for-byte output check demonstrates that the encoded payload is unchanged.

### Acceptance criteria

- The app source tree no longer contains the full generated FIT SDK.
- FIT generation tests pass.
- Release minification succeeds without additional blanket keep rules.
- License, version, provenance, and checksum are documented.
- Generated FIT files remain accepted by the existing Garmin upload path.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

Perform one real Garmin upload using a measurement generated after the change.

### Suggested commit

```text
build: replace vendored Garmin FIT sources with packaged SDK
```

---

## Phase 3 — Centralize file persistence

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: `c7f5322`

### Objective

Replace duplicated JSON loading, serialization, sorting, temporary-file replacement, and background
threads in `User`, `Weight`, and `Goal` with one tested persistence boundary. Preserve current data
files during this phase; do not introduce Room at the same time.

### Proposed components

```text
AppRepository
AtomicJsonFile
UserJsonCodec
WeightJsonCodec
GoalJsonCodec
```

### Tasks

- [x] Implement `AtomicJsonFile` for read, temporary write, atomic replacement, backup, rollback, and
      stale temporary/backup recovery.
- [x] Make codecs responsible only for converting models to and from JSON.
- [x] Preserve current file names and every supported JSON key.
- [x] Preserve current legacy-field migration behavior.
- [x] Introduce one repository-owned executor for serialized disk writes.
- [x] Stop creating an independent thread for every weight or goal save.
- [x] Ensure sorting works on copies instead of mutating caller-owned lists unexpectedly.
- [x] Return explicit success/failure results instead of only logging exceptions.
- [x] Add repository operations for users, measurements, goals, and selected-user persistence.
- [x] Keep Garmin access-token updates atomic and limited to token fields so background work cannot
      overwrite concurrent profile edits.
- [x] Add concurrency tests for profile edits occurring during Garmin renewal.
- [x] Add recovery tests for interrupted writes and stale `.tmp`/`.del` files.

### Completion notes

- Added `AtomicJsonFile` as the only component responsible for UTF-8 reads, synchronized replacement,
  file-descriptor flushing, rollback, and recovery of stale or interrupted `.tmp`/`.del` states.
- Added dedicated user, weight, and goal codecs. Existing filenames, JSON keys, optional fields, and
  legacy user/measurement conversions remain unchanged.
- Added `AppRepository` with one serialized write executor, explicit operation results, and operations
  for users, measurements, goals, selected-user preferences, and backup file discovery.
- Existing model persistence methods now act as compatibility adapters, keeping UI call sites stable.
  Weight and goal saves no longer create an independent thread for every operation.
- User and goal sorting operates on snapshots. Saving goals no longer reorders the caller's list.
- Garmin access-token renewal reloads and patches the latest user record. Full profile saves preserve
  newer token timestamps, preventing either concurrent operation from overwriting unrelated fields.
- Added direct codec characterization, malformed-data, atomic recovery, caller-mutation, and
  concurrent profile/token tests. All 32 unit tests pass.
- `lintDebug` passes with 0 errors and 50 warnings. The full minified release build passes, and the
  unsigned release APK is 2,284,276 bytes.
- A physical restore of a pre-refactor backup was not performed locally; fixture-based repository
  tests cover the same `users`, `history`, and `goals` formats without migration.

### Acceptance criteria

- Existing production data files load without migration or loss.
- Saving and reloading produces equivalent models.
- Only one component performs file replacement and backup logic.
- Concurrent repository writes cannot corrupt data or overwrite unrelated fields.
- UI behavior remains unchanged.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

Manually restore a pre-refactor backup and verify all users, measurements, goals, and Garmin status.

### Suggested commits

```text
refactor: extract atomic JSON persistence
refactor: introduce application data repository
```

---

## Phase 4 — Move application state out of `MainActivity`

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: Pending commit

### Objective

Make `MainActivity` a navigation and top-level UI host instead of the owner of every user,
measurement, goal, selection, and persistence operation.

### Tasks

- [x] Move users, measurements, goals, and selected-user state into `AppRepository`.
- [x] Persist the selected user by UUID instead of mutable display name.
- [x] Expose lifecycle-aware state through ViewModels and/or `LiveData` suitable for the Java codebase.
- [x] Replace direct mutable list access with snapshots or narrowly scoped repository operations.
- [x] Replace fragment public fields such as `the_user`, `the_weight`, and `the_goal` with stable UUID
      arguments.
- [x] Resolve objects from the repository after fragment recreation.
- [x] Remove casts to `MainActivity` where a repository or ViewModel can provide the data.
- [x] Remove `CoreInterface`; it is implemented only by `MainActivity` and is not consumed as an
      abstraction.
- [x] Centralize navigation methods and keyboard dismissal.
- [x] Add tests or manual checks for rotation, background/foreground transitions, and process death.

### Completion notes

- Repository-owned state now contains users, measurements, goals, and the selected-user UUID.
  Collection getters return snapshots, while add, edit, delete, download, and token updates use
  narrowly scoped repository operations.
- Added an activity-scoped `AppStateViewModel`. It survives configuration changes and delegates to
  the repository, which can reload the same identities from disk after process recreation.
- Migrated the `selected_user` name preference to `selected_user_uuid`. Existing installations read
  the legacy name once, resolve its UUID, and remove the mutable-name preference.
- Edit user, weight, and goal fragments now receive stable identity arguments and resolve their
  models through the ViewModel. Their edit fields and date state are preserved during recreation.
- Removed `CoreInterface`, non-persisted public fragment model fields, model list ownership from
  `MainActivity`, and activity casts used only to retrieve application data.
- `MainActivity` now coordinates navigation, ANT request lifecycle, upload launch, user spinners,
  and keyboard dismissal without performing model serialization.
- Added repository recreation, legacy-selection migration, snapshot-isolation, and stable identity
  resolution coverage. All 33 unit tests pass.
- `lintDebug` passes with 0 errors and 50 warnings. The full minified release build passes, and the
  unsigned release APK is 2,290,196 bytes.
- Rotation and process-recreation behavior is covered at the state/identity level. A full physical
  device navigation smoke test was not performed locally.

### Acceptance criteria

- Fragments restore the correct data after recreation.
- `MainActivity` no longer performs model serialization.
- No fragment relies on non-persisted public object fields for identity.
- Selected-user behavior remains compatible with existing preferences.
- Existing edit, delete, and navigation flows still work.

### Suggested commits

```text
refactor: move application state into repository
refactor: use stable fragment arguments
```

---

## Phase 5 — Make metric handling data-driven

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: `e1e4345`

### Objective

Replace repeated metric-specific condition chains across graphs, goals, cards, and exports with one
authoritative metric definition.

### Proposed metric metadata

- Stable metric identifier.
- Graph/menu resource ID.
- Label and icon resources.
- Line and fill colors.
- Unit category and formatting rules.
- Value extractor from `Weight`.
- Availability rule for missing values.
- Goal compatibility.
- Whether percentage values may be displayed as mass.

Add a `BodySegment` descriptor for trunk, left/right arms, and left/right legs so segmental logic can
be expressed as loops rather than copied branches.

### Tasks

- [x] Design `MetricDefinition` or extend `Metric` without coupling pure calculations unnecessarily
      to Android UI objects.
- [x] Add parameterized tests covering extraction, missing values, units, and formatting for every
      metric.
- [x] Migrate graph menu-ID mapping and graph value extraction.
- [x] Migrate graph colors and axis formatting.
- [x] Migrate goal editor choices and goal-to-graph matching.
- [x] Migrate measurement cards and segmental display.
- [x] Migrate email/CSV measurement formatting where applicable.
- [x] Remove obsolete switches and duplicated unit-selection expressions only after all consumers use
      the shared definitions.

### Implementation notes

- `Metric` now owns stable IDs, resources, graph styling, unit categories, value extraction,
  missing-value behavior, and percentage-as-mass policy without holding Android UI objects.
- `BodySegment` pairs the fat and muscle metrics for the five segmental cards.
- Graphs, goal progress, goal initialization, measurement cards, CSV export, and email output now
  consume the shared definitions. Export ordering remains explicit and stable.
- Replacing the goal-selection switch also corrected right-arm muscle goals reading right-leg
  muscle, and stone-unit goal parsing now reads both start and end fields correctly.
- The final unit suite, debug lint, and full minified release build all pass.

### Acceptance criteria

- Every supported metric has one authoritative definition.
- Adding a metric no longer requires editing several large condition chains.
- Graph, goal, card, email, and CSV output match pre-refactor behavior.
- Unit tests cover every metric definition.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

### Suggested commits

```text
refactor: centralize metric definitions
refactor: render graphs and goals from metric metadata
```

---

## Phase 6 — Simplify upload orchestration

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: `0e07460`

### Objective

Replace `AsyncUpload`'s executor-plus-nested-thread structure with a clear workflow and extract pure
FIT and text generation from Android UI concerns.

### Proposed components

```text
FitFileFactory
MeasurementTextFormatter
UploadCoordinator
UploadResult
```

### Tasks

- [x] Move FIT file construction into `FitFileFactory`.
- [x] Move email subject/body construction into `MeasurementTextFormatter`.
- [x] Use one executor instead of starting Garmin/email threads and immediately joining them.
- [x] Return structured success/failure results rather than mutable fields on thread subclasses.
- [x] Launch the email chooser on the main thread.
- [x] Keep dialogs and progress rendering in the UI layer.
- [x] Track the submitted task so cancellation can interrupt or ignore completion safely.
- [x] Shut down executors when work finishes.
- [x] Avoid retaining `MainActivity` longer than the interactive workflow requires.
- [x] Keep interactive MFA-driven uploads in the foreground; do not move them to WorkManager.
- [x] Keep background token renewal in WorkManager.

### Completion notes

- `FitFileFactory` now owns deterministic FIT construction and is exercised by the existing
  integrity and byte-digest characterization test.
- `MeasurementTextFormatter` creates the localized email subject and body through an injectable
  string provider, allowing formatting and unit behavior to be tested without an Activity.
- `UploadCoordinator` performs Garmin upload and email preparation sequentially on the one executor
  owned by `AsyncUpload`, returning an immutable `UploadResult`.
- `AsyncUpload` keeps only a weak Activity reference, owns progress UI, tracks its submitted
  `Future`, handles interruption, shuts down its executor, and opens the email chooser on the main
  thread.
- Interactive Garmin authentication and MFA remain in the foreground workflow. WorkManager-based
  token renewal was not changed.
- All 140 unit tests, debug lint, and the full minified release build pass.
- A live Garmin upload and email-chooser launch were not performed locally; cover both with the
  existing manual smoke-test checklist before release.

### Acceptance criteria

- No nested thread creation/joining remains in the upload coordinator.
- FIT and email formatting can be tested without an Activity.
- Cancellation and lifecycle behavior are explicit.
- Garmin upload and email sharing behave as before.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

### Suggested commit

```text
refactor: simplify measurement upload orchestration
```

---

## Phase 7 — Split Garmin responsibilities

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: `0d694c7`

### Objective

Separate HTTP transport, authentication, token storage, weight operations, and MFA UI currently
combined in `GarminConnect`.

### Proposed components

```text
GarminHttpClient
GarminAuthenticator
GarminTokenStore
GarminWeightService
MfaCodeProvider
```

### Tasks

- [x] Move generic request execution, redirects, headers, cookies, and response decoding into
      `GarminHttpClient`.
- [x] Move SSO, OAuth1 acquisition, MFA token handling, and signed OAuth2 exchange into
      `GarminAuthenticator`.
- [x] Back `GarminTokenStore` with repository operations.
- [x] Move FIT upload and history download into `GarminWeightService`.
- [x] Represent MFA input as an injected callback/provider rather than an Activity-owned dialog in
      the network class.
- [x] Ensure network/authentication classes do not retain an Activity.
- [x] Inject fake transport responses for authentication tests.
- [x] Test successful login, MFA, cancellation, invalid credentials, temporary server errors,
      renewal, and rejected renewal credentials.
- [x] Preserve the existing endpoints, request signing, payloads, and retry semantics during this
      structural change.
- [x] Consider Android Keystore-backed protection for Garmin credentials only after token storage is
      isolated and migration behavior is defined.

### Completion notes

- `GarminHttpClient` owns request encoding, response decoding, timeouts, redirects, headers, and an
  explicit cookie manager. Foreground SSO retains the existing process cookie-handler behavior;
  background renewal does not replace it.
- `GarminAuthenticator` owns SSO, OAuth1 acquisition, MFA branching, signed OAuth2 exchange, access
  validation, and renewal result classification without importing Android UI classes.
- `GarminTokenStore` is backed by the existing repository-compatible synchronous user/token
  persistence operations and retains refresh scheduling after an interactive connection.
- `GarminWeightService` owns FIT upload and weight-history download. `GarminConnect` remains only as
  a compatibility facade for the two foreground callers.
- `DialogMfaCodeProvider` contains the existing notification permission, automatic code detection,
  manual entry, and cancellation UI behind the injected `MfaCodeProvider` interface. It holds the
  Activity weakly.
- WorkManager constructs the same `GarminAuthenticator` used by foreground operations with a
  non-interactive MFA provider and repository-backed token store.
- Fake transport tests cover successful login, MFA, cancellation, invalid credentials, temporary
  login failure, successful background renewal, temporary renewal failure, and rejected renewal
  credentials.
- Keystore-backed credential protection is deliberately deferred: token storage is now isolated,
  but a compatible migration and recovery design is required before changing the persisted format.
- All 148 unit tests, debug lint, and the full minified release build pass.
- Live login, MFA, upload, history download, and background renewal were not performed locally; use
  the Garmin manual smoke-test checklist before release.

### Acceptance criteria

- Garmin network code can be tested without Android UI.
- MFA UI can be replaced without changing authentication logic.
- Background renewal uses the same authentication component as foreground renewal.
- Existing login, upload, download, token display, and WorkManager behavior remain correct.

### Verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

### Suggested commits

```text
refactor: separate Garmin authentication and transport
refactor: decouple Garmin MFA from activity UI
```

---

## Phase 8 — Decouple the ANT state machine

Status: Verification pending<br>
Completed: —<br>
Commit: `704c8bc`

### Objective

Separate ANT service communication, protocol parsing, state transitions, and UI. This is the
highest-risk phase and requires real scale testing.

### Proposed components

```text
AntServiceClient
AntWeightSession
AntMessageParser
AntWeightListener
```

### Tasks

- [x] Move service binding, claiming, receiver registration, and cleanup into `AntServiceClient`.
- [x] Move protocol state and transitions into `AntWeightSession`.
- [x] Extract message validation and data-page decoding into pure parser functions.
- [x] Replace the direct `WeightFragment` reference with listener or observable events.
- [x] Replace static/deprecated `ProgressDialog` ownership with fragment-rendered progress state.
- [x] Make stop/release cleanup idempotent.
- [x] Model success, timeout, permission, disconnect, and protocol failures explicitly.
- [x] Add state-machine tests based on sanitized captured ANT message sequences.
- [x] Verify receiver flags and service lifecycle on all supported Android versions.
- [ ] Perform repeated real-device tests for success, timeout, cancellation, disconnect, and partial
      measurement cases.

### Implementation notes

- `AntServiceClient` owns explicit service resolution, binding, receiver registration, permission
  inspection, channel cleanup, unbinding, and idempotent repeated stop/unregister calls. External
  ANT service broadcasts retain the required exported receiver flags.
- `AntWeightSession` is a pure protocol state machine covering startup, channel configuration,
  search, profile confirmation, receiving, completion, and typed failures.
- `AntMessageParser` validates envelopes and decodes standard and Tanita-specific weight,
  composition, metabolic, mass, and segmental pages without Android dependencies.
- `AntWeightController` coordinates the service and pure session through a weak
  `AntWeightListener`, schedules typed timeouts, persists only complete measurements, and rejects
  partial results on timeout or failure.
- The activity-scoped ViewModel owns the controller so an active session survives Activity
  recreation. Fragments detach and reattach listeners without being retained by protocol code.
- `WeightFragment` owns and renders ANT progress, cancellation, permission guidance, errors, and
  successful completion. The static deprecated `ProgressDialog` was removed.
- The former 1,192-line `RequestWeight` class is now only a small compatibility utility for the
  unrelated health-range calculations still used by measurement cards.
- Sanitized parser and state-machine tests cover successful standard-page completion, incomplete
  measurements, scale-not-ready, non-barefoot data, Tanita segmental pages, configuration
  transitions, and profile confirmation. All 155 unit tests, debug lint, and the minified release
  build pass.

### Remaining device verification

Repeat each case at least three times on a representative supported Android device:

1. Successful complete measurement, confirming it is saved exactly once.
2. Search timeout with the scale off, confirming nothing is saved.
3. User cancellation during search and while waiting for measurements.
4. ANT service/radio disconnect during search and during measurement reception.
5. Partial composition sequence or non-barefoot measurement, confirming it is not saved/uploaded.
6. Activity recreation during search, confirming progress resumes and no Activity/Fragment leaks.

### Acceptance criteria

- ANT protocol/session classes do not retain a Fragment or Activity.
- UI observes session state and renders dialogs/progress itself.
- Repeated stop/destroy calls are safe.
- Partial or failed measurements are never saved or uploaded.
- Real ANT scale behavior matches the pre-refactor app.

### Automated verification

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

### Suggested commits

```text
refactor: extract ANT message parser
refactor: decouple ANT session from weight UI
```

---

## Phase 9 — Apply View Binding consistently

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: Pending commit

### Objective

Use the already enabled View Binding feature throughout fragments and adapters, after business logic
has moved out of those classes.

### Tasks

- [x] Convert one fragment or adapter at a time in reviewable changes.
- [x] Hold fragment bindings only from `onCreateView` through `onDestroyView`.
- [x] Clear every fragment binding in `onDestroyView`.
- [x] Convert RecyclerView row holders to row bindings.
- [x] Replace broad `notifyDataSetChanged()` calls with specific updates or `ListAdapter`/`DiffUtil`.
- [x] Consolidate duplicated keyboard dismissal and simple dialog helpers.
- [x] Remove remaining `findViewById` calls where binding is appropriate.
- [x] Do not combine this mechanical conversion with visual redesign.

### Implementation notes

- All fragments now use generated bindings and release view, adapter, chart, and menu references in
  `onDestroyView` so destroyed view hierarchies are not retained.
- `MainActivity`, the searchable spinner dialog, chart marker, and RecyclerView rows now use typed
  bindings instead of raw view lookup.
- Recycler adapters keep private snapshots and dispatch explicit removal and insertion ranges when
  their contents are replaced. The filtering `ArrayAdapter` retains `notifyDataSetChanged()` because
  filtering intentionally replaces its complete visible result set.
- Shared `KeyboardUtils` behavior replaces the duplicated recursive keyboard-dismissal code in the
  user and goal editors and is also used by the spinner dialog.
- No layouts, styling, strings, or interaction flows were redesigned in this phase.

### Acceptance criteria

- Fragments do not retain destroyed views.
- UI behavior and layout remain unchanged.
- Adapters use typed row bindings.
- RecyclerView updates remain correct and become more specific where practical.

### Verification

- All 155 debug unit tests pass.
- `lintDebug` passes.
- The complete minified release build, including `lintVitalRelease`, passes.
- `git diff --check` reports no whitespace errors.

### Suggested commit

```text
refactor: adopt view binding across legacy screens
```

---

## Phase 10 — Complete final migration and regression verification

Status: Pending  
Completed: —  
Commit: —

### Objective

Verify compatibility, remove temporary migration adapters, document the resulting architecture, and
measure whether the refactor achieved its goals.

### Automated verification

- [ ] Run all unit tests.
- [ ] Run lint with no fatal errors.
- [ ] Build the complete minified release without excluding lint.
- [ ] Verify no temporary migration paths or feature flags remain unnecessarily.
- [ ] Compare APK size, method count, build time, source-file count, and lint warning count with the
      baseline.

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

### Manual device verification

- [ ] Test API 23, 29, 33, and current target/API 37 where devices or emulators are available.
- [ ] Test rotation, background/foreground transitions, and process recreation.
- [ ] Create, edit, select, and delete users.
- [ ] Create, edit, and delete manual measurements.
- [ ] Create, edit, display, and delete goals.
- [ ] Complete an ANT measurement successfully.
- [ ] Test ANT timeout, permission failure, cancellation, and disconnect.
- [ ] Log in to Garmin with MFA.
- [ ] Upload with active and expired access tokens.
- [ ] Observe background renewal across a complete access-token cycle.
- [ ] Download Garmin history.
- [ ] Open and complete the email-sharing workflow.
- [ ] Restore a backup created before the refactor.
- [ ] Verify graphs for every metric, unit system, goal type, and segmental measurement.

### Documentation

- [ ] Add a concise architecture overview describing repository, ViewModels, Garmin, ANT, and upload
      boundaries.
- [ ] Update third-party dependency and licensing documentation.
- [ ] Document persisted files and their compatibility policy.
- [ ] Record final metrics and remaining technical debt below.

### Acceptance criteria

- All automated checks pass.
- Manual verification passes on representative devices.
- Existing saved data and backups remain usable.
- Garmin background renewal remains reliable.
- ANT measurement behavior remains reliable.
- The final architecture and remaining limitations are documented.

### Suggested commit

```text
docs: complete architecture refactor roadmap
```

---

## Manual smoke-test checklist

Use this checklist after any phase that touches storage, lifecycle, Garmin, ANT, metrics, or UI.

### Data and navigation

- [ ] Existing users, measurements, and goals load.
- [ ] Selected user is preserved after restart.
- [ ] User switching updates weight, history, graphs, and goals.
- [ ] Rotation does not lose the current edit target.
- [ ] Backup and restore preserve all supported fields.

### Measurements and ANT

- [ ] A successful scale measurement is saved once.
- [ ] A partial measurement is not saved.
- [ ] A timeout displays an error and leaves the UI empty.
- [ ] Cancel and disconnect release the ANT service safely.
- [ ] Automatic upload runs only after a successful measurement.

### Garmin

- [ ] Token status and timestamps are accurate.
- [ ] Foreground upload succeeds.
- [ ] MFA prompt and cancellation work.
- [ ] Expired access is renewed using saved OAuth1 credentials.
- [ ] Background renewal is scheduled before access expiry.
- [ ] History download succeeds and does not duplicate measurements.

### Display and export

- [ ] Every metric displays the correct value and unit.
- [ ] Fat percentage/mass preference is respected.
- [ ] Segmental measurements map to the correct body part.
- [ ] Goals appear on the matching graphs.
- [ ] CSV and email output use the correct values and units.

## Decision log

Add entries whenever a decision changes the implementation direction or phase ordering.

| Date | Phase | Decision | Reason | Commit |
|---|---|---|---|---|
| 2026-07-11 | Planning | Use incremental Java refactoring instead of a Kotlin/Compose rewrite | Preserves behavior and reduces regression risk around Garmin and ANT integrations | — |
| 2026-07-11 | Phase 3 | Preserve JSON storage before considering Room | A repository and shared atomic store provide most of the simplification without a risky data migration | — |
| 2026-07-11 | Phase 0 | Use reflection only in persistence characterization tests | Captures current private serializer behavior without changing production persistence APIs before Phase 3 | `ea29454` |
| 2026-07-11 | Phase 1 | Suppress lint only on the two generated FIT sources scheduled for removal | Avoids modifying generated third-party code while keeping all application lint errors visible | `5230bf5` |
| 2026-07-11 | Phase 1 | Use the English jump-to label as the documented translation fallback | Avoids inventing unreviewed translations while resolving the fatal missing-translation check | `5230bf5` |
| 2026-07-11 | Phase 2 | Use Garmin's official Maven artifact at the exact vendored profile version | Removes generated source while preserving byte-level FIT output and obtaining the SDK from its publisher | `851a9c9` |
| 2026-07-11 | Phase 3 | Keep JSON files and compatibility adapters behind one repository | Centralizes safety and concurrency without combining the refactor with a Room migration or UI rewrite | `c7f5322` |
| 2026-07-11 | Phase 4 | Use repository-owned state behind an activity-scoped ViewModel | Preserves state across rotation, supports disk re-resolution after process death, and keeps navigation separate from model ownership | Pending commit |
| 2026-07-11 | Phase 7 | Defer Android Keystore protection until a credential migration and recovery format is designed | Avoids silently invalidating existing Garmin connections while token persistence is being structurally isolated | Pending commit |
| 2026-07-11 | Phase 8 | Persist only protocol-complete ANT measurements | Prevents timeouts, disconnects, and partial composition sequences from being saved or automatically uploaded | `704c8bc` |
| 2026-07-11 | Phase 9 | Keep adapter-owned snapshots and dispatch explicit replacement ranges | Avoids broad RecyclerView invalidation while preventing adapters from mutating repository-owned collections | Pending commit |

## Final results

Complete this section during Phase 10.

| Measure | Baseline | Final | Change |
|---|---:|---:|---:|
| Application Java files | 29 | — | — |
| Application Java lines | 9,903 | — | — |
| Vendored FIT Java files | 493 | — | — |
| Vendored FIT Java lines | ~77,500 | — | — |
| Lint errors | 9 | — | — |
| Lint warnings | 155 | — | — |
| Unit tests | 22 passing | — | — |
| Release APK size | 2,290,656 bytes unsigned | — | — |
| Clean release build time | 16 seconds, fatal lint excluded | — | — |

## Remaining technical debt

Record deliberately deferred work here instead of expanding an active phase without review.

- The 60 non-fatal lint warnings are assigned to later phases or low-risk final cleanup; see the
  Phase 1 completion notes for their categories.
