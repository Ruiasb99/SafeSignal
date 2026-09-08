# UX review — September 2026

This pass changes presentation and contact selection, not emergency delivery, BLE triggering, or PIN cancellation.

| Issue | Resolution |
| --- | --- |
| Headings/status bar and bottom buttons overlap Android system bars | One shared page frame consumes safe drawing and keyboard insets. Secondary pages have persistent bottom navigation outside their scroll area. |
| Controls look equally faint | Blue primary actions and navigation; mint contacts, amber Bluetooth and lavender settings accents on a soft warm gradient. Red remains reserved for SOS and destructive actions. Shared palette tokens keep the current light appearance consistent. |
| Invitation creation appears to do nothing | Successful creation opens a code dialog immediately, independently of list updates, with explicit copy confirmation. Leaving the page clears private dialog state and invalidates late results. |
| Too much visual competition | Shorter invitation guidance, less ambiguous location-cache wording, and no idle “Ready” card on the dashboard. Safety limitations remain visible. |
| Number entry is laborious | Android's phone-number picker fills an editable draft. Only the chosen URI is read; no READ_CONTACTS permission or address-book upload. Manual entry remains available. Review the country code and explicitly save. |
| Accidental contact removal | SMS contact removal now requires confirmation. Existing incident recipient snapshots are unchanged. |
| Clipped SOS caption and crowded dashboard navigation | Explicit SOS content padding preserves the complete caption, both Bluetooth lines are centred, and a separate gap distinguishes Bluetooth from Settings. |

## Checks

- JVM tests cover immediate invitation result state, failure/dismissal, session isolation, and picker drafts requiring explicit save.
- Device checks should cover three-button navigation, gesture navigation, keyboard open, large text, long contact lists and offline invitation failure.
- No real SOS, SMS or contact modifications should be triggered by automated visual checks.
- Reference screenshots contain personal data and are intentionally not included in this repository.

Android contact picker reference: https://developer.android.com/guide/components/intents-common#Contacts
