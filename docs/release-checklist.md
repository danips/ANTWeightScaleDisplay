# Release checklist

The architecture refactor is implemented and its automated verification passes. Complete the
following checks on representative devices before publishing a release.

## Automated verification

Run these checks from the repository root:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
```

Expected result: all unit tests pass, Android lint reports no issues, and the complete minified
release build succeeds, including `lintVitalRelease`.

Final simplification verification on 2026-07-12: 205 test executions passed, lint reported no
issues, and the minified unsigned release APK built successfully. Device, ANT, Garmin, document
provider, locale, rotation, and process-recreation items below remain intentionally unchecked
because no representative device, scale, or authenticated Garmin account was available.

Phase 2 APK-reduction verification on 2026-07-14: 210 test executions passed, lint reported no
issues, and the minified unsigned release APK built successfully. The new Garmin token-refresh
lifecycle checks below remain pending. An API 36 device was detected, but replacing its existing
debug build, using its saved data, controlling its network, and rebooting it require an explicit
device-test session.

## Android lifecycle and compatibility

- [ ] Smoke-test API 23, 29, 33, and 37 where devices or emulators are available.
- [ ] Rotate the device during editing and active operations.
- [ ] Move the app between foreground and background during active operations.
- [ ] Verify state restoration after process recreation.

## Data and navigation

- [ ] Confirm existing users, measurements, and goals load correctly.
- [ ] Create, edit, select, and delete users.
- [ ] Confirm the selected user is preserved after restart.
- [ ] Confirm user switching updates weight, history, graphs, and goals.
- [ ] Create, edit, and delete manual measurements.
- [ ] Create, edit, display, and delete goals.
- [ ] Restore an actual backup created by a pre-refactor version of the app.
- [ ] Confirm backup and restore preserve every supported field.

## ANT measurements

Repeat the failure-sensitive cases at least three times on a representative supported device.

- [ ] Complete a measurement and confirm it is saved exactly once.
- [ ] Search with the scale off and confirm timeout saves nothing.
- [ ] Cancel during search and during measurement reception.
- [ ] Disconnect the ANT service/radio during search and measurement reception.
- [ ] Submit a partial composition or non-barefoot measurement and confirm it is not saved or
      uploaded.
- [ ] Recreate the Activity during search and confirm progress resumes without duplicate results or
      leaks.
- [ ] Confirm automatic upload runs only after a complete, successful measurement.

## Garmin

- [ ] Confirm token status and expiration timestamps are accurate.
- [ ] Log in with MFA and verify both code entry and cancellation.
- [ ] Upload with active and expired access tokens.
- [ ] Upload a FIT file produced with Garmin FIT SDK 21.205.0.
- [ ] Observe background renewal across a complete access-token cycle.
- [ ] Confirm the token-refresh job survives process termination and device reboot.
- [ ] Confirm an offline refresh retries with backoff after connectivity returns.
- [ ] Confirm deleting a user or removing their Garmin credentials cancels the pending refresh.
- [ ] Confirm rejected renewal credentials do not schedule another refresh.
- [ ] Confirm expired access is renewed using the saved OAuth1 credentials without another MFA
      prompt.
- [ ] Download Garmin history and confirm existing measurements are not duplicated.

## Display and export

- [ ] Verify graphs for every metric, unit system, goal type, and segmental measurement.
- [ ] Confirm fat percentage/mass preference is respected.
- [ ] Confirm segmental values map to the correct body parts.
- [ ] Confirm goals appear on their matching graphs.
- [ ] Open and complete the email-sharing workflow.
- [ ] Confirm CSV and email output use the correct values and units.

## Locales

- [ ] Check English default resources and navigation.
- [ ] Check Arabic right-to-left layout and translated resources.
- [ ] Check Portuguese (Portugal) translated resources.
- [ ] Check Japanese non-Latin translated resources.

## Deferred technical debt

Garmin credentials remain in the backward-compatible `users` JSON file. Moving them to Android
Keystore requires an explicit migration plus backup and recovery design so existing connections are
not silently invalidated.
