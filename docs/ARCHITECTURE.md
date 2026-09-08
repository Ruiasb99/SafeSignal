# Architecture and design decisions

## Boundaries

`MainActivity` requests Android permissions and wires screen events. Compose screens accept state and callbacks. `EmergencyViewModel` owns form/navigation state and PIN validation, while `EmergencyCoordinator` owns the incident operation. `AppContainer` creates one coordinator per application process and injects it into the UI and BLE service; a ViewModel never creates or closes its own coordinator.

`platform/ble/BleManager` owns Android GATT/scanning resources and exposes a read-only StateFlow of plain `BleState` values. Android framework objects do not cross into the screens. The service consumes the same connection state and forwards complete press patterns to the incident coordinator. All these operations are serialized on the main thread; GATT callbacks are posted there, and location results use the main executor.

Domain interfaces describe persistence, SMS submission and cancellable location requests. `LocalEmergencyRepository` delegates key/format ownership to `EmergencyStorage`. Starting an incident stores the active flag and recipient snapshot in one SharedPreferences edit. Existing key names remain compatible with installed versions. SharedPreferences applies changes asynchronously: this is not a durable job queue or an exactly-once transport.

The top-level `LocationUpdateWorker` class retains its name because WorkManager persists worker class names. Top-level storage/PIN adapters also preserve the original data format. A future package move or storage migration should explicitly account for installed app data.

## Incident lifecycle

1. An on-screen action passes permission checks, or the enabled BLE service receives three presses.
2. The coordinator records the recipient snapshot and submits the initial SMS.
3. A cancellable location request completes within its timeout; a successful result is saved and submitted to the same recipients.
4. Correct PIN validation calls cancellation, invalidates the request generation and submits cancellation to those original recipients.

The generation check suppresses already-queued location callbacks after cancellation or replacement. Hardware triggers cannot duplicate an active incident. The on-screen SOS retains its existing ability to explicitly resend/replace the active request. Removing a contact during an incident does not silently change that incident's destination snapshot.

## Why one module and manual dependency injection?

This project has one application and a small firmware component. Packages and explicit constructor dependencies currently make the code easy to follow and test. More Gradle modules, a DI framework or a backend would introduce setup costs without resolving today's main limitations. Extract modules when independent ownership, build performance or reuse warrants it.

Callbacks are used for one-shot platform/Firebase operations; BLE uses StateFlow for observable transport state. This is a deliberate incremental design, not a claim that every layer follows one asynchronous abstraction. A future coroutine migration should preserve cancellation and account-generation guards and be verified against the same tests.

## Scalability

SMS/BLE are local to each installation; backend scale is irrelevant to the core alert path. Firebase invitations query participants, not a public user directory, and use server transactions and restrictive rules. They do not yet have pagination/retention, abuse controls or a canonical unique sender/recipient relationship. Those become necessary before app-to-app alerts or broader account usage.

The most valuable next engineering work is recovering interrupted incidents, measuring BLE/background reliability and authenticated device provisioning. Adding layers alone does not solve those problems.
