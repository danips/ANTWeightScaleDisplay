# Third-party notices

This file lists the direct third-party components used to build or run the application. Transitive
Android dependencies retain the notices and license metadata distributed with their artifacts.

## ANT service interface

- Component: `IAnt_6` Android Interface Definition Language interface
- Repository source: `app/src/main/aidl/com/dsi/ant/IAnt_6.aidl`
- Upstream SDK: https://github.com/ant-wireless/ANT-Android-SDKs
- Copyright: 2011 Dynastream Innovations Inc.
- License: Apache License 2.0 (included in the source-file header)

No ANT AAR is committed or packaged. The app compiles the interface definition and communicates
with ANT Radio Service or ANT USB Service installed separately by the user; those service apps are
not distributed in this APK.

## Garmin FIT Java SDK

- Component: Official Garmin FIT Java SDK
- Version: 21.205.0 (FIT Profile 21.205.0 Release)
- Maven coordinate: `com.garmin:fit:21.205.0`
- Source: https://github.com/garmin/fit-java-sdk
- Distribution: https://central.sonatype.com/artifact/com.garmin/fit/21.205.0
- Maven Central JAR SHA-256: `78d07f655070bb30921ad2ea0d89310ac8824e34055e5ec3adb4e809bea005d6`
- Copyright: 2026 Garmin International, Inc.
- License: Flexible and Interoperable Data Transfer (FIT) Protocol License Agreement
- License text: https://github.com/garmin/fit-java-sdk/blob/main/LICENSE.txt

The SDK is a test-only dependency consumed from Garmin's official Maven Central publication; its
source or binary is not committed to this repository or packaged in the release APK. FIT integrity,
decoded weight fields, and deterministic output are covered by the characterization test. Garmin's
license applies to the SDK and its use.

## AndroidX

- Components: AppCompat 1.7.1, CardView 1.0.0, ConstraintLayout 2.2.1, and RecyclerView 1.4.0
- Source: https://github.com/androidx/androidx
- License: Apache License 2.0

## Charts

- Component: MPAndroidChart 3.1.0
- Source: https://github.com/PhilJay/MPAndroidChart
- License: Apache License 2.0

## Google Play services

- Component: Google Play services Basement 18.10.0
- Distribution: https://developers.google.com/android/guides/setup
- Terms: https://developers.google.com/terms

This component supplies the security-provider update and service-availability APIs used on older
Android versions.

## Other test-only dependencies

- AndroidX Test Core 1.7.0, Runner 1.7.0, and Ext JUnit 1.3.0 — Apache License 2.0: https://github.com/android/android-test
- JUnit 4.13.2 — Eclipse Public License 1.0: https://github.com/junit-team/junit4
- JSON-java 20260522 — JSON License: https://github.com/stleary/JSON-java

Test-only dependencies are not packaged in the release APK.
