# Building Assessment Ukraine - Sensor Dashboard

A web dashboard that visualizes building sensor values in real time: one real
physical sensor plus several simulated sensors, all communicating over MQTT, with
support for multiple sensor types.

## Structure

```
backend/      Java backend, JPMS plugin architecture, Maven multi-module build
frontend/     React + TypeScript web dashboard (Vite)
simulators/   Mock sensor publisher(s) (MQTT), config-driven (not yet implemented)
infra/        MQTT broker and other local/deployment infrastructure (not yet defined)
docs/         Architecture decision records and other documentation
```

See [ADR-0001](docs/architecture/ADR-0001-architecture-overview.md) for the
overall architecture and [ADR-0002](docs/architecture/ADR-0002-plugin-architecture.md)
for the backend's plugin structure and currently open items.

## Backend

Multi-module Maven build under `backend/`. Requires Java 21+.

```bash
cd backend
mvn clean install     # builds, and installs the JARs into the two runtime layers
mvn -N exec:exec      # runs the app (demo messages until an MQTT client is chosen)
```

### Layout

```
common/             shared contracts - data types, service roles, plugin SPI. Depends on nothing.
app/                host: entry point, wiring, message pipeline. Requires common only.
mqtt-ingestion/     owns the topic convention sensors/{sensorType}/{sensorId}
sensor-registry/    which sensors exist
reading-store/      latest reading per sensor
realtime-gateway/   pushes readings to the dashboard
sensor-vibration/   sensor plugin - the real vibration/strain device
sensor-temperature/ sensor plugin - simulated, proving a new type costs one module

mods-mvn/           build output: boot module layer
plugins/            build output: sensor plugin JARs, loaded into a child layer at runtime
```

### Adding a sensor type

1. Copy `sensor-temperature/` and rename it.
2. Implement `SensorDecoder` (one `SensorType`, one `decode`) and `SensorPlugin`.
3. `provides` both in its `module-info.java`.
4. Add the module to `backend/pom.xml`.

No existing module is edited, and `app` is not rebuilt. An already-built plugin
JAR can be added to or removed from `plugins/` between runs with no build at
all.

## Frontend

React + TypeScript (Vite) skeleton under `frontend/`.

```bash
cd frontend
npm install
npm run dev
```
