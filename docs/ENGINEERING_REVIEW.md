# Engineering review — 2026-09-08

Scope: an ongoing personal project, assessed for maintainability, testability and reviewer clarity. This is not a production-readiness certification.

## Strengths

- A concrete end-to-end system spanning embedded input, BLE, Android background execution, SMS and location.
- Separation of screens, ViewModels, incident logic, repository contracts and Android/Firebase adapters.
- Tests exercise cancellation races, recipient snapshots, duplicate notifications, permission results and account changes rather than only happy paths.
- Firebase rules have adversarial emulator tests; private invitations do not require an email/phone directory.
- Source and setup instructions are sufficient to build the Android app without the author's Firebase project or hardware.

## Changes made during this review

- Required injection of the application-scoped emergency coordinator; removed redundant SMS/location dependencies and the alternative screen-owned coordinator lifetime.
- Changed the BLE transport to expose StateFlow and plain state; screens now receive state and callbacks rather than a Bluetooth manager. Moved the transport into `platform/ble`.
- Centralised incident-recipient persistence and saved it with the active flag in one edit.
- Preserved the explanatory service-start error when service teardown follows a rejected start.
- Centralised the WorkManager dependency in the version catalog and added the Gradle distribution checksum.
- Replaced template tests and stale setup claims with a reproducible verification record, architecture notes and GitHub CI.
- Excluded personal configuration, IDE data, generated output and local tooling from publication.

## Remaining improvements, separate from feature expansion

- Move PIN derivation off the main thread and add attempt throttling; it currently uses salted PBKDF2 and constant-time hash comparison but is synchronous.
- Consolidate the periodic worker and immediate-location adapter carefully: they currently choose providers differently. Handle cancellation and permission revocation consistently.
- Add transport-level tests with a fake GATT boundary; the current pure protocol tests do not emulate the Android Bluetooth stack.
- Improve storage/backup policy and retained-data handling. Current Android backup XML is still the starter configuration, and local contacts/location are not application-encrypted.
- Introduce consistent formatting/localised string resources as UI maintenance grows; avoid a large unrelated rewrite of working UI code.
- Validate process-death recovery, bonding and vendor background restrictions before claiming emergency reliability.

## Portfolio assessment

This is a credible personal-project discussion piece because the engineering tradeoffs and observed limitations are concrete. Be ready to explain notification subscription versus reads, foreground-service lifetime, callback cancellation, offline location, SMS submission versus delivery, and Firestore authorization. Present it as a working prototype and discuss what you personally implemented, tested and learned, including assistance from development tools where relevant.
