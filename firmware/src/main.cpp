#include <Wire.h>

#ifndef MOCK_SENSOR
#include <Adafruit_MMA8451.h>
#include <Adafruit_Sensor.h>
Adafruit_MMA8451 mma;
#endif

const unsigned long SAMPLE_INTERVAL_MS = 10; // ~100 Hz

void setup() {
  Serial.begin(115200);
  while (!Serial) {
    delay(10);
  }

#ifndef MOCK_SENSOR
  Wire.begin();
  if (!mma.begin()) {
    Serial.println("{\"error\":\"MMA8451 not found - check wiring\"}");
    while (1) {
      delay(10);
    }
  }
  mma.setRange(MMA8451_RANGE_2_G);
#endif
}

#ifdef MOCK_SENSOR
// No sensor wired up yet. Stands in for a wall-mounted sensor idling around
// gravity on z, with a ~3.5 Hz wobble plus noise on all three axes - just
// enough signal for the bridge's FFT to have a dominant frequency to find.
void readAcceleration(float &x, float &y, float &z) {
  float t = millis() / 1000.0;
  float noise = (random(-100, 100) / 100.0) * 0.05;
  x = 0.05 * sin(2 * PI * 3.5 * t) + noise;
  y = 0.04 * sin(2 * PI * 3.5 * t + 1.0) + noise;
  z = 9.81 + 0.3 * sin(2 * PI * 3.5 * t) + noise;
}
#else
void readAcceleration(float &x, float &y, float &z) {
  mma.read();
  sensors_event_t event;
  mma.getEvent(&event);
  x = event.acceleration.x;
  y = event.acceleration.y;
  z = event.acceleration.z;
}
#endif

void loop() {
  static unsigned long lastSample = 0;
  unsigned long now = millis();
  if (now - lastSample < SAMPLE_INTERVAL_MS) {
    return;
  }
  lastSample = now;

  float x, y, z;
  readAcceleration(x, y, z);

  Serial.print("{\"x\":");
  Serial.print(x, 4);
  Serial.print(",\"y\":");
  Serial.print(y, 4);
  Serial.print(",\"z\":");
  Serial.print(z, 4);
  Serial.println("}");
}
