# Architecture

The application remains a single Android application module written in Java. The refactor separates
state, persistence, presentation, graph calculations, integrations, and protocol logic while
retaining the existing fragments and XML layouts.

## Application state and persistence

`AppStateViewModel` is the lifecycle-aware entry point used by activities and fragments. It delegates
all model lookup and mutation to the process-wide `AppRepository`; UI classes do not read or replace
data files directly. The repository keeps in-memory snapshots, serializes writes on one executor, and
returns `RepositoryResult` values for operations that can fail. UI mutations use
`AppRepository.MutationCallback`: work is serialized on the repository executor and
`AppStateViewModel` delivers completion on the main thread. Callers never receive an ignored
`Future`, and every failure reaches a visible handler. Failed writes retain the optimistic in-memory
snapshot so queued mutations are not rolled back over one another.

Three codecs define the persisted JSON contract:

- `UserJsonCodec` reads and writes the `users` JSON array.
- `WeightJsonCodec` reads and writes the `history` JSON array.
- `GoalJsonCodec` reads and writes the `goals` JSON array.

`AtomicJsonFile` writes UTF-8 data through a `.tmp` file, keeps a `.del` rollback file during
replacement, synchronizes the file descriptor, and recovers interrupted replacements on the next
read or write. Repository snapshots prevent callers from mutating shared collections accidentally.

## UI boundary

`MainActivity` owns the activity-scoped `AppStateViewModel`. `NavigationDestination` maps stable
drawer resource IDs to titles and Fragment factories, while `AppHost` is the narrow navigation and
shared-action-bar contract exposed to Fragments. `UserSpinnerController` is the only normal/large
user-selection setup path and selection remains persisted by UUID.

Fragments render state and forward user actions; generated View Binding objects exist only for the
corresponding view lifecycle and are cleared in `onDestroyView`. RecyclerView adapters own shallow
list snapshots and use typed row bindings.

## Measurement presentation and conversion

`Metric` is the authoritative read-only definition of supported measurements. `MassConverter` and
`LocalizedNumberParser` are Android-independent conversion and strict localized-input boundaries.
`MeasurementPresentationFactory` constructs reusable display models consumed by weight cards and
history rows. `EditableWeightMetric` supplies editor-only setters and input policy without adding UI
mutation concerns to `Weight`.

Goal editing follows the same split: `GoalValueDefinition` derives layout mode, precision, unit
labels, and canonical conversion from `Metric`, and two `GoalValueInput` controllers bind the start
and end subviews. Persisted mass values remain canonical kilograms.

## Graph boundary

`GraphPeriod` is the table for all period menu IDs, viewport spans, and availability rules.
`GraphSeriesBuilder` operates on chart-independent `GraphPoint` values and owns raw point selection,
exponential rolling averages, interpolated visible-window averages, and goal filtering. Periods
define the initial viewport and averaging windows but intentionally do not discard historical points,
preserving panning. `GraphsFragment` retains MPAndroidChart datasets, colors, gestures, and viewport
rendering only.

## Backup boundary

`BackupArchive` is the only ZIP implementation. It owns the fixed `users`, `history`, and `goals`
entry definition, compression, buffering, transferred-stream closure, JSON/size/path validation,
and atomic restoration. Picker Fragments only open streams off the main thread and present results.

`ForegroundUpload` is the UI owner for an interactive upload. It owns the progress dialog, one
executor, cancellation, and final user-visible results. Pure FIT construction and message formatting
remain in `FitFileFactory` and `MeasurementTextFormatter`.

## Garmin boundary

The foreground composition root is `GarminForegroundSession`. It constructs and connects:

- `GarminHttpClient` for HTTP requests, redirects, cookies, and response decoding;
- `GarminAuthenticator` for SSO, MFA, OAuth1 acquisition, OAuth2 exchange, and renewal decisions;
- `GarminTokenStore` for repository-backed credential updates and refresh scheduling;
- `GarminWeightService` for FIT upload and weight-history download;
- `DialogMfaCodeProvider` for the replaceable Android MFA interface.

`UploadCoordinator` invokes the foreground session synchronously on the executor owned by
`ForegroundUpload`. Background access-token renewal uses `GarminTokenRefreshWorker` and constructs
the same authenticator with a non-interactive MFA provider. `GarminTokenRefreshScheduler` is the
only component that defines WorkManager names and renewal timing.

Interactive history download is owned by `GarminHistoryDownloadCoordinator`, which observes the
History view lifecycle and owns its executor, cancellation, notification channel, progress updates,
and main-thread result delivery. Leaving the History view cancels the task and removes its pending
callbacks and notification. `GarminHistoryImporter` separately parses the response and applies the
established duplicate-detection rules without Android UI dependencies.

## ANT boundary

`AntServiceClient` owns Android service discovery, binding, broadcast receivers, channel commands,
and idempotent cleanup. It forwards incoming protocol messages without holding a Fragment or
Activity.

`AntMessageParser` validates and decodes ANT pages. `AntWeightSession` is the Android-free protocol
state machine. `AntWeightController` coordinates the service and state machine, applies timeouts,
persists only protocol-complete measurements, and reports success only after persistence completes
through `AntWeightListener`. The activity-scoped ViewModel owns the controller so a measurement can survive
Activity recreation; `WeightFragment` attaches only while its UI is active.

## Persisted-data compatibility

The internal filenames and JSON structures are intentionally unchanged. Backups remain ZIP archives
whose recognized entries are exactly `users`, `history`, and `goals`.

- Readers continue accepting legacy users with `age` instead of `birthdate` and `usesKg` instead of
  `mass_unit`.
- Missing optional measurement values retain their established `-1` sentinel. The historical
  active-metabolism-only representation is still normalized to basal metabolism when read.
- Existing keys are retained when current models are written. No schema-version gate or mandatory
  one-time migration was introduced.
- Sanitized fixtures characterize old and current representations. Any future format change must add
  backward-reading tests before changing a writer.

Garmin credentials and tokens are currently stored in the `users` file so existing installations
and backups remain compatible. Keystore encryption requires an explicit migration and recovery
design and remains release-tracked technical debt.

## Remaining release verification

Automated tests cover persistence compatibility, metric behavior, FIT generation, Garmin
authentication/renewal decisions, and ANT parsing/state transitions. They cannot establish behavior
of external services or hardware. Before release, complete `docs/release-checklist.md`, including
physical ANT measurements, MFA, history download, email sharing, backup restoration, and a complete
background token-renewal cycle.
