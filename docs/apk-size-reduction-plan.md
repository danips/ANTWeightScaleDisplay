# APK Size Reduction Plan

This document tracks incremental reduction of the ANT+ Weight Scale Display release APK. It is the
source of truth for this work: complete one phase at a time, update its checkboxes and measurements,
and keep every commit buildable and behaviorally compatible unless a trade-off is explicitly
approved in the decision log.

## Current status

- Overall status: In progress
- Current phase: Phase 7 — Final verification and distribution optimization
- Last updated: 2026-07-14
- Plan created from commit: `fdb4d97`
- Branch at plan creation: `main`
- Working tree before plan creation: Clean
- Unsigned release APK baseline: 2,293,728 bytes
- Baseline SHA-256: `0f709cb2f8f7c30ce0bd9c3d32b4914e4e1556d0a99b6a27d7ded814db2d8c12`
- Compressed `classes.dex` baseline: 1,002,986 bytes
- Uncompressed `resources.arsc` baseline: 930,796 bytes
- Current clean unsigned release APK: 1,206,013 bytes
- Current APK reduction from baseline: 1,087,715 bytes (47.4%)

Update the current phase, date, measurements, and relevant commit after completing every phase.

## Goals

1. Reduce the universal release APK while preserving current features and supported languages.
2. Remove disproportionately expensive dependencies when a small, testable platform implementation
   can provide the required behavior.
3. Keep Garmin authentication, upload, FIT-file generation, and background renewal reliable.
4. Measure actual minified release APKs rather than judging dependencies by downloaded JAR/AAR size.
5. Preserve useful diagnostics and maintainability when their APK cost is negligible.
6. Keep old-Android compatibility decisions explicit instead of silently weakening TLS behavior.

## Size targets

- First target: no more than 2,010,000 bytes after restricting dependency locales.
- Feature-preserving target: approximately 1.75–1.85 MB after replacing WorkManager and the FIT SDK.
- Modern-Android target: measure after deciding whether a separate API-29+ build may omit Google Play
  Services.
- Stretch target: consider deeper UI dependency changes only after the earlier phases are complete.

Targets after Phase 1 are estimates because savings from different changes overlap and cannot be
added directly. Every phase must replace its estimate with a measured result.

## Non-goals

- Do not implement a cryptographic security provider.
- Do not replace OAuth Signpost solely for a theoretical saving below 5 KB.
- Do not remove translations from the universal APK without an explicit distribution decision.
- Do not copy the generic Garmin FIT SDK source into production and assume that this reduces size;
  R8 already removes most unreachable SDK code.
- Do not replace MPAndroidChart or the Material/AppCompat UI stack before measuring a prototype and
  accounting for the replacement code.
- Do not remove source and line-number diagnostics for a measured saving of only hundreds of bytes.
- Do not combine size work with unrelated UI or persisted-data-format changes.

## Working rules

1. Make one independently useful size change per commit.
2. Run `./gradlew testDebugUnitTest` after every production-code change.
3. Run `./gradlew lintDebug` and `./gradlew assembleRelease` before completing every phase.
4. Record APK size and SHA-256 after every release build used as a phase result.
5. Compare minified release builds made from the same commit and toolchain configuration.
6. Treat builds that remove functionality or use no-op stubs as upper-bound experiments, not expected
   production savings.
7. Add characterization tests before replacing protocol, authentication, or scheduling code.
8. Keep SDKs as test-only dependencies when they are useful for validating an app-owned format.
9. Record manual device/API/service checks in `docs/release-checklist.md`.
10. Preserve unrelated working-tree changes and document deviations in the decision log.

## Measurement commands

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
stat -c '%s' app/build/outputs/apk/release/app-release-unsigned.apk
sha256sum app/build/outputs/apk/release/app-release-unsigned.apk
unzip -lv app/build/outputs/apk/release/app-release-unsigned.apk \
  | awk '$8 == "classes.dex" || $8 == "resources.arsc" { print }'
```

The signed APK and Play-delivered split APKs have different packaging overhead. Use the unsigned
release APK above for consistent phase-to-phase comparisons, then measure the real distribution
artifact in the final phase.

## Measured opportunities

All measurements below used controlled minified release builds from the baseline. Rows that removed
functionality represent a maximum saving before replacement code is added.

| Experiment | Resulting APK | Measured maximum saving | Baseline percentage | Notes |
|---|---:|---:|---:|---|
| Keep only English resources | 1,780,708 | 513,020 bytes | 22.4% | Distribution/language trade-off |
| Keep the app's current locales | 2,005,012 | 288,716 bytes | 12.6% | Expected low-risk first change |
| Remove Google Play Services provider support | 2,069,706 | 224,022 bytes | 9.8% | Impacts API 23–28 TLS handling |
| Remove WorkManager scheduling | 2,085,679 | 208,049 bytes | 9.1% | Replacement scheduling adds code back |
| Remove Garmin FIT generation | 2,245,416 | 48,312 bytes | 2.1% | Specialized encoder expected to net 40–45 KB |
| Remove MPAndroidChart and graph implementation | 2,249,028 | 44,700 bytes | 1.9% | Includes app graph code; not library-only |
| Remove OAuth Signpost | 2,289,380 | 4,348 bytes | 0.2% | Correct replacement adds most bytes back |
| Remove source/line-number metadata | 2,293,164 | 564 bytes | 0.02% | Keep diagnostics |

Other observations:

- At the original baseline, all packaged bitmap files together occupied approximately 94 KB.
- At the original baseline, `ic_gc.png` was 63,601 bytes in source but approximately 13,987 bytes after Android resource
  processing. A lossless WebP experiment was approximately 10 KB; a quality-90 WebP was
  approximately 2 KB.
- Kotlin built-in metadata occupies approximately 12 KB compressed. Do not exclude it blindly;
  transitive libraries may rely on it.
- Independent savings overlap, particularly locale filtering and dependencies with translated
  resources. Never calculate a release target by simply summing the table.

## Progress summary

- [x] Phase 0 — Record baseline and upper-bound experiments
- [x] Phase 1 — Restrict packaged locales
- [x] Phase 2 — Replace WorkManager token-refresh scheduling
- [x] Phase 3 — Decide the Google Play Services/API-level strategy
- [x] Phase 4 — Replace the production Garmin FIT SDK with a specialized encoder
- [x] Phase 5 — Restore app-owned vector drawables
- [x] Phase 6 — Evaluate deeper UI dependency reduction
- [ ] Phase 7 — Final verification and distribution optimization

---

## Phase 0 — Record baseline and upper-bound experiments

Status: Completed<br>
Completed: 2026-07-14<br>
Commit: Documentation to be committed with this plan

### Objective

Establish the current optimized APK composition and realistic ceilings for the main dependencies.

### Completed work

- [x] Build the current minified release APK.
- [x] Record total APK, compressed DEX, and resource-table sizes.
- [x] Measure English-only and supported-locale resource configurations.
- [x] Measure upper bounds for WorkManager, Google Play Services, Garmin FIT, Signpost, and charts.
- [x] Measure the cost of source/line-number metadata.
- [x] Inspect packaged bitmap and Kotlin metadata totals.
- [x] Confirm that measurement experiments did not modify the project working tree.

### Completion notes

- The resource table and compressed DEX account for most of the APK, so dependency and locale work
  has substantially more potential than image recompression.
- Raw dependency archive sizes were rejected as a planning metric because R8 retains only reachable
  production code.
- The table in “Measured opportunities” contains the reproducible results from this phase.

---

## Phase 1 — Restrict packaged locales

Status: Completed; device locale smoke test deferred to the release checklist<br>
Completed: 2026-07-14<br>
Commit: `a2ed699`

### Objective

Preserve every translation currently provided by the app while preventing dependencies from adding
translations for unsupported locales.

### Proposed configuration

```gradle
def localeDirectoryPattern =
        ~/^values-([a-z]{2}(?:-r[A-Z]{2})?|b\+[A-Za-z0-9+]+)$/
def translatedLocales = fileTree('src/main/res') {
    include 'values-*/strings.xml'
}.files.collect { stringsFile ->
    def matcher = stringsFile.parentFile.name =~ localeDirectoryPattern
    matcher.matches() ? matcher.group(1) : null
}.findAll().toSet()
def supportedLocales = (['en'] + translatedLocales).sort()

android {
    androidResources {
        localeFilters += supportedLocales
    }
}
```

### Tasks

- [x] Derive the configuration list from every `values-*` directory and include the default English
      resources.
- [x] Add dynamically generated `androidResources.localeFilters` to the application module.
- [x] Build the release APK and confirm a result near the measured 2,005,012-byte experiment.
- [x] Inspect the packaged resource configurations and confirm that each supported locale remains.
- [x] Derive translated locales at build time so adding a translation requires no filter update.
- [x] Run automated verification.
- [ ] Check at least English, one right-to-left locale, Portuguese, and one non-Latin locale on a
      device or emulator.

### Acceptance criteria

- All current app translations remain available.
- Unsupported dependency-only locales are absent.
- Automated verification passes.
- The unsigned release APK is no more than 2,010,000 bytes unless a documented toolchain difference
  explains the result.

### Completion notes

- The unsigned release APK is 2,005,012 bytes, 288,716 bytes (12.6%) smaller than the baseline.
- SHA-256: `42cf111a6ddea00efb21d380f5e48c079fc3ebe3943e9ebbd6ba957c957c7243`.
- Compressed `classes.dex`: 1,002,986 bytes; uncompressed `resources.arsc`: 642,080 bytes.
- AGP's deprecated `resourceConfigurations` property was replaced with
  `androidResources.localeFilters`. The filter discovers locale-qualified `strings.xml`
  directories during Gradle configuration; only default English remains explicit because the
  unqualified `values` directory has no locale.
- `aapt dump configurations` reports the default configuration plus `ar`, `ca`, `cs`, `de`, `el`,
  `es`, `fr`, `gl`, `hu`, `it`, `iw`, `ja`, `nb`, `nl`, `pl`, `pt`, `pt-rPT`, `ru`, `sv`, `tr`,
  `uk`, and `zh`. No locale outside the configured supported locale families remains.
- `testDebugUnitTest`, `lintDebug`, and `assembleRelease` all pass.
- No device or emulator was available. Runtime checks for English, Arabic, Portuguese, and Japanese
  remain in `docs/release-checklist.md` and must pass before release.

---

## Phase 2 — Replace WorkManager token-refresh scheduling

Status: Completed; device lifecycle checks deferred to the release checklist<br>
Completed: 2026-07-14<br>
Commit: `1dcadae`

### Objective

Replace the single-purpose WorkManager/Room scheduling stack with an API-23-compatible platform
implementation while retaining reliable Garmin OAuth renewal.

### Proposed design

- Use `JobScheduler` and a small `JobService` because the application already requires API 23.
- Use one stable job ID per user and handle the possibility of UUID hash collisions explicitly.
- Require network connectivity.
- Preserve the six-hour refresh lead time and exponential retry behavior.
- Persist scheduled work across process death and reboot; explicitly retain any permissions that
  were previously supplied by WorkManager's merged manifest.
- Cancel jobs when a user or Garmin connection is removed.
- Schedule the next refresh only after the running refresh has finished successfully.

### Tasks

- [x] Add tests for delay calculation, credential eligibility, stable job IDs, collision handling,
      and retry timing.
- [x] Add an app-owned `JobService` for Garmin token renewal.
- [x] Replace `GarminTokenRefreshScheduler` WorkManager calls with `JobScheduler` calls.
- [x] Preserve network, persistence, cancellation, and backoff semantics.
- [x] Remove `GarminTokenRefreshWorker` after all callers and tests have migrated.
- [x] Remove `androidx.work:work-runtime` from production dependencies.
- [x] Inspect the merged manifest for required job/reboot permissions and removed WorkManager
      initializers.
- [x] Run automated verification and measure the release APK.
- [ ] Manually verify renewal after process termination, offline retry, reboot, user deletion, and
      rejected credentials.

### Acceptance criteria

- No WorkManager or Room runtime classes remain in the release mapping.
- Existing token-renewal behavior and failure classification are preserved.
- The expected net saving is approximately 195–205 KB after replacement code.
- Manual lifecycle and network checks are recorded before release.

### Completion notes

- `GarminTokenRefreshJobService` performs renewal off the main thread and uses the existing
  success, temporary-failure, rejected-credential, missing-user, and repository-failure outcomes.
- Jobs require a network, persist across reboot, start six hours before access-token expiry, and
  use a 30-minute exponential retry backoff. `RECEIVE_BOOT_COMPLETED` is now explicit.
- Each user receives a deterministic job ID. Pending-job extras preserve an existing assignment,
  and occupied IDs are probed so colliding UUID hashes cannot replace another user's refresh.
- A successful renewal replaces the completed running job with the next expiry-based job. Temporary
  failures request a framework retry; missing or rejected credentials finish without rescheduling.
- The release dependency graph, R8 mapping, and merged manifest contain no WorkManager or Room
  runtime classes, worker, initializer, service, or receiver.
- The unsigned release APK is 1,800,143 bytes, 204,869 bytes (10.2%) smaller than Phase 1 and
  493,585 bytes (21.5%) smaller than the baseline.
- SHA-256: `0462b449c00393283783a7726b3a345d24e0dca1a0952218b4b7a4863e69e18e`.
- Compressed `classes.dex`: 807,738 bytes; uncompressed `resources.arsc`: 641,812 bytes.
- A clean `testDebugUnitTest`, `lintDebug`, and `assembleRelease` all pass.
- An API 36 device became available with an existing debuggable v3.23 installation. Stateful checks
  were not run because they require replacing that build, using its saved data, controlling its
  network, and rebooting the device. Process-death, offline-retry, reboot, deletion, and
  rejected-credential checks remain in `docs/release-checklist.md` and must pass before release.

---

## Phase 3 — Decide the Google Play Services/API-level strategy

Status: Completed<br>
Completed: 2026-07-14<br>
Commit: `51985ea`

### Objective

Decide whether the approximately 224 KB Google Play Services cost is still required for updating the
TLS provider on Android 6–9.

### Options

1. Keep the current dependency and API-23 compatibility.
2. Raise the application minimum to API 29 and remove provider installation.
3. Publish a smaller API-29+ artifact and retain a legacy API-23 artifact.
4. Remove provider installation on API 23–28 only after explicit TLS/security compatibility review
   and testing. This is not the default recommendation.

### Tasks

- [x] Determine whether distribution analytics for API 23–28 are available.
- [x] Confirm every call site and verify that provider installation is already skipped on API 29+.
- [x] Choose and record one option in the decision log.
- [x] Retain `ProviderInstaller` while removing the unneeded full availability-dialog and Tasks
      dependency layers.
- [ ] Test Garmin sign-in, renewal, upload, and history download on the minimum supported API.
- [x] Build and measure every release artifact affected by the decision.

### Acceptance criteria

- The support/security trade-off is explicit and documented.
- No home-grown cryptographic provider is introduced.
- Garmin HTTPS workflows pass on every retained Android version.

### Completion notes

- No distribution analytics are present in the repository, so there is no evidence that API 23–28
  can be dropped. The app retains its API 23 minimum and continues installing the updated security
  provider before Garmin HTTPS work on API 23–28.
- Android 10 and newer continue skipping the installer. Android 10 enables TLS 1.3 by default and
  exposes the platform Conscrypt TLS API, so the existing API 29 boundary remains appropriate.
- The app now depends directly on the officially published `play-services-basement` artifact that
  contains `ProviderInstaller`. It no longer packages `play-services-base` or
  `play-services-tasks`.
- Repairable installation failures in interactive workflows launch the recovery `Intent` supplied
  by Google Play services. Garmin upload and history networking no longer starts when installation
  fails; users can retry after completing recovery. Background token renewal defers through the
  existing job backoff instead of communicating without the updated provider.
- A separate API-29+ artifact was rejected for now: it would add release and support complexity
  while the smaller dependency preserves one universal APK and API 23 compatibility.
- The unsigned release APK is 1,719,980 bytes, 80,163 bytes (4.5%) smaller than Phase 2 and
  573,748 bytes (25.0%) smaller than the baseline.
- SHA-256: `356e3678332deba870b5a8b07b45ac4cfe447b68312f632b57b120aeae75daea`.
- Compressed `classes.dex`: 765,612 bytes; uncompressed `resources.arsc`: 605,624 bytes.
- The release dependency graph and R8 mapping retain `ProviderInstaller` and
  `GoogleApiAvailabilityLight`; full `GoogleApiAvailability` and Tasks APIs are absent.
- All 210 unit tests, lint, and the minified release build pass. API 23–28 Garmin workflow and
  recovery checks remain in `docs/release-checklist.md`; the connected API 36 device cannot
  exercise the legacy provider-installation path.

---

## Phase 4 — Replace the production Garmin FIT SDK with a specialized encoder

Status: Completed<br>
Completed: 2026-07-14<br>
Commit: `87b468a`

### Objective

Generate only the file ID and weight-scale FIT messages required by the app without shipping the
generic FIT SDK runtime.

### Required behavior

- FIT 2.0 header and profile version.
- Correct little-endian message definitions and values.
- File ID message with the current type, manufacturer, product, and serial number.
- Weight-scale timestamp and every currently supported optional measurement.
- Correct scale/offset and invalid-value rules.
- Header and file CRC validation.

### Tasks

- [x] Review the applicable Garmin FIT source/protocol license before copying or adapting source.
- [x] Characterize the current output fields, scales, definitions, timestamp, and CRC behavior.
- [x] Implement a focused encoder without generic message/profile infrastructure.
- [x] Move `com.garmin:fit` from `implementation` to `testImplementation`.
- [x] Use the official decoder in tests to check integrity and every generated value.
- [x] Add boundary tests for missing optional fields, maximum values, rounding, and dates.
- [ ] Upload representative generated files to Garmin Connect in a manual release check.
- [x] Run automated verification and record the net APK saving.

### Acceptance criteria

- The Garmin SDK is absent from production runtime mapping but remains available to tests.
- Official decoding reports a valid FIT file and the expected weight-scale values.
- Garmin Connect accepts representative files.
- Expected net saving is approximately 40–45 KB.

### Completion notes

- The supplied Garmin SDK source is governed by the restrictive FIT Protocol License, including
  source-distribution conditions that are unsuitable for copying into this GPLv3 project. No SDK
  source was copied or adapted. The production writer was implemented independently from the
  publicly documented FIT file structure and characterized output; the official SDK remains an
  unmodified test oracle.
- The encoder writes a 14-byte FIT 2.0 header with profile 21.205, little-endian definitions and
  values, the `file_id` message (weight type, Tanita manufacturer, product 1, serial 1), and one
  `weight_scale` message.
- Weight, fat, hydration, bone, and muscle use scale 100; active metabolism uses scale 4; BMI uses
  scale 10; physique, visceral-fat rating, and metabolic age use unsigned bytes. Existing
  float-before-scaling rounding, basal-metabolism preference, and optional `-1` sentinels are
  preserved.
- Timestamps use seconds from the FIT epoch. Values that would use a reserved invalid encoding,
  non-finite/negative measurements, and dates outside the encodable range are rejected rather than
  silently wrapping.
- Header and complete-file CRCs are generated by the focused writer. Tests verify official decoder
  integrity and detect deliberate header and data corruption.
- Eight FIT tests cover every generated field, file identity, little-endian layout, missing optional
  fields, maximum values, rounding, FIT epoch and maximum dates, invalid ranges, and both CRCs.
- `com.garmin:fit:21.205.0` is now test-only. The release dependency graph and R8 mapping contain no
  Garmin SDK classes, while the unit-test runtime retains the official decoder.
- The unsigned release APK is 1,674,552 bytes, 45,428 bytes (2.6%) smaller than Phase 3 and
  619,176 bytes (27.0%) smaller than the baseline.
- SHA-256: `6adc29702877c43a4a1c1c249a5c8e89160c664e371aefff2431931df0de593a`.
- Compressed `classes.dex`: 720,188 bytes; uncompressed `resources.arsc`: 605,624 bytes.
- All 217 unit tests, lint, and the minified release build pass. Uploading representative files to
  Garmin Connect remains a manual release check because no authenticated Garmin test session was
  used during this phase.

---

## Phase 5 — Optimize remaining bitmap assets

Status: Completed; device rendering checks deferred to the release checklist<br>
Completed: 2026-07-14<br>
Commit: Pending

### Objective

Reduce bitmap size without visibly degrading icons or duplicating density resources unnecessarily.

### Tasks

- [x] Use the built release APK—not source file size—to rank packaged bitmaps.
- [x] Restore the original `ic_gc`, `ic_bone_mass`, and `ic_bone_mass_mini` vector drawables.
- [x] Remove their xxxhdpi PNG replacements.
- [x] Confirm that no app-owned PNG, WebP, or JPEG resources remain.
- [x] Preserve the Garmin notification large icon by rendering its vector to a bitmap at runtime.
- [x] Run automated verification and measure the release APK.
- [ ] Check the restored bone-mass and Garmin icons at supported densities and themes on a device.

### Acceptance criteria

- No visible artifacts appear at supported screen densities.
- Transparent backgrounds and notification rendering remain correct.
- Each committed conversion has a measured APK benefit.

### Completion notes

- Restored `ic_bone_mass.xml`, `ic_bone_mass_mini.xml`, and `ic_gc.xml` exactly from commit
  `223d679` immediately before their replacement in `b384aca`. Deleted the corresponding xxxhdpi
  PNG files.
- `BitmapFactory.decodeResource` cannot reliably decode an XML vector. The Garmin history
  notification now renders `ic_gc` through a `Drawable` and `Canvas`, preserving its large icon
  while allowing the PNG to be removed.
- No app-owned raster image remains under `app/src/main/res`; remaining packaged PNG and nine-patch
  resources belong to dependencies and are outside the app artwork optimization scope.
- The unsigned release APK is 1,206,013 bytes, 15,264 bytes (1.2%) smaller than Phase 6 and
  1,087,715 bytes (47.4%) smaller than the original baseline.
- SHA-256: `82266f8a848036ae5f9a35907af98128adda07cf10097dc5fa7672297fd052bb`.
- Compressed `classes.dex`: 612,593 bytes; uncompressed `resources.arsc`: 328,688 bytes.
- All 218 unit tests, lint, and the minified release build pass. Visual checks for the restored
  vectors and Garmin notification remain in `docs/release-checklist.md`.
- No further Phase 5 conversion is justified unless new raster artwork is added. Lossy WebP and
  launcher-image experiments are obsolete because the application resource tree is now vector-only.

---

## Phase 6 — Evaluate deeper UI dependency reduction

Status: Completed; device UI checks deferred to the release checklist<br>
Completed: 2026-07-14<br>
Commit: `a1ee434`

### Objective

Determine whether replacing Material components or other UI infrastructure offers enough additional
saving to justify the maintenance and regression cost.

### Candidates

- Material theme and widgets: navigation view, app bar, cards, and floating action button.
- AppCompat/platform UI rewrite.
- MPAndroidChart replacement.
- View binding removal.

### Tasks

- [x] Build a controlled prototype before editing production code.
- [x] Add explicit direct dependencies for AndroidX widgets the app uses rather than relying on
      Material transitive dependencies.
- [x] Measure the replacement's net APK size, including app-owned UI code and resources.
- [x] Document accessibility, theme, navigation, configuration, and old-API regressions.
- [x] Proceed only when the measured benefit exceeds an agreed threshold and the UI can be tested
      comprehensively.

### Default decisions

- Keep MPAndroidChart: removing the library and existing graph implementation saved at most 44.7 KB.
- Keep OAuth Signpost: its absolute ceiling is 4.35 KB and a correct replacement adds most of it
  back.
- Keep source and line-number metadata: removing it saved only 564 bytes.

### Completion notes

- A clean controlled prototype replaced Material while retaining the app's navigation, toolbar,
  cards, and add action. It produced a 1,221,101-byte APK versus the 1,674,556-byte clean Phase 6
  baseline, a net prototype saving of 453,455 bytes (27.1%). This was large enough to justify the
  production change.
- `Theme.MaterialComponents` became the corresponding AppCompat day/night theme. Material cards
  became AndroidX `CardView`; the app bar became a simple toolbar/content layout; and the floating
  action button became a 56dp AppCompat image button with a circular background, elevation, ripple,
  and its existing content description.
- `NavigationView` became an app-owned 304dp drawer containing five 48dp checked rows. It preserves
  icons, localized labels, single selection, tap feedback, checked accessibility state, drawer
  gestures, and toolbar toggle behavior. Selection is reconstructed after configuration/process
  restoration, including the weight, goal, and user editor screens; a unit test covers those editor
  mappings.
- CardView 1.0.0, ConstraintLayout 2.2.1, and RecyclerView 1.2.1 are now explicit dependencies.
  Material and its unused coordinator, transition, animation, ViewPager2, shape, and translated
  resource infrastructure are absent from the release mapping and artifact.
- The accepted production APK is 1,221,277 bytes: 453,279 bytes smaller than the controlled clean
  Phase 6 baseline and 1,072,451 bytes (46.8%) smaller than the original baseline. The four-byte
  difference between the current clean baseline and Phase 4's recorded artifact is release version
  control metadata.
- SHA-256: `b7ad9c2dc5801ef80834f9d8e20612380360bc618d4cfa81c7f670e9a0fa59d0`.
- Compressed `classes.dex`: 612,546 bytes; uncompressed `resources.arsc`: 328,688 bytes.
- All 218 unit tests, lint, and the minified release build pass. Device checks remain for day/night
  themes, right-to-left layout, drawer state and insets, touch/accessibility behavior, cards, toolbar,
  and the custom add button on API 23 and a current API.
- The replacement intentionally does not reproduce unused Material behavior: adaptive navigation
  menu presentation, app-bar scrolling behaviors, checkable/stroked Material cards, or FAB motion
  transformations. The app did not configure those behaviors. MPAndroidChart, AppCompat, and view
  binding remain because replacing them would require a much broader UI rewrite for a substantially
  smaller known or expected return.

---

## Phase 7 — Final verification and distribution optimization

Status: Automated and distribution verification completed; release signing and device/service checks pending<br>
Updated: 2026-07-14

### Objective

Verify the combined result and minimize actual user downloads without compromising the universal
artifact or release diagnostics.

### Tasks

- [x] Run the full unit-test, lint, and release-build sequence from a clean checkout.
- [ ] Complete all affected items in `docs/release-checklist.md`.
- [x] Record final clean unsigned APK and AAB sizes and SHA-256 values.
- [ ] Record the release-key-signed APK size and SHA-256 value.
- [x] Build an Android App Bundle and inspect bundletool-generated language and density splits.
- [x] Confirm that ABI splitting provides no benefit while the app has no native libraries.
- [x] Decide whether GitHub/F-Droid distribution needs a universal APK, localized APKs, or separate
      modern/legacy artifacts.
- [x] Compare final size against every target and document any shortfall.
- [x] Document the release-note impact of support and distribution decisions.

### Acceptance criteria

- Automated and required manual checks pass.
- Every distributed artifact has a recorded purpose, minimum API, size, and checksum.
- The final plan status and results log are complete.

### Completion notes

- A fresh clone of commit `b498361` passed all 218 unit tests, `lintDebug`, `lintVitalRelease`,
  `assembleRelease`, and `bundleRelease`. No previous build output was available to make a task
  incorrectly pass from stale state.
- The clean unsigned universal APK is 1,206,013 bytes. SHA-256:
  `2c45e039560e4ed63476f3f579d22e64854c0f9511541304e476f408838d314e`. Compressed
  `classes.dex` is 612,593 bytes and uncompressed `resources.arsc` is 328,688 bytes. Its hash differs
  from the pre-commit Phase 5 artifact because release version-control metadata now identifies
  `b498361`; its size is unchanged.
- The unsigned release AAB is 1,999,431 bytes. SHA-256:
  `331ccb93123faa8e66840cb51868877357ea14d71bdc9d57aabc01929350f5b6`. Its raw archive size is not
  an install/download comparison because it contains the 10.5 MB uncompressed R8 mapping and other
  Play metadata.
- The repository has no release signing configuration or signing credentials. A temporary
  debug-signed universal APK measured signature overhead only: 1,248,184 bytes, SHA-256
  `c9c18affe81de7ab357dc3eab053adbbbb0798a485cef266ccc736273a595ad9`. It verifies with APK
  signature schemes v1, v2, and v3, but it is not a distributable release artifact. The actual
  release-key-signed APK and signed upload AAB remain release-automation tasks.
- Bundletool 1.18.3 found 21 translated-language splits and seven density splits. It estimated
  compressed Play downloads from 824,598 to 867,630 bytes across supported configurations.
  Representative estimates were 849,892 bytes for API 23 English/xhdpi, 828,699 bytes for API 37
  English/xxhdpi, and 837,143 bytes for API 37 Portuguese/xxhdpi.
- Neither the APK nor AAB contains a `lib/` entry or native library. ABI splits would therefore add
  configuration and release complexity without saving a byte.
- Google Play should receive one release/upload-key-signed AAB and use Play App Signing plus
  language/density delivery. GitHub should publish one release-key-signed universal APK; F-Droid can
  build and sign the same universal variant. Separate localized APKs, ABI APKs, and API-29+ variants
  are not justified at the current 1.21 MB universal size.
- The final universal APK beats the 2,010,000-byte first target by 803,987 bytes, the lower bound of
  the 1.75 MB feature-preserving target by 543,987 bytes, and the original 2,293,728-byte baseline
  by 1,087,715 bytes (47.4%). No size target has a shortfall.

### Release-note summary

- The minimum supported API remains 23, the target remains API 37, and no supported language or app
  feature was removed for size reduction.
- Play distribution uses an AAB for language and density optimization; direct/F-Droid distribution
  continues to use one universal APK.
- Users receive the same functionality in a universal APK that is 47.4% smaller than the original
  baseline; representative Play downloads are approximately 0.83–0.85 MB.
- Manual ANT, Garmin, lifecycle, locale, restored-icon, and accessibility checks plus real release
  signing remain required before publication. Because those checks are still open, this plan must
  remain until the release checklist is completed.

---

## Results log

| Date | Phase/experiment | Commit | APK bytes | Change from previous | SHA-256 | Notes |
|---|---|---|---:|---:|---|---|
| 2026-07-14 | Baseline | `fdb4d97` | 2,293,728 | — | `0f709c…d8c12` | Current minified unsigned release |
| 2026-07-14 | Phase 1 — supported locales | `a2ed699` | 2,005,012 | -288,716 | `42cf11…7243` | Tests, lint, release build, and packaged configurations verified |
| 2026-07-14 | Phase 2 — platform refresh job | `1dcadae` | 1,800,143 | -204,869 | `0462b4…9e18e` | Clean verification; device lifecycle checks deferred |
| 2026-07-14 | Phase 3 — retained provider support | `51985ea` | 1,719,980 | -80,163 | `356e36…5daea` | Kept API 23; removed full Base and Tasks layers |
| 2026-07-14 | Phase 4 — focused FIT encoder | `87b468a` | 1,674,552 | -45,428 | `6adc29…e593a` | SDK retained for tests only; Garmin Connect upload pending |
| 2026-07-14 | Phase 6 — AppCompat UI | `a1ee434` | 1,221,277 | -453,275 | `b7ad9c…a59d0` | Material removed; automated verification passed; device UI checks pending |
| 2026-07-14 | Phase 5 reevaluation — restored vectors | `b498361` | 1,206,013 | -15,264 | `82266f…052bb` | All app-owned raster drawables removed; device rendering checks pending |
| 2026-07-14 | Phase 7 — clean verification | `b498361` | 1,206,013 | 0 | `2c45e0…d314e` | Clean tests/lint/APK/AAB passed; hash changed with commit metadata |

Add one row for every accepted phase result. Temporary no-op experiments belong in the measured
opportunities table rather than this results log.

## Decision log

| Date | Decision | Reason | Revisit condition |
|---|---|---|---|
| 2026-07-14 | Start with supported-locale filtering | Largest measured low-risk saving | Revisit list whenever a locale is added |
| 2026-07-14 | Prefer `JobScheduler` for WorkManager replacement | API 23 is already the minimum and only one job family is needed | Revisit if scheduling requirements expand |
| 2026-07-14 | Keep API 23 and provider updates; use `play-services-basement` directly | Security updates remain available on API 23–28 while the smaller official artifact saves 80,163 bytes | Revisit when API 23–28 usage is known to be negligible or the minimum rises to API 29 |
| 2026-07-14 | Implement the FIT writer independently and keep Garmin's SDK test-only | The supplied SDK license is unsuitable for copying source into GPLv3; official decoding still provides compatibility validation | Revisit field/profile constants when upgrading the test SDK or adding FIT fields |
| 2026-07-14 | Keep Signpost by default | Maximum saving is only 4.35 KB | Revisit for maintenance/security reasons, not size |
| 2026-07-14 | Keep MPAndroidChart by default | Maximum measured saving including graph code is 44.7 KB | Revisit only with a planned graph redesign |
| 2026-07-14 | Keep release line metadata | It costs only 564 bytes | Revisit only if measurement changes materially |
| 2026-07-14 | Skip bitmap optimization | Visual validation was unavailable and the remaining saving is small beside the UI dependency opportunity | Revisit during an artwork refresh or with representative screenshot tests |
| 2026-07-14 | Replace Material with a focused AppCompat UI | The behavior-preserving prototype saved 453,455 bytes and the app uses only a simple subset of Material behavior | Revisit if future UI requirements need Material components |
| 2026-07-14 | Reopen Phase 5 and restore the original vectors | The three PNG replacements were the app's only raster resources; restoring the vectors saves 15,264 bytes without lossy conversion | Revisit only when new raster artwork is introduced or a device exposes a rendering problem |
| 2026-07-14 | Use AAB for Play and one universal APK elsewhere | AAB language/density delivery estimates 0.82–0.87 MB downloads; there is no native code to split and the universal APK is only 1.21 MB | Revisit if native code, large density assets, or a materially higher minimum API is introduced |
