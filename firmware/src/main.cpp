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
// gravity on z. Ramps a 40 s triangle cycle between calm (~2 Hz, small
// amplitude) and a shake (~6 Hz, large amplitude) so dominantFrequencyX
// crosses the backend's isCritical() threshold (4 Hz) once per cycle - the
// dashboard visibly flips between Normal and Critical to prove the pipeline
// is live, not just replaying one static reading.
void readAcceleration(float &x, float &y, float &z) {
  float t = millis() / 1000.0;

  // Step frequency/amplitude once every STEP_SECONDS (matches the bridge's
  // default WINDOW_SECONDS) so each analysis window sees one stable tone
  // instead of a sweep - a sweep spreads FFT energy across bins and the
  // dominant-frequency estimate comes out wrong.
  const float STEP_SECONDS = 10.0;
  const float CYCLE_SECONDS = 40.0;
  float steppedT = floor(t / STEP_SECONDS) * STEP_SECONDS;
  float phase = fmod(steppedT, CYCLE_SECONDS) / CYCLE_SECONDS; // 0..1
  float intensity = 1.0 - fabs(phase - 0.5) * 2.0;             // 0..1..0 triangle

  float frequency = 2.0 + intensity * 4.0;                     // 2 Hz .. 6 Hz
  float amplitude = 0.05 + intensity * 0.6;                    // calm .. shake

  float noise = (random(-100, 100) / 100.0) * 0.05;
  x = amplitude * sin(2 * PI * frequency * t) + noise;
  y = amplitude * 0.8 * sin(2 * PI * frequency * t + 1.0) + noise;
  z = 9.81 + amplitude * sin(2 * PI * frequency * t) + noise;
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
