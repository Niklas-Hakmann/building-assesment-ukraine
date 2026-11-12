"""Mock accelerometer publisher — one sensor per building.

Publishes one JSON reading per sensor every ``intervalSeconds`` to
``sensors/{sensorType}/{sensorId}``. Field names mirror ISensorGeneral and
IVibrationSensor so the Java decoder can map them one-to-one.

Units: dominantFrequency in Hz, maxVibrations as peak acceleration in mg
(thousandths of g) within the interval, totalVibrations as the number of
vibration events above threshold within the interval.

Behaviour per sensor (all tunable in sensors.json):

* Normal: the building's own natural frequency with a little noise, low
  background vibration.
* Blast: rare, independent per building. That message carries a high peak on
  all three axes and many events.
* Damage: after a blast the natural frequency may drop permanently (loss of
  stiffness). It is reset to baseline ``repairAfterSeconds`` after the last
  blast, so tests can be repeated without restarting the container.
* Offline: after a blast the sensor may stop sending for a while and then come
  back by itself. messagesNumber does not advance while it is offline.

The simulator only produces data; deciding what is an alarm is the backend's job.
"""
import json
import os
import random
import signal
import time
from datetime import datetime, timezone

import paho.mqtt.client as mqtt

HOST = os.getenv("MQTT_HOST", "localhost")
PORT = int(os.getenv("MQTT_PORT", "1883"))
CONFIG = os.getenv("SENSOR_CONFIG", "sensors.json")
CLIENT_ID = os.getenv("MQTT_CLIENT_ID", "sensor-simulator")

AXES = ("x", "y", "z")


def load_config(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def log(msg):
    print(f"{datetime.now(timezone.utc):%H:%M:%S} {msg}", flush=True)


class Sensor:
    def __init__(self, spec):
        self.id = spec["id"]
        self.base_frequency = spec["frequency"]
        self.frequency_drop = 0.0      # accumulated fractional drop, 0.0 = undamaged
        self.last_blast = None         # monotonic time of last blast
        self.offline_until = 0.0       # monotonic time the sensor comes back
        self.messages = 0

    def is_offline(self, now):
        return now < self.offline_until

    def maybe_repair(self, now, blast_cfg):
        if self.frequency_drop and now - self.last_blast >= blast_cfg["repairAfterSeconds"]:
            self.frequency_drop = 0.0
            log(f"sensor {self.id}: repaired, frequency back to baseline")

    def reading(self, now, cfg):
        normal, blast_cfg = cfg["normal"], cfg["blast"]
        blast = random.random() < blast_cfg["chancePerMessage"]

        if blast:
            # A blast hits all three axes at once; axes see 50-100 % of the peak.
            peak = random.uniform(*blast_cfg["maxVibrationsMg"])
            max_vib = {a: int(peak * random.uniform(0.5, 1.0)) for a in AXES}
            total_vib = {a: random.randint(*blast_cfg["totalVibrations"]) for a in AXES}
        else:
            max_vib = {a: random.randint(*normal["maxVibrationsMg"]) for a in AXES}
            total_vib = {a: random.randint(*normal["totalVibrations"]) for a in AXES}

        noise = normal["frequencyNoise"]
        frequency = {
            a: round(f * (1 - self.frequency_drop) * random.uniform(1 - noise, 1 + noise), 2)
            for a, f in self.base_frequency.items()
        }

        if blast:
            self.after_blast(now, blast_cfg, max(max_vib.values()))
        return frequency, total_vib, max_vib

    def after_blast(self, now, blast_cfg, peak):
        self.last_blast = now
        msg = f"sensor {self.id}: BLAST, peak {peak} mg"
        if random.random() < blast_cfg["damageChance"]:
            drop = random.uniform(*blast_cfg["frequencyDrop"])
            self.frequency_drop = min(self.frequency_drop + drop, blast_cfg["maxTotalFrequencyDrop"])
            msg += f", frequency now -{self.frequency_drop:.0%} vs baseline"
        if random.random() < blast_cfg["offlineChance"]:
            seconds = random.uniform(*blast_cfg["offlineSeconds"])
            self.offline_until = now + seconds
            msg += f", offline for {seconds:.0f}s"
        log(msg)


def build_payload(sensor, cfg, started, now):
    frequency, total_vib, max_vib = sensor.reading(now, cfg)
    sensor.messages += 1
    return {
        # ISensorGeneral
        "sensorId": sensor.id,
        "sensorTime": datetime.now(timezone.utc).isoformat(timespec="milliseconds"),
        "durationTimer": int(now - started),            # seconds since start
        "messagesNumber": sensor.messages,
        "batteryLevel": cfg["batteryLevel"],
        # IVibrationSensor
        "dominantFrequency": frequency,                 # Hz
        "totalVibrations": total_vib,                   # events in interval
        "maxVibrations": max_vib,                       # peak, mg
    }


def main():
    cfg = load_config(CONFIG)
    sensor_type = cfg.get("sensorType", "vibration")
    interval = float(cfg.get("intervalSeconds", 10.0))
    sensors = [Sensor(spec) for spec in cfg["sensors"]]

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=CLIENT_ID)
    client.connect(HOST, PORT, keepalive=30)
    client.loop_start()
    log(f"connected to {HOST}:{PORT}, simulating {len(sensors)} '{sensor_type}' sensors "
        f"every {interval:g}s")

    running = True

    def stop(*_):
        nonlocal running
        running = False

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)

    started = time.monotonic()
    was_offline = {s.id: False for s in sensors}
    while running:
        now = time.monotonic()
        for s in sensors:
            if s.is_offline(now):
                was_offline[s.id] = True
                continue
            if was_offline[s.id]:
                was_offline[s.id] = False
                log(f"sensor {s.id}: back online")
            if s.last_blast is not None:
                s.maybe_repair(now, cfg["blast"])
            payload = build_payload(s, cfg, started, now)
            client.publish(f"sensors/{sensor_type}/{s.id}", json.dumps(payload), qos=0)

        # Sleep in short steps so SIGTERM stops the container promptly.
        deadline = now + interval
        while running and time.monotonic() < deadline:
            time.sleep(0.2)

    client.loop_stop()
    client.disconnect()


if __name__ == "__main__":
    main()
