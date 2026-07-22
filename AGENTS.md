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

- **Project-archetype source is config, not a hard-coded path.** `resolveArchetypeUrl()` (`KzenLauncherMain.kt`) reads the bundled classpath resource `kzen-launcher-jvm/src/main/resources/kzen-launcher.properties` — an ordered `archetype.project.N` candidate list (dev `../kzen-project/.../build/dist/*.zip` paths first, the GitHub release URL last); the first that resolves wins, so an end-user machine with no source tree falls through to the release URL. `file://` candidates are re-acquired on every launcher start (`ArchetypeRepo.init` `scheme == "file"` branch), so a rebuilt project dist is picked up automatically. The relative dev candidates resolve despite the launcher's unstable CWD (`../work/kzen-launcher/<v>/`) because its offset from the shared parent dir is fixed and known; `ArchetypeRepo.install` downloads to a `.part` file then atomic-moves, so a truncated download can't leave a bad cached zip. On a version bump edit the versions in those lines. Full release procedure: [`../kzen/docs/RELEASING.md`](../kzen/docs/RELEASING.md).
- **No connection to kzen-lib's notation model.** Don't reach for `ObjectLocation` / `GraphStructure` / etc. here — those concepts don't apply. The launcher is a plain Ktor REST app.
- **`ProjectCreator.kt:62`** renames the downloaded archetype jar to `main.jar` inside the project home directory. kzen-shell's `MainJarRunner` always looks for `main.jar` — don't break this convention.
- **A created project can be upgraded in place; its data is never touched.** `ProjectCreator.create` refuses an existing project home (`check(!Files.exists(home))`) — creation builds into a sibling `.staging/`, verifies `main.jar`, then atomic-moves into place, so a crash mid-create can't poison the home path. `ProjectCreator.upgrade` (SH4) replaces just the program — `main.jar` + `dependencies/` — from a selected archetype, extracting into a sibling `.upgrade/` and swapping through `.old` backups; everything else (`notation/`, `work/`, `logs/`, user files) is preserved, and the new archetype's own seed notation is deliberately NOT imported. **Windows file locks are the enforcement mechanism, not just a failure mode:** the swap renames `main.jar` first (the child JVM holds it open via its classpath `JarFile`, without `FILE_SHARE_DELETE`), so on a running project the rename fails and the whole operation rolls back to a byte-identical home and throws `IllegalStateException` → **409 Conflict** (`respondCommand`). The order (jar, then the `dependencies/` directory) means a running project is caught before anything in home changes. POSIX has no such lock, so upgrading a running project there succeeds silently (old inodes keep serving) — the same accepted residual as `delete`; the launcher server has no channel to the shell, so the real guard is the client filtering running projects out of the manage list. The launcher records the archetype base name + version it upgraded to; a **downgrade / same-version reinstall** is allowed (with a client-side warning) — a rebuilt dev snapshot re-acquired under the same version string is the reinstall case that keeps the upgrade path testable in the dev loop.
- **The project registry is loaded once and persisted atomically.** `ProjectRepo` reads `<project.home>/kzen-projects.yaml` at construction into an immutable snapshot; reads serve that snapshot (no per-request disk I/O), mutations are `@Synchronized` and each persists via a `.tmp` sibling plus `ATOMIC_MOVE` before publishing the new snapshot. Each project entry records `home`, `args`, and (SH4) `archetype` + `version` — the archetype base name and version it was created/upgraded from, so the client can offer newer versions. **These two fields are additive**: legacy registries and hand-edits without them bind to `ProjectRepo.unknownValue` (`"unknown"`), and an unknown-version project is offered *every* cached version as an upgrade so it can adopt version tracking via one. Consequences of load-once: a hand-edit made while the launcher is **stopped** is reflected on relaunch, but one made while it **runs** is invisible and gets overwritten by the next mutation; an unparseable file fails the boot loudly (rather than silently starting empty, which would let the next mutation persist an empty registry over the user's list); two launcher processes sharing one home are unsupported (whole-file last-writer-wins). The interactive and kzen-shell-spawned launchers resolve *different* homes unless pointed at the same `--project.home` — the resolved absolute path is logged at boot for exactly this reason. `RestHandler.listProjects`' per-entry `Files.exists` is a deliberate liveness signal for the UI, not registry I/O.
- **Version ordering is shared, in `kzen-launcher-common`.** `VersionNumbers` (commonMain) is the single source of version comparison — numeric components with a trailing snapshot flag so `X-SNAPSHOT` sorts **above** the equal release `X`, and an unparseable version (`kzen-project-custom.zip`) sorts last but is still offered (never hide a cached artifact). `ArchetypeRepo` (server, catalogue sort) and the JS client (New Project's latest-per-name filter, the Upgrade offer gating, dialog ordering, and the downgrade warning) both delegate to it, so client and server can't disagree about what "newer" means. New Project shows only the latest version per archetype name; the full per-version list is for the Upgrade action.
- **kotlin-wrappers is at `2026.7.1`** (bumped 2026-07-07 alongside the JVM 26 / Gradle 9.6.1 update; kzen-auto tracks the same version). The initial 2026.x upgrade landed 2026-05-11 with a shallow patch (no FC migration). Three pieces of scaffolding in `wrap/React.kt` carry the load: (1) `RPureComponent` was re-implemented as `Component` + `shouldComponentUpdate` that shallow-compares props and state, because `react.PureComponent` was removed from the wrappers — the new class has the same shape as `RComponent`, callers don't change; (2) a new `KClass<out Component<P, *>>.react` extension property bridges the old `SomeComponent::class.react { ... }` DSL (which lived in `kotlin-react-legacy`, also gone) to the modern `ElementType.invoke`, by casting `KClass.js` to `ComponentType` via `js.reflect.unsafeCast`; (3) `kotlinWrappers.reactLegacy` was dropped from `kzen-launcher-js/build.gradle.kts`. Two `key = <String>` sites (`ProjectList.kt`, `ProjectRunning.kt`) were wrapped in `react.Key(...)` for the new `Key?` type. This was used as the template for the kzen-auto migration (also done 2026-05-11). kzen-auto needed one additional bridge — a `createRef<T>()` top-level — because kzen-auto's class components use `react.createRef` heavily and that was also removed; kzen-launcher itself doesn't use refs so it didn't surface here. Copy this `wrap/React.kt` scaffolding verbatim for any future JS sibling that bumps to 2026.x+; only the package path differs.

## Pointers

- **Composite build + toolchain** → [`../kzen/AGENTS.md`](../kzen/AGENTS.md).
- **Runtime host** → [`../kzen-shell/AGENTS.md`](../kzen-shell/AGENTS.md) (kzen-shell downloads and spawns the launcher).
- **What gets launched** → [`../kzen-project/AGENTS.md`](../kzen-project/AGENTS.md) (the typical archetype).
