# BLE + SMS prototype

## Firmware

Development backups and the original Arduino sketch are retained locally outside this repository.
The updated version to upload is `firmware/EmergencyButton/EmergencyButton.ino`.
Board: Seeed XIAO ESP32-C6; switch wired D2–GND with INPUT_PULLUP.
Build target: `esp32:esp32:XIAO_ESP32C6`; installed ESP32 core 3.3.10 and NimBLE-Arduino 2.5.0.

## Protocol

- Name: Emergency Button (original sketch used EmergencyButton). Scan uses service UUID, not name.
- Service UUID: `12345678-1234-1234-1234-123456789abc`.
- Characteristic UUID: `87654321-4321-4321-4321-cba987654321` (READ | NOTIFY).
- CCCD: `00002902-0000-1000-8000-00805f9b34fb`, provided by NimBLE.
- Updated firmware: `PRESS:0` baseline, then `PRESS:1`, `PRESS:2`, etc. One event per debounced falling edge; holding does not repeat. No stored presses replayed on reconnect.
- Firmware restarts advertising after disconnect. Debounce is 35 ms on both edges. A held button at boot must first be released.
- Android reads only for baseline, then enables notifications and waits for descriptor-write success before showing Listening. Reads never trigger SOS. Repeated/out-of-order counters are ignored; a counter jump is one observed event, not invented missing presses.
- Legacy Idle/SOS notifications are recognised only as release/press edges. The original sketch never restores Idle, so **upload the updated sketch before triple-press testing**.
- Android counts 3 observed presses within a sliding 2-second window using monotonic time; events under 80 ms apart are treated as bounce. Reset on disconnect and successful cancellation. Extra complete triples do not resend an active incident.

## Safe phone test

1. Tell your saved SMS contacts this is a test. SMS is real and carrier charges may apply. Accounts are not required.
2. Save at least one SMS contact and create a cancellation PIN in Settings.
3. Upload the updated sketch to the XIAO. Disconnect nRF Connect so it does not hold the BLE connection.
4. In SafeSignal, open **Emergency button · Bluetooth** near the bottom of the dashboard.
5. Scan, select the correct address, then Start monitoring and confirm. Grant Nearby devices, SMS, location and preferably notification permissions. Turn on Bluetooth and phone Location.
6. Wait for **Listening · 3 quick presses send SOS**. Connected alone is not sufficient; notification setup must finish.
7. One press: counter increases, no SMS. Wait over 2 seconds. Two quick presses: no SMS. Wait over 2 seconds.
8. Three quick presses: initial emergency SMS, then a location SMS if a fix is available. Press a fourth/fifth time: no additional incident. A held press counts once.
9. Dashboard shows the active incident. Wrong PIN must not cancel. Correct PIN sends cancellation to the original recipient snapshot and suppresses a pending location result.
10. Repeat with screen locked and after leaving the app. Confirm actual SMS receipt, not just the app's submission status. Review Xiaomi battery restrictions manually if needed.
11. Switch the board off/on and toggle phone Bluetooth. Status must show disconnect/retry, then Listening after reconnection. Old read values must not send anything. Two presses before disconnect plus one after must not trigger.
12. Stop monitoring: button cannot send, but this does not cancel an active SOS. Reboot/force-stop requires reopening the app and starting monitoring again.

## Scope and limitations

- The foreground service is started while the Activity is visible with connectedDevice + location types. It retains eligibility for an SOS location request with the screen off; it does **not** continuously poll GPS. Normal cached-location work remains 15 min / 2 h / Off, separate from BLE monitoring.
- Connection attempts time out and retry at 2/4/8/16/32/60 seconds, capped at 60 seconds. Scanning stops after 10 seconds or selection; scans never automatically select an unknown device. Address changes require selection again.
- UI and service share a process-scoped emergency coordinator; Activity destruction does not cancel an in-flight location request. Killing the process can still interrupt an SMS/location operation. Persisted active state prevents automatic duplicate SOS after reopening, but interrupted work is not automatically replayed.
- SMS submission is not delivery confirmation. No five-minute emergency location repeats, authenticated BLE bonding, boot recovery, companion association, siren, flash, calls or audio yet.
- A BLE name, address or UUID is not proof of identity. Prototype only; authenticated provisioning/bonding and extensive reliability testing are required before depending on this for personal safety.
- Android requires a visible foreground-service notification (if notification permission is denied, status is only available through system task management). No custom alarm sound, flash or vibration is added. This is not a guarantee of invisibility.

## Platform references

- [Android background BLE](https://developer.android.com/develop/connectivity/bluetooth/ble/background)
- [Foreground service requirements](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [NimBLE server API](https://h2zero.github.io/NimBLE-Arduino/class_nim_b_l_e_server.html)
