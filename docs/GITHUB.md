# GitHub presentation

Repository: [Ruiasb99/SafeSignal](https://github.com/Ruiasb99/SafeSignal).

Suggested About description: **Android safety prototype: ESP32-C6 BLE button, SMS/location alerts, Jetpack Compose and tested Firebase invitations.**

Suggested topics: `android`, `kotlin`, `jetpack-compose`, `esp32`, `bluetooth-low-energy`, `firebase`, `personal-project`.

## Repository contents

Publish source, firmware, Gradle wrapper, dependency lockfile, rule tests and documentation. Keep device configuration, credentials, signing keys, APK/build output, local caches and backups outside Git history. `.gitignore` implements those exclusions. `google-services.json` is client configuration rather than an admin secret, but each developer supplies their own project configuration.

CI builds the Android project with no Firebase configuration and runs Firestore emulator checks. A workflow file alone is not a passing CI result; inspect the Actions run after pushing.

Keep the repository's existing commit history. Use focused commits with messages describing actual changes. Do not manufacture a development history or claim unexecuted tests passed. No open-source licence is added automatically; choose reuse permissions deliberately before adding a licence file.

## Helpful next presentation assets

- A short recording of three button presses, app status, SMS receipt and PIN cancellation using consenting test recipients.
- Dashboard and Bluetooth screenshots with personal phone numbers, locations and account details removed.
- One breadboard photo showing the XIAO and D2/GND wiring.

Use real captures rather than rendered mockups labelled as working app screenshots. These assets can be added after the code review without blocking publication of the source.

## Suggested CV wording

**SafeSignal — Android / embedded personal project**

Developed a Kotlin/Jetpack Compose Android prototype integrating an ESP32-C6 BLE emergency button with SMS and location alerts. Implemented foreground monitoring, notification handling, PIN cancellation, dependency-based testing and Firebase contact-invitation authorization.

Link the repository and be prepared to demonstrate the system, explain its limitations and describe your own contribution. Do not describe it as a deployed safety service or claim production reliability.
