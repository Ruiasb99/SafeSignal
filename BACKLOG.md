# Emergency Button Backlog

## Accounts and app-to-app support

- **On hold by user decision (2026-09-07):** retain working accounts/invitations, but postpone app-to-app alerts, paid backend and audio until a concrete benefit justifies them. Prioritise BLE + SMS.

- Prepared: optional Firebase email/password accounts, profile name, verification, password reset and sign-out.
- Firebase Authentication connected; user confirmed registration/email verification. Complete any remaining password reset/session tests.
- Add account deletion with recent-login confirmation before release.
- Implemented app-contact invitations with private codes, preview, explicit acceptance/decline and revocation. User confirmed invitations work between two accounts.
- Implemented private user profiles and participant-scoped invitation lists; no email/phone directory.
- Before app alerts, enforce one canonical relationship per sender/recipient at the database level; current normal UI prevents duplicate acceptance, but simultaneous clients may still create duplicate connections.
- Add invitation abuse controls, App Check and a retention/deletion policy before release.
- Add app-to-app emergency notifications, acknowledgements and a clearly defined SMS fallback.
- Decide how phone-local contacts and PIN settings should behave when different users share a phone.
- Revisit audio after accounts and trusted-contact delivery are working.

## Hardware button pattern

- Replaced press/press/hold with **3 or more quick presses**: first-to-third within 2 seconds; trigger immediately on the third. Extra presses during an active incident do not resend. Implemented; hardware stress/pocket tests pending.
- Firmware debounces both edges for 35 ms and sends numbered press events. Tune the timings from physical tests.
- Add physical confirmation when the pattern is accepted, such as vibration or an LED on the final PCB.
- Test the pattern under stress and while the device is carried in a pocket or bag.

## Bluetooth Low Energy

- Confirmed from user's Arduino sketch: service `12345678-1234-1234-1234-123456789abc`, characteristic `87654321-4321-4321-4321-cba987654321` (read/notify).
- Pair the button as an Android companion device.
- Implemented known-address reconnect with bounded backoff, service discovery and confirmed CCCD notification subscription. Verify with real hardware.
- Implemented an explicitly enabled connected-device/location foreground service, independent of the UI. Verify screen-off/Xiaomi behaviour. Reboot/force-stop recovery is manual for this prototype.
- Add authenticated bonding/provisioning before real-world use; selecting an address/UUID is not cryptographic device authentication.
- Apply different BLE behavior for Armed and Low Power modes.
- Show clear connected, reconnecting, and disconnected states.
- Test locked-screen operation and Xiaomi battery-management behavior.

## Active emergency location updates

- Start an active incident after an SOS is triggered.
- Send location updates to every emergency contact approximately every five minutes.
- Include the location timestamp and accuracy so contacts can judge stale data.
- Continue safely through app backgrounding and temporary location failures.
- Stop emergency updates only after the correct cancellation PIN is entered.
- Send a final cancellation message to every contact.
- Persist enough incident state to recover correctly if Android kills or restarts the app.
- Add SMS submission and delivery feedback where the phone and carrier support it.

## Later product work

- Discuss **silent versus attention-drawing SOS scenarios after testing triple press**. No sound, flash, vibration or automatic call added now.
- Consider an optional accidental-activation warning (sound/flash) to let the wearer notice and PIN-cancel; weigh risk of exposing a silent emergency.
- Consider a distinct deliberate gesture/mode for being followed in the street: optional loud alarm or initiating a video call while sending location.
- Contrast that with kidnapping/coercion scenarios where silence may be essential. Do not assume a call/alarm will deter an aggressor; evaluate safety, feasibility, permissions and consent before implementation.

- Add secure live-location links so contacts do not receive a long sequence of SMS messages.
- Add an optional duress PIN that appears to cancel while keeping the emergency active.
- Define escalation instructions and a factual emergency-call script for contacts.
- Review privacy, data retention, Play Store permissions, and emergency-service requirements before release.
