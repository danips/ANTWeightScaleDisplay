# 3.27 release checklist

Complete the automated and representative-device checks below before publishing 3.27.

## Release preparation

- [x] Set `versionCode` to 327 and `versionName` to 3.27.
- [x] Update release notes, architecture documentation, and direct-dependency notices.
- [x] Remove the completed code-simplification working plan from current documentation.
- [x] Add the 3.27 publication date to `CHANGELOG.md`.
- [x] Translate the new scale-information labels and battery states for every supported locale.
- [ ] Complete the targeted ANT diagnostics, CSV export, and user-row icon checks below.
- [x] Commit the release-preparation changes and tag the verified commit as `v3.27`.

## Automated verification

Run these checks from the repository root:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleRelease
./gradlew bundleRelease
./gradlew assembleDebugAndroidTest
```

Expected result: all unit tests pass, Android lint reports zero errors, and the complete minified
release APK and AAB builds succeed, including `lintVitalRelease`; the instrumentation APK must also
compile.
Review every lint warning rather than treating warnings as a failed command: update-availability,
complex-vector, and large edit-layout warnings are accepted only while the dependency and device
performance checks below remain current.
GitHub Actions runs those checks on every push and pull request and executes the Android smoke suite
serially on Gradle-managed API 27 and API 35 devices. Where local emulator provisioning is
available, the same connected gate is:

```bash
./gradlew pixel2Api27DebugAndroidTest pixel2Api35DebugAndroidTest
```

Clean 3.27 verification on 2026-08-16: all 316 unit-test executions passed with no failures or
errors; lint reported zero errors; and the minified release APK, release AAB, and instrumentation
APK compiled successfully. The seven reviewed lint warnings are three dependency-update
advisories, three existing complex-vector warnings, and the existing large edit-layout warning.
Gradle 9.7.0, JSON-java 20260814, and Garmin FIT 21.213.0 are optional post-release upgrades rather
than 3.27 blockers.

### Historical v3.26 verification

The dated records below are historical results, not verification or artifacts for 3.27.

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

## 3.27 distribution artifacts

- [x] Clean unsigned universal APK: 1,259,191 bytes; SHA-256
      `3bb11e7ebe466d193b575a65e45150368d590148e8de5c034418c21560378b0c`.
- [x] Clean unsigned release AAB: 2,079,036 bytes; SHA-256
      `ab0b35706f01f37f168256e2687a38d3349e0b52e26d129475d5801f26e7f942`.
- [x] Confirm the APK and AAB contain no native libraries or Markdown files.
- [ ] Inspect bundletool language/density splits and representative download estimates.
- [ ] Sign and verify the universal APK with the release key; record its final size and SHA-256.
- [ ] Sign the AAB with the upload key and verify the Play Console-generated delivery artifacts.

### Historical v3.26 artifact baseline

- [x] Clean unsigned universal APK: 1,206,013 bytes; SHA-256
      `2c45e039560e4ed63476f3f579d22e64854c0f9511541304e476f408838d314e`.
- [x] Clean unsigned release AAB: 1,999,431 bytes; SHA-256
      `331ccb93123faa8e66840cb51868877357ea14d71bdc9d57aabc01929350f5b6`.
- [x] Confirm the APK and AAB contain no native libraries and require no ABI splits.
- [x] Inspect bundletool language/density splits and representative download estimates.

## Android lifecycle and compatibility

- [x] Confirm the merged manifest advertises USB host support as optional, omits USB accessory mode,
      and disables cleartext traffic (covered by the Android smoke suite).
- [ ] Compare the Play Console device catalog before and after the USB feature correction; confirm
      built-in-ANT devices without advertised USB host support remain eligible.
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
- [ ] Expand history rows before and after deleting, inserting, reordering, and refreshing records;
      confirm expansion follows `(profile,date)`, never another position, and recycled rows display
      details only for the current measurement.
- [ ] Create, edit, and delete manual measurements.
- [ ] Create, edit, display, and delete goals.
- [ ] Restore an actual backup created by a pre-refactor version of the app.
- [ ] Confirm backup and restore preserve every supported field.

## ANT measurements

Repeat the failure-sensitive cases at least three times on a representative supported device.

- [ ] Install and launch on both a built-in-ANT device and a USB-host device with an external ANT
      adapter; confirm each hardware path remains available.
- [ ] Complete a measurement and confirm it is saved exactly once.
- [ ] With a scale that broadcasts ANT common pages 0x50–0x52, open **Scale info** and compare the
      displayed manufacturer, model, revisions, serial number, operating time, voltage, and battery
      status with the scale's data. Confirm unavailable fields are omitted.
- [ ] With a scale that does not broadcast common device information, confirm **Scale info** remains
      hidden and ordinary measurement completion is unchanged.
- [ ] Search with the scale off and confirm timeout saves nothing.
- [ ] Cancel during search and during measurement reception.
- [ ] Disconnect the ANT service/radio during search and measurement reception.
- [ ] Enable, disable, and reset ANT before and during a session; confirm bind, measurement data,
      stop, and a subsequent rebind still work without the former no-op status receiver.
- [ ] Submit a partial composition or non-barefoot measurement and confirm it is not saved or
      uploaded.
- [ ] Recreate the Activity during search and confirm progress resumes without duplicate results or
      leaks.
- [ ] Measure for profile A, then select profile B and navigate away/back; confirm profile B's
      measurement is rendered and edit/upload cannot target profile A. Repeat after rotation during
      and after profile A's measurement.
- [ ] Confirm automatic upload runs only after a complete, successful measurement.

## Garmin

- [ ] On API 23–28, sign in, upload a measurement, renew a token, and download history with current
      Google Play services installed.
- [ ] On API 23–28 with repairable Google Play services, confirm the supplied recovery action opens,
      no Garmin HTTPS request starts before recovery completes, and credential test, upload, and
      history retry exactly once afterward. Repeat across Activity recreation while repair is open.
- [ ] Cancel the API 23–28 Google Play services recovery action and confirm no pending Garmin action
      starts; repeat when repair still reports required and confirm there is no retry loop.
- [ ] On API 23–28 without Google Play services, confirm Garmin workflows stop safely without a
      crash or HTTPS request.
- [ ] Confirm token status and expiration timestamps are accurate.
- [ ] Log in with MFA and verify manual code entry, scoped notification autofill, and cancellation.
- [ ] Open sequential MFA dialogs and reconnect the notification listener with an old active Garmin
      notification; confirm old, replayed, unrelated, pre-dialog, and post-cancellation codes do not
      autofill, while one fresh Garmin code received during the visible dialog does.
- [ ] Upload with active and expired access tokens.
- [ ] Against a controlled Garmin-compatible endpoint, verify redirect cookies are replayed,
      clearing one session removes only its cookies, and simultaneous sessions remain isolated.
- [ ] Rotate, background, and destroy/recreate the Activity during Garmin sign-in, upload, and email
      preparation; confirm one retained operation, a replaceable progress dialog, one result, no
      leaked window, and functional Cancel behavior.
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
- [ ] Verify Garmin Connect and email indicators are larger and vertically centered in user rows,
      including rows with one indicator, both indicators, long names, and right-to-left text.
- [ ] On API 23 and a current API, verify toolbar/menu layout, weight cards, drawer width/insets, and
      the circular add button in both day and night themes.
- [ ] With TalkBack or another accessibility service, confirm drawer rows expose their checked state,
      all navigation and add targets are easy to activate, and the add action is announced correctly.
- [ ] Verify graphs for every metric, unit system, goal type, and segmental measurement.
- [ ] Confirm fat percentage/mass preference is respected.
- [ ] Confirm segmental values map to the correct body parts.
- [ ] Verify fat-only, muscle-only, combined, and empty segment cards with both fat percentage and
      fat mass preferences; confirm muscle-only sections remain visible and trends compare like
      quantities.
- [ ] Confirm goals appear on their matching graphs.
- [ ] Open and complete the email-sharing workflow.
- [ ] Export CSV through a representative document provider and confirm its rows, values, units,
      filename, cancellation, and error handling; also confirm email output remains correct.

## Locales

- [ ] Check English default resources and navigation.
- [ ] Check Arabic right-to-left layout and translated resources.
- [ ] Check Portuguese (Portugal) translated resources.
- [ ] Check Japanese non-Latin translated resources.

## Deferred technical debt

Garmin credentials remain in the backward-compatible `users` JSON file. Moving them to Android
Keystore requires an explicit migration plus backup and recovery design so existing connections are
not silently invalidated.
