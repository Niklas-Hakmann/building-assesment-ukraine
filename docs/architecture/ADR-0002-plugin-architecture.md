# ADR-0002: Backend as a JPMS plugin architecture

## Status

Accepted (2026-11-09). Supersedes the backend module layout in
[ADR-0001](ADR-0001-architecture-overview.md); the transport, topic convention,
frontend and simulator decisions there are unchanged.

Scope note: this ADR settles the **module structure and the runtime layering**.
The contracts in `common` and the modules' functionality are deliberately left
undesigned and unimplemented.

## Context

ADR-0001 scaffolded the backend as seven modules under `backend/`: `core`,
`mqtt-ingestion`, `sensor-decoding`, `sensor-registry`, `reading-store`,
`realtime-gateway` and `app`. Two problems emerged before any of it was
implemented.

**Adding a sensor type touched shared code.** A single `sensor-decoding` module
holding one decoder per type means every new sensor edits a module that every
other sensor depends on. The cost of a new sensor type was spread across the
system instead of contained.

**The split did not pay for itself.** `sensor-registry`, `reading-store` and
`realtime-gateway` were each a module for one small, host-owned concern, and the
sensors were not modules at all — the decomposition was finest exactly where the
system is stable and coarsest exactly where it changes.

## Decision

Reorganise around the axis the system actually varies along: the sensor type.

**Layout** — Maven multi-module build, aggregator at the repository root, modules
as top-level directories. This mirrors the ST4 semester project, which uses the
same two-layer JPMS plugin arrangement.

```
pom.xml               aggregator
common/               contracts — data, service roles, plugin SPI, ServiceLocator
core/                 host — Main and wiring
  ui/                 dashboard (Vite + React + TS)
sensor-vibration/     sensor plugin → plugins/
sensor-temperature/   sensor plugin → plugins/
mods-mvn/             build output: boot module layer
plugins/              build output: sensor plugin JARs, loaded at runtime
```

**Two runtime layers.** `mods-mvn/` is the boot layer, resolved by the JVM at
startup from `--module-path`. `plugins/` is a child layer that `ServiceLocator`
builds at startup with `defineModulesWithManyLoaders`. A module's `pom.xml`
chooses its layer by overriding one property:

```xml
<module.outputDir>${project.basedir}/../plugins</module.outputDir>
```

Many loaders rather than one, so two plugins can bundle different versions of
the same library without colliding.

**Third-party dependencies always go to the boot layer**, even for a plugin
whose own JAR lands in `plugins/` — the child layer's configuration is resolved
against the boot configuration, so a plugin's libraries must be visible there.

**`common` depends on nothing, and must stay that way.** Every module requires
it, so a JSON or JDBC dependency added there is forced onto every sensor plugin
in the system.

**`core` requires `common` and nothing else.** A `requires` on a sensor module is
exactly what would stop a plugin JAR from being droppable into `plugins/`
without rebuilding the host. The host must reach plugins only through
`ServiceLocator`.

**Plugins export nothing.** A plugin that exports a package invites a second
plugin to depend on it, and two plugins that know about each other can no longer
be removed independently.

**The pipeline modules are dissolved.** `reading-store`, `sensor-registry` and
`realtime-gateway` become concerns of `core`, reachable through their `common`
interfaces, so promoting one back to its own module later is a `provides` clause
and nothing else. `mqtt-ingestion` is dissolved into the plugins: each sensor
plugin owns its own transport and subscribes to its own topic filter, so a new
sensor type brings its own connection without touching the host.

**`uses` directives belong in `common`, not in the consumers.**
`ServiceLocator.locateAll` calls `ServiceLoader.load(layer, X)` from inside
`common`, and JPMS resolves the service-use declaration against the *calling*
module. A missing clause fails at runtime with the providers present on the
module path:

```
ServiceConfigurationError: com.example.X: module com.buildingassessment.common
does not declare `uses`
```

`core` needs no `uses` clauses at all, because it never calls `ServiceLoader`
itself.

**Service roles are split by cardinality.** `common.services` is for roles with
exactly one active implementation, resolved with `ServiceLocator.locateOne`,
which throws if two providers are present — an ambiguous store is a
configuration error, not something to settle by iteration order.
`common.services.spi` is for the plugin surface, resolved with `locateAll`.

### Adding a sensor type

1. Copy `sensor-temperature/` and rename it.
2. Implement the SPI contracts from `common.services.spi`.
3. `provides` them in its `module-info.java`.
4. Add the module to the root `pom.xml`.

No existing module is edited and `core` is not rebuilt. An already-built plugin
JAR can be added to or removed from `plugins/` between runs with no build at
all.

## Consequences

- `ServiceLocator` is a singleton enum that builds the plugin layer once, in its
  constructor. Nothing can be discovered before it is first touched, and the
  plugin directory is read relative to the working directory — which
  `mvn -N exec:exec` sets to the repository root.
- An absent or empty `plugins/` directory is not an error; the system then runs
  with the boot layer alone.
- A plugin whose provider throws during construction is logged and skipped
  rather than taking startup down. One bad sensor JAR should not stop the others
  from reporting.
- `exports` on an empty package and `provides` naming a non-existent class are
  both compile errors, so the `module-info.java` files grow as the code does
  rather than being written up front.
- Frontend lives in `core/ui/`, beside `core/src/`, rather than inside the Java
  source tree as in the ST4 project. ST4's UI is raw `.jsx` with no build step;
  with Vite, `node_modules/` and `dist/` inside `src/main/java` would be scanned
  by Maven and the IDE.

## Open items

Carried over from ADR-0001 and still undecided:

- **The contracts in `common`** — data types, service roles and the plugin SPI.
  Everything else here depends on these, and they are deliberately not designed
  yet.
- The real sensor's MQTT topic/payload format. Only the decoder for a given
  sensor type should ever parse bytes, so this stays contained to one plugin.
- MQTT broker hosting: local Mosquitto via docker-compose vs. cloud.
- Java MQTT client library (Eclipse Paho vs. HiveMQ). Whichever is chosen
  becomes a boot-layer dependency of each sensor plugin that uses it.
- Realtime transport to the frontend: WebSocket vs. SSE.
- Persistence: in-memory only, or a time-series store from the start.
- Visual design of the dashboard — deferred on purpose.
- Language/runtime for the `simulators` component.
