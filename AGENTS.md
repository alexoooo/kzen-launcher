# kzen-launcher — AI agent guide

## Purpose

kzen-launcher is the **project selector UI**: a small Ktor + Kotlin/JS app that the desktop shell boots first. It downloads project archetype zips, unpacks them into per-project home directories, and hands control to kzen-shell for spawning the project's `main.jar`. Visually, this is the "pick a project to open" screen the user sees first.

It does NOT consume kzen-lib's notation/CQRS model directly — it's a much simpler app than kzen-auto/kzen-project. Still, the toolchain rules and KMP module conventions apply.

## Module layout

Three Gradle subprojects:

- **`kzen-launcher-common`** — KMP shared code (`commonMain`/`commonTest`). API DTOs, REST contract types (`CommonRestApi`).
- **`kzen-launcher-jvm`** — Ktor server. Entry point `tech.kzen.launcher.server.KzenLauncherMain`. Owns archetype repo, project repo, download service, REST handlers.
- **`kzen-launcher-js`** — Kotlin/JS frontend.

## Entry points

| Class | Module | Purpose |
|----|----|----|
| `tech.kzen.launcher.server.KzenLauncherMain` | kzen-launcher-jvm | Production `fun main`. Builds the context, inits archetype repo + download service, starts Ktor. Default port `8080` (CLI override via `--server.port=<n>`). `--project.home=<path>` sets the project registry + project root; default `../kzen-proj` relative to CWD, and kzen-shell passes it explicitly. |
| `tech.kzen.launcher.server.dev.FrontendDevelopment` | kzen-launcher-jvm | IDE-launched dev backend paired with live-rebuild JS bundle. |
| Kotlin/JS `Main.kt` | kzen-launcher-js | JS entry point. |

## Dev loop

Frontend live reload. **Open kzen-launcher as its OWN IntelliJ project** (umbrella's KMP includeBuild breaks IDE run/debug).

```powershell
# Terminal 1 — IDE: run tech.kzen.launcher.server.dev.FrontendDevelopment
# Terminal 2:
./gradlew -t :kzen-launcher-js:build -x test -PjsWatch

# Browse: http://localhost:8080/
```

## Distribution build

```powershell
./gradlew jar
java -jar kzen-launcher-jvm/build/libs/kzen-launcher-jvm-*.jar
```

The `:kzen-launcher-jvm:dist` Gradle `Zip` task packages `main.jar` (the thin jar, `Class-Path` → `dependencies/`) + `dependencies/` into `build/dist/kzen-launcher-<v>.zip` — the layout kzen-shell's `ArtifactRepo` expects. That zip is what `kzen-shell` downloads/unpacks and spawns on startup. (Not wired into `build`.)

## Key directories

| Path (under `kzen-launcher-jvm/src/main/kotlin/tech/kzen/launcher/server/`) | What lives here |
|----|----|
| `KzenLauncherMain.kt` | Entry point, context wiring, `KzenLauncherConfig` (port parsing) |
| `api/RestHandler.kt` | REST endpoints handler |
| `archetype/` | `ArchetypeInfo`, `ArchetypeRepo` — known project archetype catalog |
| `project/` | `ProjectInfo`, `ProjectRepo`, `ProjectCreator` — user-created project instances |
| `security/SecurityGate.kt` | Fetch-metadata + Host gate (CSRF / DNS-rebinding); duplicated from kzen-shell — keep in sync |
| `service/DownloadService.kt` | Downloads archetype zips; validates TLS certs (custom trust store via `-Djavax.net.ssl.trustStore`) |
| `properties/KzenProperties.kt` | Config & archetype defaults |
| `backend/Pages.kt` | Index page rendering |
| `dev/FrontendDevelopment.kt` | IDE-launched dev main |

`kzen-launcher-common/.../api/CommonRestApi.kt` is the shared REST contract; touch both client and server when changing it.

## Gotchas

- **Project-archetype source is config, not a hard-coded path.** `resolveArchetypeUrl()` (`KzenLauncherMain.kt`) reads the bundled classpath resource `kzen-launcher-jvm/src/main/resources/kzen-launcher.properties` — an ordered `archetype.project.N` candidate list (dev `../kzen-project/.../build/dist/*.zip` paths first, the GitHub release URL last); the first that resolves wins. `file://` candidates are re-acquired on every launcher start, so a rebuilt project dist is picked up automatically; `ArchetypeRepo.install` downloads to a `.part` file then atomic-moves, so a truncated download can't leave a bad cached zip. On a version bump edit the versions in those lines — full procedure: [`../kzen/docs/RELEASING.md`](../kzen/docs/RELEASING.md).
- **The archetype catalogue is scan-derived — there is no `kzen-archetypes.yaml`.** `ArchetypeRepo` builds the New Project / Upgrade catalogue by scanning the archetype cache for `kzen-project-<version>.zip` files (one entry per version; a legacy `kzen-archetypes.yaml` is deleted at boot as residue). Boot reconciliation: re-acquire the current candidate (`file://` always, `https` if absent), acquire `archetype.project.released` — the latest published release, a separate `kzen-launcher.properties` key rolled forward at release time ([`../kzen/docs/RELEASING.md`](../kzen/docs/RELEASING.md)) — if absent, prune non-current `-SNAPSHOT` zips (only when the current acquisition succeeded), and clean `.part` orphans. Offline/404 boots degrade to serving cached zips instead of crashing.
- **No connection to kzen-lib's notation model.** Don't reach for `ObjectLocation` / `GraphStructure` / etc. here — those concepts don't apply. The launcher is a plain Ktor REST app.
- **`ProjectCreator.kt:62`** renames the downloaded archetype jar to `main.jar` inside the project home directory. kzen-shell's `MainJarRunner` always looks for `main.jar` — don't break this convention.
- **A created project can be upgraded in place; its data is never touched.** `ProjectCreator.create` refuses an existing project home; creation builds into a sibling `.staging/`, verifies `main.jar`, then atomic-moves into place. `ProjectCreator.upgrade` replaces just the program (`main.jar` + `dependencies/`) via a sibling `.upgrade/` and `.old` backups; `notation/`, `work/`, `logs/`, and user files are preserved, and the new archetype's seed notation is deliberately NOT imported. On Windows the swap renames `main.jar` first — a running project holds it locked, so the rename fails, the operation rolls back to a byte-identical home, and the API returns **409 Conflict**; that lock is the enforcement mechanism, and it doesn't exist on POSIX (there the guard is the client filtering running projects out of the manage list). Downgrade / same-version reinstall is allowed (with a client-side warning) — that's what keeps the upgrade path testable against rebuilt dev snapshots.
- **The project registry is loaded once and persisted atomically.** `ProjectRepo` reads `<project.home>/kzen-projects.yaml` at construction into an immutable snapshot; mutations are `@Synchronized` and persist via a `.tmp` sibling plus `ATOMIC_MOVE`. Entries record `home`, `args`, `archetype`, `version` — the last two are additive; entries without them bind to `"unknown"` and get offered every cached version as an upgrade. Consequences of load-once: a hand-edit while the launcher is stopped is picked up on relaunch, but one made while it runs is invisible and gets overwritten by the next mutation; an unparseable file fails the boot loudly; two launcher processes sharing one home are unsupported. The interactive and kzen-shell-spawned launchers resolve *different* homes unless pointed at the same `--project.home` — the resolved absolute path is logged at boot for exactly this reason.
- **Version ordering is shared, in `kzen-launcher-common`.** `VersionNumbers` (commonMain) is the single source of version comparison — numeric components with a trailing snapshot flag so `X-SNAPSHOT` sorts **above** the equal release `X`, and an unparseable version (`kzen-project-custom.zip`) sorts last but is still offered (never hide a cached artifact). `ArchetypeRepo` (server, catalogue sort) and the JS client (New Project's latest-per-name filter, the Upgrade offer gating, dialog ordering, and the downgrade warning) both delegate to it, so client and server can't disagree about what "newer" means. New Project shows only the latest version per archetype name; the full per-version list is for the Upgrade action.
- **kotlin-wrappers is at `2026.7.1`**, same as kzen-auto (see [`../kzen/AGENTS.md`](../kzen/AGENTS.md) Toolchain pins). The launcher's `wrap/React.kt` mirrors kzen-auto's migration scaffolding, minus `createRef` (the launcher uses no refs); the full template and 2026.x breakage catalogue live in [`../kzen-auto/docs/js-architecture.md`](../kzen-auto/docs/js-architecture.md) § React DSL wrapper layer.

## Headless verification

Never point a test boot at the user's real project home — `ArchetypeRepo.init` prunes stale snapshot zips, so the default home would mutate their dev cache. Two recipes:

- **Shell-simulator surface:** run `tech.kzen.launcher.server.dev.FrontendDevelopmentKt` (note the `Kt` — a top-level `main`) via `java -cp "<libs>/kzen-launcher-jvm-<v>.jar;<libs>/dependencies/*"` (`-cp`, not `-jar`, so the main class can be overridden) with `--server.port=<spare>`; drive `GET /shell/project` (JSON list), `/shell/project/start?name=X`, `/shell/project/stop?name=X` — simulator timings are ~2 s start / ~1 s stop, and a name containing `fail` resolves to `failed`.
- **Project registry:** run the production `KzenLauncherMainKt` with `--project.home=<temp dir>`, which isolates both `kzen-projects.yaml` and the archetype cache. First boot to a fresh home takes ~40 s (archetype downloads precede the Ktor bind). Drive `GET /rs/query/project`, `GET /rs/command/project/import?path=…` (+ `remove` / `delete` / `rename` / `args`); failures are 400/409 with a JSON `message`.

## Pointers

- **Composite build + toolchain** → [`../kzen/AGENTS.md`](../kzen/AGENTS.md).
- **Runtime host** → [`../kzen-shell/AGENTS.md`](../kzen-shell/AGENTS.md) (kzen-shell downloads and spawns the launcher).
- **What gets launched** → [`../kzen-project/AGENTS.md`](../kzen-project/AGENTS.md) (the typical archetype).
