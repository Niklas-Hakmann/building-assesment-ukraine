# Firmware

Real accelerometer hardware (Seeed XIAO RP2040 + MMA8451), publishing the same
MQTT schema `simulators/simulator.py` fakes — the Java backend can't tell them
apart. The RP2040 has no WiFi, so the pipeline is split in two:

```
MMA8451 --I2C--> XIAO RP2040 --USB serial--> bridge.py --MQTT--> sensors/accelerometer/{id}
```

- `src/main.cpp` — reads the MMA8451 at ~100 Hz and streams raw samples
  (`{"x":..,"y":..,"z":..}` in m/s², one line per sample) over USB serial.
  No MQTT, no vibration math — just getting the raw signal off the board.
- `bridge/bridge.py` — runs on the host the board is plugged into. Buffers a
  window of samples, computes `dominantFrequency` (FFT peak), `totalVibrations`
  (threshold crossings) and `maxVibrations` (peak deviation, in mg) per axis,
  and publishes to `sensors/{sensorType}/{sensorId}` in the same shape as
  `simulators/simulator.py`'s payload — see `simulators/README.md` for the
  field reference.

## Wiring

I2C on the XIAO RP2040: `SDA -> D4`, `SCL -> D5`, `VIN -> 3V3`, `GND -> GND`.

## Flashing the firmware

```bash
cd firmware
pio run -t upload
pio device monitor
```

You should see a stream of `{"x":...,"y":...,"z":...}` lines once it's wired
up correctly.

## Running the bridge

```bash
cd firmware/bridge
pip install -r requirements.txt
SERIAL_PORT=/dev/ttyACM0 SENSOR_ID=1 MQTT_HOST=localhost python bridge.py
```

On Windows, `SERIAL_PORT` is a `COMx` port instead (check Device Manager).

Env vars (all optional, defaults shown):

| Variable                | Default           | Meaning                                  |
|--------------------------|-------------------|-------------------------------------------|
| `SERIAL_PORT`            | `/dev/ttyACM0`    | Port the XIAO RP2040 is on                |
| `BAUD_RATE`              | `115200`          | Must match `main.cpp`'s `Serial.begin`    |
| `SAMPLE_RATE_HZ`         | `100`             | Must match `main.cpp`'s sample interval   |
| `WINDOW_SECONDS`         | `10`              | How often a reading is published          |
| `VIBRATION_THRESHOLD_MG` | `20`              | Deviation above which an event is counted |
| `BATTERY_LEVEL`          | `100`             | MMA8451 has no fuel gauge — static for now|
| `SENSOR_ID`              | `1`               | Must match the `sensorId` the UI expects  |
| `MQTT_HOST` / `MQTT_PORT`| `localhost`/`1883`| The Mosquitto broker from `docker-compose.yml` |

Verify the real sensor's traffic the same way as the simulator's:

```bash
docker compose exec mosquitto mosquitto_sub -t 'sensors/accelerometer/#' -v
```
