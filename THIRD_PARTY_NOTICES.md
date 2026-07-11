# Third-party notices

This file lists the direct third-party components used to build or run the application. Transitive
Android dependencies retain the notices and license metadata distributed with their artifacts.

## ANT Android Library

- Component: ANT Android Library
- Version: 4.16.0
- Repository artifact: `app/libs/android_antlib_4-16-0.aar`
- Source and SDK distribution: https://github.com/ant-wireless/ANT-Android-SDKs
- Copyright: Garmin Canada Inc. and its subsidiaries
- License/terms: distributed with the ANT Android SDK

The AAR is committed because it is not consumed from the project's configured Maven repositories.

## Garmin FIT Java SDK

- Component: Official Garmin FIT Java SDK
- Version: 21.188.0 (FIT Profile 21.188.0 Release)
- Maven coordinate: `com.garmin:fit:21.188.0`
- Source: https://github.com/garmin/fit-java-sdk
- Distribution: https://central.sonatype.com/artifact/com.garmin/fit/21.188.0
- Maven Central JAR SHA-256: `bb11d60e3a2fb77d5d6554beb551c7dc18c76a5e5720ea080f9b34448e603d20`
- Copyright: 2025 Garmin International, Inc.
- License: Flexible and Interoperable Data Transfer (FIT) Protocol License Agreement
- License text: https://github.com/garmin/fit-java-sdk/blob/main/LICENSE.txt

The SDK is consumed from Garmin's official Maven Central publication; its source or binary is not
committed to this repository. The dependency version is pinned to match the previously vendored
FIT Profile version. Garmin's license applies to the SDK and its use.

## AndroidX and Material Components

- Components: AndroidX AppCompat 1.7.1 and WorkManager Runtime 2.11.2
- Sources: https://github.com/androidx/androidx
- License: Apache License 2.0
- Component: Material Components for Android 1.14.0
- Source: https://github.com/material-components/material-components-android
- License: Apache License 2.0

## Charts and color picker

- Component: MPAndroidChart 3.1.0
- Source: https://github.com/PhilJay/MPAndroidChart
- License: Apache License 2.0
- Component: HoloColorPicker 1.5
- Source: https://github.com/LarsWerkman/HoloColorPicker
- License: Apache License 2.0

## Google Play services

- Component: Google Play services Base 18.10.0
- Distribution: https://developers.google.com/android/guides/setup
- Terms: https://developers.google.com/terms

This component supplies the security-provider update and service-availability APIs used on older
Android versions.

## Signpost

- Component: Signpost Core 2.1.1
- Source: https://github.com/mttkay/signpost
- License: Apache License 2.0

## Test-only dependencies

- JUnit 4.13.2 — Eclipse Public License 1.0: https://github.com/junit-team/junit4
- JSON-java 20260522 — JSON License: https://github.com/stleary/JSON-java

Test-only dependencies are not packaged in the release APK.
