# Architecture

The application remains a single Android application module written in Java. The refactor separates
state, persistence, integrations, and protocol logic while retaining the existing fragments and XML
layouts.

## Application state and persistence

`AppStateViewModel` is the lifecycle-aware entry point used by activities and fragments. It delegates
all model lookup and mutation to the process-wide `AppRepository`; UI classes do not read or replace
data files directly. The repository keeps in-memory snapshots, serializes writes on one executor, and
returns `RepositoryResult` values for operations that can fail.

Three codecs define the persisted JSON contract:

- `UserJsonCodec` reads and writes the `users` JSON array.
- `WeightJsonCodec` reads and writes the `history` JSON array.
- `GoalJsonCodec` reads and writes the `goals` JSON array.

`AtomicJsonFile` writes UTF-8 data through a `.tmp` file, keeps a `.del` rollback file during
replacement, synchronizes the file descriptor, and recovers interrupted replacements on the next
read or write. Repository snapshots prevent callers from mutating shared collections accidentally.

## UI boundary

`MainActivity` owns navigation and the activity-scoped `AppStateViewModel`. Fragments render state
and forward user actions; generated View Binding objects exist only for the corresponding view
lifecycle and are cleared in `onDestroyView`. RecyclerView adapters own shallow list snapshots and
use typed row bindings.

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

## ANT boundary

`AntServiceClient` owns Android service discovery, binding, broadcast receivers, channel commands,
and idempotent cleanup. It forwards incoming protocol messages without holding a Fragment or
Activity.

`AntMessageParser` validates and decodes ANT pages. `AntWeightSession` is the Android-free protocol
state machine. `AntWeightController` coordinates the service and state machine, applies timeouts,
persists only protocol-complete measurements, and reports immutable progress through
`AntWeightListener`. The activity-scoped ViewModel owns the controller so a measurement can survive
Activity recreation; `WeightFragment` attaches only while its UI is active.

## Persisted-data compatibility

The internal filenames and JSON structures are intentionally unchanged. Backups remain ZIP archives
whose recognized entries are `users`, `history`, and `goals`.

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
of external services or hardware. Before release, complete the device checklist in
`docs/refactor-plan.md`, including physical ANT measurements, MFA, history download, email sharing,
backup restoration, and a complete background token-renewal cycle.
