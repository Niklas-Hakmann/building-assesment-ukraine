#include <Wire.h>
#include <Adafruit_MMA8451.h>
#include <Adafruit_Sensor.h>

Adafruit_MMA8451 mma;

const unsigned long SAMPLE_INTERVAL_MS = 10; // ~100 Hz

void setup() {
  Serial.begin(115200);
  while (!Serial) {
    delay(10);
  }

  Wire.begin();
  if (!mma.begin()) {
    Serial.println("{\"error\":\"MMA8451 not found - check wiring\"}");
    while (1) {
      delay(10);
    }
  }
  mma.setRange(MMA8451_RANGE_2_G);
}

void loop() {
  static unsigned long lastSample = 0;
  unsigned long now = millis();
  if (now - lastSample < SAMPLE_INTERVAL_MS) {
    return;
  }
  lastSample = now;

  mma.read();
  sensors_event_t event;
  mma.getEvent(&event);

  Serial.print("{\"x\":");
  Serial.print(event.acceleration.x, 4);
  Serial.print(",\"y\":");
  Serial.print(event.acceleration.y, 4);
  Serial.print(",\"z\":");
  Serial.print(event.acceleration.z, 4);
  Serial.println("}");
}
