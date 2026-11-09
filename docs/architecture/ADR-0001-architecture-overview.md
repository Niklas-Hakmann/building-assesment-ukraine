# ADR-0001: Sensor visualization architecture

## Status

Accepted (initial scaffold). Several follow-up decisions are still open and listed below.

> **Note:** the backend module layout below has been superseded by
> [ADR-0002](ADR-0002-plugin-architecture.md), which replaces the shared `core`
> module with `common`, makes each sensor type its own drop-in module, and moves
> the composition root's wiring to runtime service loading. The transport, topic
> convention, frontend and simulator decisions here are unchanged.

## Context

The scope has been narrowed to a first concrete goal: a web dashboard that visualizes
building sensor values in real time. One real physical sensor (vibration/strain) will
publish over MQTT; additional sensors are simulated (mocked), also over MQTT, using
the same message format, so the backend cannot tell a real device from a mock one.
The architecture must support adding more sensor types later without restructuring.

This supersedes the previous, broader scope (inspections, documents, DIIA.Signature,
municipalities), which has been removed. That domain may return later as additional
modules once the sensor pipeline is proven, but nothing from it is scaffolded now.

## Decision

- **Transport**: real MQTT protocol end to end, via a broker, for both the real
  sensor and all mock sensors. Topic convention: `sensors/{sensorType}/{sensorId}`.
- **Backend** (`backend/`): Java, modular monolith with JPMS module boundaries
  (Maven multi-module build):
  - `core` — shared kernel: `SensorReading`, `SensorId`, `SensorType`, common
    error/result types. No dependency on any other module.
  - `mqtt-ingestion` — subscribes to the broker.
  - `sensor-decoding` — one decoder per sensor type, translating raw MQTT payloads
    into `SensorReading`. Only vibration/strain is in scope now.
  - `sensor-registry` — tracks known sensors (id, type, metadata).
  - `reading-store` — latest reading per sensor (and, later, short history).
  - `realtime-gateway` — pushes updates to the frontend (depends on `reading-store`).
  - `app` — composition root / entry point.
- **Frontend** (`frontend/`): React + TypeScript web dashboard (Vite). A web app was
  chosen over the previously planned mobile app because the immediate goal is a fast
  visual proof, not field use.
  - `types` — shared types mirroring the backend's `core` module.
  - `realtime` — WebSocket client to the backend's realtime gateway.
  - `store` — current reading per sensor.
  - `renderers` — maps sensor type to its visual representation (visuals themselves
    deliberately not designed yet).
  - `components` — shared, reusable UI components.
  - `features/sensors` — composes the above into the dashboard page.
- **Simulators** (`simulators/`): a generic, config-driven mock sensor publisher,
  publishing on the same topic/payload convention as the real sensor, so adding a new
  mock sensor is a config change rather than new code.

## Open items (not yet decided)

- The real sensor's existing MQTT topic/payload format (or do we define it, because
  we control the firmware?).
- MQTT broker hosting: local (Mosquitto via docker-compose) vs. cloud (HiveMQ Cloud,
  AWS IoT Core).
- Java MQTT client library (Eclipse Paho vs. HiveMQ MQTT Client).
- Realtime transport from backend to frontend: WebSocket (assumed) vs. Server-Sent
  Events.
- Persistence: in-memory only for now, or a time-series store from the start.
- Visual design of the dashboard (cards vs. graphs vs. both) — deferred on purpose.
- Language/runtime for the `simulators` component.
