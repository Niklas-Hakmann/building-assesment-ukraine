# Building Assessment Ukraine — Sensor Dashboard

A web dashboard that visualizes building sensor values in real time: one real
physical sensor plus several simulated sensors, all communicating over MQTT, with
support for multiple sensor types.

See [ADR-0001](docs/architecture/ADR-0001-architecture-overview.md) for the
overall architecture and [ADR-0002](docs/architecture/ADR-0002-plugin-architecture.md)
for the backend's plugin structure and the currently open items.

> **State:** scaffolding only. The module structure, the two JPMS runtime layers
> and `ServiceLocator` work and are verified. The contracts in `common` and all
> module functionality are still to be written.

## Structure

Maven multi-module build, aggregator at the repository root. Requires Java 21+
and Node 18+.

```
pom.xml               aggregator
common/               contracts — depends on nothing, and must stay that way
  .../common/data/        types that travel between modules          (empty)
  .../common/services/    roles with one active impl — "choose one"   (empty)
  .../common/services/spi/  plugin surface — "add as many as you like" (empty)
  .../common/util/        ServiceLocator
core/                 host — requires common only
  .../core/server/        Main and wiring                            (empty)
  ui/                     dashboard (Vite + React + TypeScript)
sensor-vibration/     sensor plugin — the real vibration/strain device  (empty)
sensor-temperature/   sensor plugin — simulated                        (empty)

mods-mvn/             build output: boot module layer
plugins/              build output: sensor plugin JARs, loaded into a child layer at runtime

docs/                 architecture decision records
infra/                MQTT broker and other local/deployment infrastructure (not yet defined)
simulators/           mock sensor publisher(s) over MQTT, config-driven (not yet implemented)
```

## Building

```bash
mvn clean install     # builds, and installs the JARs into the two runtime layers
mvn -N exec:exec      # runs the host — fails until core.server.Main exists
```

On Windows, `./build-windows.ps1` does the same but stops any running `java`
first — Windows holds a lock on a JAR that is on the module path, which makes
`mvn clean` fail while the app is running.

The `-N` on `exec:exec` matters: the goal is configured on the root project only,
so letting Maven descend into the modules fails on a missing executable.

### Dashboard

```bash
cd core/ui
npm install
npm run dev           # Vite on :5173, proxying /api to the host on :8080
npm run build         # emits dist/
```

## How the plugin layer works

Two runtime layers. `mods-mvn/` is the boot layer, resolved by the JVM at
startup from `--module-path`. `plugins/` is a child layer that `ServiceLocator`
builds at startup from whatever JARs it finds there, with
`defineModulesWithManyLoaders` — many loaders rather than one, so two plugins can
bundle different versions of the same library without colliding.

A module chooses its layer with one property in its `pom.xml`:

```xml
<module.outputDir>${project.basedir}/../plugins</module.outputDir>
```

That is the only difference between a boot module and a drop-in plugin. After
`mvn clean install`:

```
mods-mvn/   common-0.1.0-SNAPSHOT.jar  core-0.1.0-SNAPSHOT.jar
plugins/    sensor-vibration-0.1.0-SNAPSHOT.jar  sensor-temperature-0.1.0-SNAPSHOT.jar
```

`core` requires `common` and nothing else, and must keep it that way: a
`requires` on a sensor module is what would stop a plugin JAR from being
droppable into `plugins/` without rebuilding the host.

### Adding a sensor type

1. Copy `sensor-temperature/` and rename it.
2. Implement the SPI contracts from `common.services.spi`.
3. `provides` them in its `module-info.java`.
4. Add the module to the root `pom.xml`.

No existing module is edited, and `core` is not rebuilt.

## Two things that will bite you

**`uses` directives go in `common`, not in the module that wants the service.**
`ServiceLocator` calls `ServiceLoader.load(layer, X)` from inside `common`, and
JPMS checks the service-use declaration against the *calling* module. A missing
clause fails at runtime with the providers sitting right there on the module
path:

```
ServiceConfigurationError: com.example.X: module com.buildingassessment.common
does not declare `uses`
```

The fix is always a `uses` line in `common/module-info.java`.

**Third-party dependencies always land in `mods-mvn/`, never `plugins/`.** The
plugin layer's configuration is resolved against the boot configuration, so a
library a plugin reads has to be visible in the boot layer. Only the plugin's own
JAR goes to `plugins/`. The root `pom.xml` already routes dependencies this way.

## One build-level gotcha

`exports` on an empty package is a compile error, and so is a `provides` clause
naming a class that does not exist. The `module-info.java` files are therefore
currently minimal; add each `exports`, `uses` and `provides` line as the package
or class it names appears. Each file carries a comment saying what to add.
