# Building Assessment Ukraine - Sensor Dashboard

A web dashboard that visualizes building sensor values in real time: one real
physical sensor plus several simulated sensors, all communicating over MQTT, with
support for multiple sensor types.

## Structure

```
backend/      Java backend, modular monolith (JPMS modules), Maven multi-module build
frontend/     React + TypeScript web dashboard (Vite)
simulators/   Mock sensor publisher(s) (MQTT), config-driven (not yet implemented)
infra/        MQTT broker and other local/deployment infrastructure (not yet defined)
docs/         Architecture decision records and other documentation
```

See [docs/architecture/ADR-0001-architecture-overview.md](docs/architecture/ADR-0001-architecture-overview.md)
for the architecture decisions and currently open items.

## Backend

Multi-module Maven build under `backend/`. Requires Java 21+.

```bash
cd backend
mvn compile
```

## Frontend

React + TypeScript (Vite) skeleton under `frontend/`.

```bash
cd frontend
npm install
npm run dev
```
