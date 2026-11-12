# Simulators

Mock sensor publisher (Python + paho-mqtt) that publishes fake readings to the MQTT
broker on `sensors/{sensorType}/{sensorId}`. Configured by `sensors.json` — adding a
sensor is a config change, not new code.

Simulates 10 wall-mounted accelerometers, one per building (`sensors/vibration/1` …
`/10`), each sending one message every 10 seconds. Field names mirror
`ISensorGeneral` and `IVibrationSensor`:

```json
{
  "sensorId": 3,
  "sensorTime": "2026-11-12T10:15:30.123+00:00",
  "durationTimer": 42,
  "messagesNumber": 5,
  "batteryLevel": 87,
  "dominantFrequency": { "x": 3.52, "y": 3.08, "z": 7.41 },
  "totalVibrations":   { "x": 1,    "y": 0,    "z": 2    },
  "maxVibrations":     { "x": 2,    "y": 1,    "z": 3    }
}
```

| Field               | Unit / meaning                                              |
|---------------------|-------------------------------------------------------------|
| `dominantFrequency` | Hz — the building's natural frequency                       |
| `maxVibrations`     | mg (thousandths of g) — peak acceleration in the interval   |
| `totalVibrations`   | number of vibration events above threshold in the interval  |
| `durationTimer`     | seconds since the simulator started                         |

## Scenarios

Each sensor behaves independently. All numbers are in `sensors.json`.

- **Normal** — the building's own frequency (`sensors[].frequency`) with ±2 % noise,
  1–3 mg peaks, 0–2 events.
- **Blast** — 1 % chance per message. That one message has 200–2000 mg peaks on all
  three axes and 10–60 events.
- **Damage** — after a blast, 60 % chance that the frequency drops permanently by
  5–25 % (cumulative, capped at 40 %). Reset to baseline 15 min after the last blast,
  so tests can be repeated without restarting.
- **Offline** — after a blast, 30 % chance the sensor stops sending for 1–3 min and
  then comes back by itself. `messagesNumber` does not advance while offline.

The simulator only produces data — what counts as an alarm is decided in the backend.
Events are logged, so you can see what to expect:

```bash
docker compose logs -f sensor-simulator
# 12:43:45 sensor 1: BLAST, peak 508 mg, frequency now -9% vs baseline, offline for 120s
# 12:45:45 sensor 1: back online
```

## Running

From the repository root:

```bash
docker compose up --build -d
docker compose exec mosquitto mosquitto_sub -t 'sensors/#' -v
```

After editing `sensors.json`, `docker compose restart sensor-simulator` is enough.
Editing `simulator.py` needs `docker compose up --build -d sensor-simulator`.
