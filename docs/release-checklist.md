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

Phase 3 APK-reduction verification on 2026-07-14: 210 test executions passed, lint reported no
issues, and the minified unsigned release APK built successfully. `ProviderInstaller` remains for
API 23–28, while the full Google Play services Base and Tasks layers were removed. The connected
API 36 device skips this legacy path, so the API 23–28 checks below remain pending.

Phase 4 APK-reduction verification on 2026-07-14: 217 test executions passed, lint reported no
issues, and the minified unsigned release APK built successfully. The app-owned FIT writer passes
official SDK decoding, value, boundary, endianness, and CRC tests. A real Garmin Connect upload
remains pending.

Phase 5 APK-reduction verification on 2026-07-14: 218 test executions passed, lint reported no
issues, and the minified unsigned release APK built successfully. The original Garmin and bone-mass
vectors replaced the app's last three raster resources. Their rendering checks below remain pending.

Phase 6 APK-reduction verification on 2026-07-14: 218 test executions passed, lint reported no
issues, and the minified unsigned release APK built successfully. Material UI infrastructure was
replaced by focused AppCompat/AndroidX layouts and controls. The UI checks below remain pending
because no device or emulator was modified during this phase.

Phase 7 clean verification on 2026-07-14: a fresh clone at `b498361` passed all 218 unit tests,
`lintDebug`, `lintVitalRelease`, `assembleRelease`, and `bundleRelease`. Bundletool confirmed
language/density delivery and no ABI splits. No application was installed and no device, scale, or
authenticated service state was changed, so the manual checks below remain pending.

## Distribution artifacts

- [x] Clean unsigned universal APK: 1,206,013 bytes; SHA-256
      `2c45e039560e4ed63476f3f579d22e64854c0f9511541304e476f408838d314e`.
- [x] Clean unsigned release AAB: 1,999,431 bytes; SHA-256
      `331ccb93123faa8e66840cb51868877357ea14d71bdc9d57aabc01929350f5b6`.
- [x] Confirm the APK and AAB contain no native libraries and require no ABI splits.
- [x] Inspect bundletool language/density splits and representative download estimates.
- [ ] Sign and verify the universal APK with the release key; record its final size and SHA-256.
- [ ] Sign the AAB with the upload key and verify the Play Console-generated delivery artifacts.

## Android lifecycle and compatibility

- [ ] Smoke-test API 23, 29, 33, and 37 where devices or emulators are available.
- [ ] Rotate the device during editing and active operations.
- [ ] Move the app between foreground and background during active operations.
- [ ] Verify state restoration after process recreation.

## Data and navigation

- [ ] Open the drawer from the toolbar and with an edge swipe; select every destination and confirm
      the title, fragment, icon, and single checked row are correct.
- [ ] Rotate and recreate the Activity on every top-level and edit screen; confirm the correct parent
      drawer row remains checked.
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

- [ ] On API 23–28, sign in, upload a measurement, renew a token, and download history with current
      Google Play services installed.
- [ ] On API 23–28 with repairable Google Play services, confirm the supplied recovery action opens,
      no Garmin HTTPS request starts before recovery completes, and retry succeeds afterward.
- [ ] On API 23–28 without Google Play services, confirm Garmin workflows stop safely without a
      crash or HTTPS request.
- [ ] Confirm token status and expiration timestamps are accurate.
- [ ] Log in with MFA and verify both code entry and cancellation.
- [ ] Upload with active and expired access tokens.
- [ ] Upload representative app-generated FIT files to Garmin Connect: one weight-only file and one
      containing every supported optional measurement.
- [ ] Observe background renewal across a complete access-token cycle.
- [ ] Confirm the token-refresh job survives process termination and device reboot.
- [ ] Confirm an offline refresh retries with backoff after connectivity returns.
- [ ] Confirm deleting a user or removing their Garmin credentials cancels the pending refresh.
- [ ] Confirm rejected renewal credentials do not schedule another refresh.
- [ ] Confirm expired access is renewed using the saved OAuth1 credentials without another MFA
      prompt.
- [ ] Download Garmin history and confirm existing measurements are not duplicated.

## Display and export

- [ ] Verify the restored bone-mass icon in weight editing, history rows, metric cards, and graph
      menus at representative screen densities in day and night themes.
- [ ] Verify the restored Garmin icon in user rows and in the Garmin history notification, including
      both its small and large notification forms on API 23 and a current API.
- [ ] On API 23 and a current API, verify toolbar/menu layout, weight cards, drawer width/insets, and
      the circular add button in both day and night themes.
- [ ] With TalkBack or another accessibility service, confirm drawer rows expose their checked state,
      all navigation and add targets are easy to activate, and the add action is announced correctly.
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
