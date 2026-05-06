# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Build commands

```bash
# First-time setup (generates gradle-wrapper.jar)
gradle wrapper --gradle-version 8.5

# Build the mod jar
./gradlew build

# Build jar only (skip sources jar)
./gradlew jar

# Run a local Fabric test server (provided by Fabric Loom)
./gradlew runServer
```

There are no tests in this project yet. Compilation is the primary correctness check — `./gradlew build` will catch any type or import errors.

## Architecture

This is a **Fabric 1.20.1 server-side mod** (not Paper/Spigot). It is **not a plugin** — it is a Fabric mod that runs on the server. The entrypoint is `KingdomsPlugin.java` which implements `ModInitializer`.

### Startup sequence

`KingdomsPlugin.onInitialize()` runs once when the server starts:
1. Copies `config.yml` from classpath to the server's config dir if absent
2. Loads config via `ConfigLoader` (SnakeYAML → typed records, immutable after load)
3. Opens `PersistenceService`, which connects to SQLite and runs `SchemaManager` to apply `schema.sql`

### Data layer

All persistence goes through `PersistenceService`, which owns a single `java.sql.Connection` for the server's lifetime. Access repositories via `persistence.players()`, `persistence.elections()`, etc. — never instantiate repositories directly.

Repository methods declare `throws SQLException` — callers (game logic) are responsible for handling or wrapping these. SQLite upserts use `ON CONFLICT(pk) DO UPDATE SET ...` (not `INSERT OR REPLACE`) to avoid triggering delete-then-insert semantics.

### Config

`ConfigLoader` exposes nine nested `record` types (e.g., `config.office()`, `config.voting()`). Config is read once at startup and never reloaded. All fields have hardcoded defaults so the server starts even with a missing or partial `config.yml`.

### Origins integration

Origins is a **soft/optional dependency** — `modCompileOnly` only, not bundled. Any code that calls Origins APIs must guard with a runtime presence check (e.g., `FabricLoader.getInstance().isModLoaded("origins")`). Never add Origins to `modImplementation`.

Origins and its transitive deps (Cardinal Components, Calio) are stored as local JARs in `libs/`:
- `origins-fabric-1.10.1+1.20.x.jar`
- `cardinal-components-base-5.2.1.jar`, `cardinal-components-entity-5.2.1.jar`
- `calio-1.11.2+mc.1.20.x.jar`

All are declared as `modCompileOnly(fileTree("libs"))` — Fabric Loom remaps them for compile but they are **not** bundled into the output jar.

Use this for documentation: https://origins.readthedocs.io/en/latest/

### Map integration

BlueMap and Dynmap are **soft/optional compile-only** dependencies fetched from Maven repos (not local JARs):
- `compileOnly("de.bluecolored:bluemap-api:2.7.5")` — from `https://repo.bluecolored.de/releases`
- `compileOnly("us.dynmap:DynmapCoreAPI:3.4-beta-3")` — from `https://repo.mikeprimm.com/`

`MapIntegrationService` uses fully-qualified class names and wraps all calls in `try/catch` (including `NoClassDefFoundError`) so the mod compiles and runs regardless of whether BlueMap/Dynmap is present. The map provider is selected via `config.map().provider()` (`"bluemap"`, `"dynmap"`, or `"none"`).

### Key package layout

```
com.example.kingdoms
├── KingdomsPlugin.java          # ModInitializer entrypoint
├── config/
│   └── ConfigLoader.java        # YAML → typed records
└── db/
    ├── PersistenceService.java  # Connection owner, repository factory
    ├── SchemaManager.java       # Applies schema.sql from classpath
    ├── model/                   # Plain POJOs, one per DB table
    └── repository/              # JDBC CRUD, one per table
```

### Fabric-specific notes

- `fabric.mod.json` uses `${version}` substitution — the value comes from `project.version` in `build.gradle.kts` via `processResources`.
- SQLite JDBC and SnakeYAML are bundled into the output jar via Loom's `include()` — no server-side lib installation needed.
- Origins, Cardinal Components, Calio, BlueMap, and Dynmap are all **compile-only** — never bundle them.
- The mod ID is `kingdoms_of_origin` (underscores). Use this when registering events, identifiers, or scheduler keys.
- Use this for documentation: https://docs.fabricmc.net/develop/