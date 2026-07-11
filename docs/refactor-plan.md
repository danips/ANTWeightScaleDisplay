# ANT+ Weight Scale Display Refactoring Plan

This document tracks the incremental simplification of the application. It is intended to remain
in version control for the duration of the work. Completed phases should be marked complete and
retained as an audit trail; do not delete them.

## Current status

- Current phase: Phase 1 — Fix lint and remove dead code
- Overall status: In progress
- Last updated: 2026-07-11
- Baseline commit: `6e0a7fa`
- Baseline application code: 29 Java files and 9,903 lines
- Vendored Garmin FIT SDK: approximately 491 Java files and 77,500 lines
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
- [ ] Phase 1 — Fix lint and remove dead code
- [ ] Phase 2 — Package or isolate the Garmin FIT SDK
- [ ] Phase 3 — Centralize file persistence
- [ ] Phase 4 — Move application state out of `MainActivity`
- [ ] Phase 5 — Make metric handling data-driven
- [ ] Phase 6 — Simplify upload orchestration
- [ ] Phase 7 — Split Garmin responsibilities
- [ ] Phase 8 — Decouple the ANT state machine
- [ ] Phase 9 — Apply View Binding consistently
- [ ] Phase 10 — Complete final migration and regression verification

---

## Phase 0 — Establish a safety baseline

Status: Completed<br>
Completed: 2026-07-11<br>
Commit: Pending commit

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

Status: Pending  
Completed: —  
Commit: —

### Objective

Remove low-risk clutter and make lint a useful regression gate before structural work begins.

### Tasks

- [ ] Delete the three unused qualified `fab_margin` resources rather than adding an unused
      default resource.
- [ ] Register ANT broadcast receivers with the correct exported/not-exported flags for supported
      Android versions. Confirm the flags still permit broadcasts from the external ANT service.
- [ ] Resolve the missing `history_fragment_action_jump_to` translation issue by translating it or
      documenting the intended translation policy.
- [ ] Resolve the FIT SDK API-24 stream usage for the current minimum SDK, or defer it explicitly to
      the FIT isolation phase if that phase removes the source from app lint.
- [ ] Review the suspicious indentation reported in the FIT SDK before deciding whether to patch or
      replace that source.
- [ ] Delete resources confirmed unused by lint after checking dynamic/resource-name lookups.
- [ ] Remove obsolete commented-out methods, constants, and stale TODO comments.
- [ ] Replace remaining hardcoded user-facing text with resources.
- [ ] Address straightforward RTL, label, content-description, and accessibility warnings.
- [ ] Fix clear logging mistakes and replace `printStackTrace()` in application code with contextual
      logging or propagated errors.
- [ ] Do not create a broad lint baseline to hide existing errors.

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

Status: Pending  
Completed: —  
Commit: —

### Objective

Remove approximately 491 generated third-party files from the main application source tree while
preserving FIT encoding behavior and licensing information.

### Tasks

- [ ] Identify the exact Garmin FIT SDK version represented by the vendored source.
- [ ] Check the SDK license and redistribution requirements.
- [ ] Prefer an official binary artifact when one is available and reproducible.
- [ ] If an official binary is unavailable, isolate the exact existing SDK in a dedicated Gradle
      module or build a versioned local JAR from a documented upstream source.
- [ ] Store a checksum and source/version reference for any committed binary.
- [ ] Add or update `THIRD_PARTY_NOTICES.md` with the SDK source and license.
- [ ] Replace direct source inclusion with the module or binary dependency.
- [ ] Keep only application-owned FIT construction code in the app package.
- [ ] Confirm R8 still retains required message/encoder classes and removes unused ones.
- [ ] Verify the generated weight FIT file before and after the change is semantically equivalent.

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

Status: Pending  
Completed: —  
Commit: —

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

- [ ] Implement `AtomicJsonFile` for read, temporary write, atomic replacement, backup, rollback, and
      stale temporary/backup recovery.
- [ ] Make codecs responsible only for converting models to and from JSON.
- [ ] Preserve current file names and every supported JSON key.
- [ ] Preserve current legacy-field migration behavior.
- [ ] Introduce one repository-owned executor for serialized disk writes.
- [ ] Stop creating an independent thread for every weight or goal save.
- [ ] Ensure sorting works on copies instead of mutating caller-owned lists unexpectedly.
- [ ] Return explicit success/failure results instead of only logging exceptions.
- [ ] Add repository operations for users, measurements, goals, and selected-user persistence.
- [ ] Keep Garmin access-token updates atomic and limited to token fields so background work cannot
      overwrite concurrent profile edits.
- [ ] Add concurrency tests for profile edits occurring during Garmin renewal.
- [ ] Add recovery tests for interrupted writes and stale `.tmp`/`.del` files.

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

Status: Pending  
Completed: —  
Commit: —

### Objective

Make `MainActivity` a navigation and top-level UI host instead of the owner of every user,
measurement, goal, selection, and persistence operation.

### Tasks

- [ ] Move users, measurements, goals, and selected-user state into `AppRepository`.
- [ ] Persist the selected user by UUID instead of mutable display name.
- [ ] Expose lifecycle-aware state through ViewModels and/or `LiveData` suitable for the Java codebase.
- [ ] Replace direct mutable list access with snapshots or narrowly scoped repository operations.
- [ ] Replace fragment public fields such as `the_user`, `the_weight`, and `the_goal` with stable UUID
      arguments.
- [ ] Resolve objects from the repository after fragment recreation.
- [ ] Remove casts to `MainActivity` where a repository or ViewModel can provide the data.
- [ ] Remove `CoreInterface`; it is implemented only by `MainActivity` and is not consumed as an
      abstraction.
- [ ] Centralize navigation methods and keyboard dismissal.
- [ ] Add tests or manual checks for rotation, background/foreground transitions, and process death.

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

Status: Pending  
Completed: —  
Commit: —

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

- [ ] Design `MetricDefinition` or extend `Metric` without coupling pure calculations unnecessarily
      to Android UI objects.
- [ ] Add parameterized tests covering extraction, missing values, units, and formatting for every
      metric.
- [ ] Migrate graph menu-ID mapping and graph value extraction.
- [ ] Migrate graph colors and axis formatting.
- [ ] Migrate goal editor choices and goal-to-graph matching.
- [ ] Migrate measurement cards and segmental display.
- [ ] Migrate email/CSV measurement formatting where applicable.
- [ ] Remove obsolete switches and duplicated unit-selection expressions only after all consumers use
      the shared definitions.

### Acceptance criteria

- Every supported metric has one authoritative definition.
- Adding a metric no longer requires editing several large condition chains.
- Graph, goal, card, email, and CSV output match pre-refactor behavior.
- Unit tests cover every metric definition.

### Suggested commits

```text
refactor: centralize metric definitions
refactor: render graphs and goals from metric metadata
```

---

## Phase 6 — Simplify upload orchestration

Status: Pending  
Completed: —  
Commit: —

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

- [ ] Move FIT file construction into `FitFileFactory`.
- [ ] Move email subject/body construction into `MeasurementTextFormatter`.
- [ ] Use one executor instead of starting Garmin/email threads and immediately joining them.
- [ ] Return structured success/failure results rather than mutable fields on thread subclasses.
- [ ] Launch the email chooser on the main thread.
- [ ] Keep dialogs and progress rendering in the UI layer.
- [ ] Track the submitted task so cancellation can interrupt or ignore completion safely.
- [ ] Shut down executors when work finishes.
- [ ] Avoid retaining `MainActivity` longer than the interactive workflow requires.
- [ ] Keep interactive MFA-driven uploads in the foreground; do not move them to WorkManager.
- [ ] Keep background token renewal in WorkManager.

### Acceptance criteria

- No nested thread creation/joining remains in the upload coordinator.
- FIT and email formatting can be tested without an Activity.
- Cancellation and lifecycle behavior are explicit.
- Garmin upload and email sharing behave as before.

### Suggested commit

```text
refactor: simplify measurement upload orchestration
```

---

## Phase 7 — Split Garmin responsibilities

Status: Pending  
Completed: —  
Commit: —

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

- [ ] Move generic request execution, redirects, headers, cookies, and response decoding into
      `GarminHttpClient`.
- [ ] Move SSO, OAuth1 acquisition, MFA token handling, and signed OAuth2 exchange into
      `GarminAuthenticator`.
- [ ] Back `GarminTokenStore` with repository operations.
- [ ] Move FIT upload and history download into `GarminWeightService`.
- [ ] Represent MFA input as an injected callback/provider rather than an Activity-owned dialog in
      the network class.
- [ ] Ensure network/authentication classes do not retain an Activity.
- [ ] Inject fake transport responses for authentication tests.
- [ ] Test successful login, MFA, cancellation, invalid credentials, temporary server errors,
      renewal, and rejected renewal credentials.
- [ ] Preserve the existing endpoints, request signing, payloads, and retry semantics during this
      structural change.
- [ ] Consider Android Keystore-backed protection for Garmin credentials only after token storage is
      isolated and migration behavior is defined.

### Acceptance criteria

- Garmin network code can be tested without Android UI.
- MFA UI can be replaced without changing authentication logic.
- Background renewal uses the same authentication component as foreground renewal.
- Existing login, upload, download, token display, and WorkManager behavior remain correct.

### Suggested commits

```text
refactor: separate Garmin authentication and transport
refactor: decouple Garmin MFA from activity UI
```

---

## Phase 8 — Decouple the ANT state machine

Status: Pending  
Completed: —  
Commit: —

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

- [ ] Move service binding, claiming, receiver registration, and cleanup into `AntServiceClient`.
- [ ] Move protocol state and transitions into `AntWeightSession`.
- [ ] Extract message validation and data-page decoding into pure parser functions.
- [ ] Replace the direct `WeightFragment` reference with listener or observable events.
- [ ] Replace static/deprecated `ProgressDialog` ownership with fragment-rendered progress state.
- [ ] Make stop/release cleanup idempotent.
- [ ] Model success, timeout, permission, disconnect, and protocol failures explicitly.
- [ ] Add state-machine tests based on sanitized captured ANT message sequences.
- [ ] Verify receiver flags and service lifecycle on all supported Android versions.
- [ ] Perform repeated real-device tests for success, timeout, cancellation, disconnect, and partial
      measurement cases.

### Acceptance criteria

- ANT protocol/session classes do not retain a Fragment or Activity.
- UI observes session state and renders dialogs/progress itself.
- Repeated stop/destroy calls are safe.
- Partial or failed measurements are never saved or uploaded.
- Real ANT scale behavior matches the pre-refactor app.

### Suggested commits

```text
refactor: extract ANT message parser
refactor: decouple ANT session from weight UI
```

---

## Phase 9 — Apply View Binding consistently

Status: Pending  
Completed: —  
Commit: —

### Objective

Use the already enabled View Binding feature throughout fragments and adapters, after business logic
has moved out of those classes.

### Tasks

- [ ] Convert one fragment or adapter per small commit.
- [ ] Hold fragment bindings only from `onCreateView` through `onDestroyView`.
- [ ] Clear every fragment binding in `onDestroyView`.
- [ ] Convert RecyclerView row holders to row bindings.
- [ ] Replace broad `notifyDataSetChanged()` calls with specific updates or `ListAdapter`/`DiffUtil`.
- [ ] Consolidate duplicated keyboard dismissal and simple dialog helpers.
- [ ] Remove remaining `findViewById` calls where binding is appropriate.
- [ ] Do not combine this mechanical conversion with visual redesign.

### Acceptance criteria

- Fragments do not retain destroyed views.
- UI behavior and layout remain unchanged.
- Adapters use typed row bindings.
- RecyclerView updates remain correct and become more specific where practical.

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
| 2026-07-11 | Phase 0 | Use reflection only in persistence characterization tests | Captures current private serializer behavior without changing production persistence APIs before Phase 3 | Pending commit |

## Final results

Complete this section during Phase 10.

| Measure | Baseline | Final | Change |
|---|---:|---:|---:|
| Application Java files | 29 | — | — |
| Application Java lines | 9,903 | — | — |
| Vendored FIT Java files | 491 | — | — |
| Vendored FIT Java lines | ~77,500 | — | — |
| Lint errors | 9 | — | — |
| Lint warnings | 155 | — | — |
| Unit tests | 22 passing | — | — |
| Release APK size | 2,290,656 bytes unsigned | — | — |
| Clean release build time | 16 seconds, fatal lint excluded | — | — |

## Remaining technical debt

Record deliberately deferred work here instead of expanding an active phase without review.

- None recorded yet.
