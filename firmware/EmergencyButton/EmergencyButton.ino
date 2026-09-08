#include <NimBLEDevice.h>

// Seeed XIAO ESP32-C6: physical button between D2 and GND, internal pull-up.
// Original sketch is preserved in the sibling Advertise_BLE folder.
constexpr uint8_t BUTTON_PIN = D2;
constexpr uint32_t DEBOUNCE_MS = 35;
NimBLECharacteristic* emergencyChar;
bool rawState = HIGH;
bool stableState = HIGH;
uint32_t changedAt = 0;
uint32_t sequence = 0;

void setup() {
  Serial.begin(115200);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  // A button held during boot is not a new press; require release first.
  rawState = stableState = digitalRead(BUTTON_PIN);
  changedAt = millis();
  NimBLEDevice::init("Emergency Button");
  NimBLEServer* server = NimBLEDevice::createServer();
  server->advertiseOnDisconnect(true);
  NimBLEService* service = server->createService("12345678-1234-1234-1234-123456789abc");
  emergencyChar = service->createCharacteristic("87654321-4321-4321-4321-cba987654321",
      NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY);
  emergencyChar->setValue("PRESS:0");
  service->start();
  NimBLEAdvertising* advertising = NimBLEDevice::getAdvertising();
  advertising->addServiceUUID(service->getUUID());
  advertising->enableScanResponse(true);
  advertising->setName("Emergency Button");
  advertising->start();
  Serial.println("BLE ready. Each debounced press notifies; the phone counts 3 within 2 seconds.");
}

void loop() {
  const uint32_t now = millis();
  const bool reading = digitalRead(BUTTON_PIN);
  if (reading != rawState) {
    rawState = reading;
    changedAt = now;
  }
  if (rawState != stableState && uint32_t(now - changedAt) >= DEBOUNCE_MS) {
    stableState = rawState;
    if (stableState == LOW) {
      ++sequence;
      char value[20];
      snprintf(value, sizeof(value), "PRESS:%lu", static_cast<unsigned long>(sequence));
      emergencyChar->setValue(value);
      emergencyChar->notify();
      Serial.println(value);
    }
  }
  delay(5);
}
