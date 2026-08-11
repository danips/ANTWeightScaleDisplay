# Architecture

The application remains a single Android application module written in Java. The refactor separates
state, persistence, presentation, graph calculations, integrations, and protocol logic while
retaining the existing fragments and XML layouts.

## Application state and persistence

`AppStateViewModel` is the lifecycle-aware entry point used by activities and fragments. It delegates
all model lookup and mutation to the process-wide `AppRepository`; UI classes do not read or replace
data files directly. Initial/recovery loading and document-provider operations run on one ViewModel
I/O executor. Replayable load state lets restored editor views defer model binding without blocking
the main thread, while typed one-shot operation results are delivered to the current view lifecycle
after rotation or navigation. The repository keeps in-memory snapshots, serializes writes on one executor, and
returns `RepositoryResult` values for operations that can fail. UI mutations use
`AppRepository.MutationCallback`: work is serialized on the repository executor and
`AppStateViewModel` delivers completion on the main thread. Callers never receive an ignored
`Future`, and every failure reaches a visible handler. Each queued mutation derives a candidate from
the latest successfully committed in-memory state when it reaches the executor, persists that
candidate, and publishes it only after the write succeeds. A failed write leaves live state
unchanged, and later mutations cannot persist the rejected candidate as a side effect.

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
list snapshots and use typed row bindings. The weight editor owns a detached working copy and an
immutable baseline copy, preserving both across recreation. Weight replacement uses the baseline's
original `(uuid,date)` key and rejects a changed baseline or destination-key collision rather than
overwriting concurrent state.

History-row expansion is keyed by the exact persisted `(uuid,date)` identity rather than adapter
position. Replacement prunes only identities no longer present, so deletion, insertion, and reorder
cannot transfer expansion to another measurement. Collapsed rows bind only their header; opening a
row uses an expansion payload to bind details without recomputing the header.

## Measurement presentation and conversion

`Metric` is the authoritative read-only definition of supported measurements. `MassConverter` and
`LocalizedNumberParser` are Android-independent conversion and strict localized-input boundaries.
`MeasurementPresentationFactory` constructs reusable display models consumed by weight cards and
history rows. `EditableWeightMetric` supplies editor-only setters and input policy without adding UI
mutation concerns to `Weight`.

Segment presentation treats fat percentage, derived fat mass, and muscle mass as distinct primary
value kinds. Fat uses the profile's percentage/mass preference when available; muscle remains the
secondary value when both exist and becomes primary only for a muscle-only segment. Availability is
fat-or-muscle, and trend comparison always uses the same value kind on both measurements.

Goal editing follows the same split: `GoalValueDefinition` derives layout mode, precision, unit
labels, and canonical conversion from `Metric`, and two `GoalValueInput` controllers bind the start
and end subviews. `GoalProgress` calculates total/on-track values without Android dependencies and
represents a missing current measurement explicitly. Goal rows refresh the latest measurement with
each selected profile and omit on-track values for inactive or invalid date ranges. Persisted mass
values remain canonical kilograms.

## Graph boundary

`GraphPeriod` is the table for all period menu IDs, viewport spans, and availability rules.
`GraphSeriesBuilder` operates on chart-independent `GraphPoint` values and owns raw point selection,
exponential rolling averages, interpolated visible-window averages, and goal filtering. Periods
define the initial viewport and averaging windows but intentionally do not discard historical points,
preserving panning. `GraphsFragment` retains MPAndroidChart datasets, colors, gestures, and viewport
rendering only.

## Backup boundary

`BackupArchive` is the only ZIP implementation. It owns the fixed `users`, `history`, and `goals`
entry definition, compression, buffering, transferred-stream closure, JSON/path validation, and a
50 MiB aggregate uncompressed-data limit. Restore requires all three entries and decodes each with
its production codec before persistence. `AtomicJsonDataset` journals the prior generation and
rolls back all three files after a write failure or interrupted process. Backup snapshots, restore
commits, delete-user commits, and ordinary writes share the repository's single executor, preventing
mixed-generation archives and interleaved dataset replacement. Picker Fragments only open streams
off the main thread and present results. CSV document creation and UTF-8 row encoding use the same
ViewModel I/O boundary; a success event is emitted only after the writer flushes and closes cleanly.

`ForegroundUploadManager` is retained by the activity-scoped ViewModel and owns one executor,
cancellation, immutable progress state, and one-shot results. It uses application context for file,
repository, and formatting work and holds only a weak reference to the current Activity for
interactive MFA/provider repair. `MainActivity` renders a replaceable cancelable progress dialog and
the final result, so rotation does not duplicate work or strand completion on the old Activity. Pure
FIT construction and message formatting remain in `FitFileFactory` and
`MeasurementTextFormatter`.

## Garmin boundary

The foreground composition root is `GarminForegroundSession`. It constructs and connects:

- `GarminHttpClient` for HTTP requests, redirects, cookies, and response decoding;
- `GarminAuthenticator` for SSO, MFA, OAuth1 acquisition, OAuth2 exchange, and renewal decisions;
- `GarminTokenStore` for repository-backed credential updates and refresh scheduling;
- `GarminWeightService` for FIT upload and weight-history download;
- `DialogMfaCodeProvider` for the replaceable Android MFA interface.

Notification autofill is registered only while the Garmin MFA input dialog is visible. The listener
requires a Garmin keyword in the notification title or content, rejects notification timestamps
older than the request, and delivers at most one code through that request-specific registration.
Manual entry remains available regardless of notification source or listener access.

`UploadCoordinator` invokes the foreground session synchronously on the executor owned by
`ForegroundUpload`. Background access-token renewal uses `GarminTokenRefreshWorker` and constructs
the same authenticator with a non-interactive MFA provider. `GarminTokenRefreshScheduler` is the
only component that defines WorkManager names and renewal timing.

Interactive history download is owned by `GarminHistoryDownloadCoordinator`, which observes the
History view lifecycle and owns its executor, cancellation, notification channel, progress updates,
and main-thread result delivery. Leaving the History view cancels the task and removes its pending
callbacks and notification. `GarminHistoryImporter` separately parses the response and applies the
established duplicate-detection rules without Android UI dependencies. It indexes measurements for
the selected user into duplicate-window date buckets, including newly accepted Garmin records in
that index, so matching is independent of history sort order and does not rescan all history for
every summary. Notification progress is emitted only when the whole-number percentage changes.

## ANT boundary

`AntServiceClient` owns Android service discovery, binding, broadcast receivers, channel commands,
and idempotent cleanup. It forwards incoming protocol messages without holding a Fragment or
Activity.

`AntMessageParser` validates and decodes ANT pages. `AntWeightSession` is the Android-free protocol
state machine. `AntWeightController` coordinates the service and state machine, applies timeouts,
persists only protocol-complete measurements, and reports success only after persistence completes
through `AntWeightListener`. The activity-scoped ViewModel owns the controller so a measurement can survive
Activity recreation; `WeightFragment` attaches only while its UI is active and the controller's
profile matches the selected profile. Rendering, editing, and upload actions use that same selected
controller boundary. A delivered completion remains editable across recreation and navigation for
its profile, then is discarded when the user selects another profile.

Built-in ANT support does not depend on USB. External ANT adapters use Android USB host mode, which
is advertised as an optional capability so distribution does not exclude built-in-ANT devices.
In-process networking is HTTPS-only; browser fallbacks for ANT service installation also use HTTPS.

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

## Automated verification boundary

Local JVM tests own Android-independent contracts and repository failure injection. The
`androidTest` smoke suite exercises Android resource binding, selected-profile rebinding, editor
cancel navigation, notification-to-MFA event delivery, Activity recreation, and the merged
USB/cleartext manifest policy. GitHub Actions runs
the JVM suite, lint, the minified release build, and test-APK compilation, then executes the smoke
suite serially on Gradle-managed API 27 and API 35 devices. Physical ANT hardware, authenticated
Garmin behavior, document-provider implementations, API 23 compatibility, and destructive process
termination remain explicit manual release gates.

## Remaining release verification

Automated tests cover persistence compatibility, metric behavior, FIT generation, Garmin
authentication/renewal decisions, and ANT parsing/state transitions. They cannot establish behavior
of external services or hardware. Before release, complete `docs/release-checklist.md`, including
physical ANT measurements, MFA, history download, email sharing, backup restoration, and a complete
background token-renewal cycle.
