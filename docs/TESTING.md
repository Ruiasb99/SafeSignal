# Verification and phone test checklist

## Review verification — 2026-09-08

- Configured Android app: debug APK and instrumentation APK compile; 61 JVM tests pass; lint reports 0 errors and 32 warnings (mainly dependency/style notices).
- Clean staged-source copy without `google-services.json` or `local.properties`: debug build, 61 JVM tests and lint pass using an SDK environment variable. The configured app update was installed and launched successfully on the test phone.
- Firestore emulator: all 14 authorization tests pass.
- Account instrumentation tests were compiled only; no claim of device execution.
- The Android Studio template arithmetic/context tests were removed; test counts refer to behaviour-focused tests.

## Automated checks

- Android debug build, JVM tests and lint: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` (use `gradlew.bat` on Windows).
- Firestore rules: `pnpm install --frozen-lockfile` then `pnpm test` in `firebase-tests`. Uses only the local `demo-safesignal` emulator.
- Firmware: `arduino-cli compile --fqbn esp32:esp32:XIAO_ESP32C6 firmware/EmergencyButton` with ESP32 core 3.3.10 and NimBLE-Arduino 2.5.0.
- Account Compose instrumentation tests: `./gradlew :app:connectedDebugAndroidTest` with an emulator/device. They have compiled but have not been executed on the test phone because it rejected installation of the separate test APK. They are not part of the headless CI run.

The unit tests use fake SMS/location/account dependencies. They do not send real messages or verify carrier delivery. Gradle reports live under `app/build/reports`; Firestore test output reports individual authorization cases.

## User-confirmed hardware checks

- Xiaomi Redmi Note 15, Android 15: SMS/location and PIN cancellation flow tested during development.
- Firebase account creation/verification and invitations between two accounts confirmed by the project owner.
- 2026-09-08: owner confirmed the uploaded ESP32-C6 firmware and Android BLE button flow work.

This confirmation does not establish reboot recovery, long-duration screen-off reliability or reliability across phone models. Use [BLE_TESTING.md](BLE_TESTING.md) for the full checklist.

## Regression checks without messages

1. Install as an update; do not uninstall first. Confirm existing contacts, mode, PIN and selected button survive.
2. Open contacts; validate editing and duplicate rejection without removing real recipients.
3. Rotate during editing and confirm the draft remains.
4. Open Settings; check PIN confirmation and current-PIN requirements. Leaving the screen clears PIN drafts.
5. Open Bluetooth setup; confirm connection status and controls. Stopping monitoring must not claim that the emergency was cancelled.
6. With no Firebase configuration, account forms show an unavailable state. SMS/BLE screens remain accessible.

## Coordinated SMS checks

Tell the recipients first; real SMS/carrier charges may apply.

1. Press on-screen SOS or use the three-press physical gesture. Confirm actual receipt of the initial SMS and location link.
2. Rotate/leave the screen during the location request; it should finish without an extra initial SOS.
3. Check that extra physical presses do not resend during the active incident.
4. Try a wrong cancellation PIN, then the correct PIN. The original incident recipients should receive cancellation.
5. Cancel while location is pending. No later location callback should submit a message for that cancelled request. Already-submitted SMS cannot be recalled.
6. Test with mobile data/Wi-Fi off and cellular SMS coverage available. Check outdoor GPS and the labelled cached-location fallback.

Record the action, permission settings, displayed state and messages actually received. Do not infer delivery from the app's submission status.
