"""Serial -> MQTT bridge for a real MMA8451 accelerometer on a Seeed XIAO RP2040.

The microcontroller (see ../src/main.cpp) only streams raw acceleration samples
as one JSON object per line, {"x":..,"y":..,"z":..} in m/s^2, over USB serial.
This script turns a window of those samples into the same vibration metrics
simulators/simulator.py fakes, and publishes them to sensors/{sensorType}/{sensorId}
so the Java backend (IAccelerometer) can't tell a real sensor from a mock one.
"""
import json
import os
import time
from datetime import datetime, timezone

import numpy as np
import paho.mqtt.client as mqtt
import serial

SERIAL_PORT = os.getenv("SERIAL_PORT", "/dev/ttyACM0")
BAUD_RATE = int(os.getenv("BAUD_RATE", "115200"))
SAMPLE_RATE_HZ = float(os.getenv("SAMPLE_RATE_HZ", "100"))
WINDOW_SECONDS = float(os.getenv("WINDOW_SECONDS", "10"))
VIBRATION_THRESHOLD_MG = float(os.getenv("VIBRATION_THRESHOLD_MG", "20"))
BATTERY_LEVEL = int(os.getenv("BATTERY_LEVEL", "100"))

MQTT_HOST = os.getenv("MQTT_HOST", "localhost")
MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))
SENSOR_ID = int(os.getenv("SENSOR_ID", "1"))
SENSOR_TYPE = os.getenv("SENSOR_TYPE", "accelerometer")
CLIENT_ID = os.getenv("MQTT_CLIENT_ID", f"sensor-bridge-{SENSOR_ID}")

AXES = ("x", "y", "z")
G_TO_MG = 1000.0 / 9.80665


def log(msg):
    print(f"{datetime.now(timezone.utc):%H:%M:%S} {msg}", flush=True)


def read_window(ser, window_seconds):
    samples = {a: [] for a in AXES}
    deadline = time.monotonic() + window_seconds
    while time.monotonic() < deadline:
        line = ser.readline().decode("utf-8", errors="ignore").strip()
        if not line:
            continue
        try:
            reading = json.loads(line)
            for a in AXES:
                samples[a].append(float(reading[a]))
        except (json.JSONDecodeError, KeyError, ValueError):
            continue
    return samples


def analyze_axis(values, sample_rate_hz):
    if len(values) < 4:
        return 0.0, 0, 0

    deviation_mg = (np.array(values) - np.mean(values)) * G_TO_MG

    spectrum = np.abs(np.fft.rfft(deviation_mg))
    freqs = np.fft.rfftfreq(len(deviation_mg), d=1.0 / sample_rate_hz)
    dominant_frequency = float(freqs[1:][np.argmax(spectrum[1:])]) if len(freqs) > 1 else 0.0

    above = np.abs(deviation_mg) > VIBRATION_THRESHOLD_MG
    total_vibrations = int(np.count_nonzero(above & ~np.roll(above, 1)))
    max_vibrations = int(np.max(np.abs(deviation_mg)))

    return round(dominant_frequency, 2), total_vibrations, max_vibrations


def build_payload(samples, started, messages_number):
    frequency, total_vib, max_vib = {}, {}, {}
    for a in AXES:
        frequency[a], total_vib[a], max_vib[a] = analyze_axis(samples[a], SAMPLE_RATE_HZ)

    return {
        "sensorId": SENSOR_ID,
        "sensorTime": datetime.now(timezone.utc).isoformat(timespec="milliseconds"),
        "durationTimer": int(time.monotonic() - started),
        "messagesNumber": messages_number,
        "batteryLevel": BATTERY_LEVEL,
        "dominantFrequency": frequency,
        "totalVibrations": total_vib,
        "maxVibrations": max_vib,
    }


def main():
    ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
    time.sleep(2)  # the board resets when the serial port opens

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=CLIENT_ID)
    client.connect(MQTT_HOST, MQTT_PORT, keepalive=30)
    client.loop_start()
    log(f"connected to {MQTT_HOST}:{MQTT_PORT}, reading sensor {SENSOR_ID} from {SERIAL_PORT}")

    started = time.monotonic()
    messages_number = 0
    try:
        while True:
            samples = read_window(ser, WINDOW_SECONDS)
            if not samples["x"]:
                log("no samples received from the microcontroller - check wiring/port")
                continue
            messages_number += 1
            payload = build_payload(samples, started, messages_number)
            client.publish(f"sensors/{SENSOR_TYPE}/{SENSOR_ID}", json.dumps(payload), qos=0)
            log(f"sensor {SENSOR_ID}: published window of {len(samples['x'])} samples")
    except KeyboardInterrupt:
        pass
    finally:
        client.loop_stop()
        client.disconnect()
        ser.close()


if __name__ == "__main__":
    main()
