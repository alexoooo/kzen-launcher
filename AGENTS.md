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
| `tech.kzen.launcher.server.KzenLauncherMain` | kzen-launcher-jvm | Production `fun main`. Builds the context, inits archetype repo + download service, starts Ktor. Default port `8080` (CLI override via `--server.port=<n>`). |
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
| `service/DownloadService.kt` | Downloads archetype zips; trusts bad certs (dev convenience) |
| `properties/KzenProperties.kt` | Config & archetype defaults |
| `backend/Pages.kt` | Index page rendering |
| `dev/FrontendDevelopment.kt` | IDE-launched dev main |

`kzen-launcher-common/.../api/CommonRestApi.kt` is the shared REST contract; touch both client and server when changing it.

## Gotchas

- **Project-archetype source is config, not a hard-coded path.** `resolveArchetypeUrl()` (`KzenLauncherMain.kt`) reads the bundled classpath resource `kzen-launcher-jvm/src/main/resources/kzen-launcher.properties` — an ordered `archetype.project.N` candidate list (dev `../kzen-project/.../build/dist/*.zip` paths first, the GitHub release URL last); the first that resolves wins, so an end-user machine with no source tree falls through to the release URL. `file://` candidates are re-acquired on every launcher start (`ArchetypeRepo.init` `scheme == "file"` branch), so a rebuilt project dist is picked up automatically. The relative dev candidates resolve despite the launcher's unstable CWD (`../work/kzen-launcher/<v>/`) because its offset from the shared parent dir is fixed and known; `ArchetypeRepo.install` downloads to a `.part` file then atomic-moves, so a truncated download can't leave a bad cached zip. On a version bump edit the versions in those lines. Full release procedure: [`../kzen/docs/RELEASING.md`](../kzen/docs/RELEASING.md).
- **No connection to kzen-lib's notation model.** Don't reach for `ObjectLocation` / `GraphStructure` / etc. here — those concepts don't apply. The launcher is a plain Ktor REST app.
- **`ProjectCreator.kt:62`** renames the downloaded archetype jar to `main.jar` inside the project home directory. kzen-shell's `MainJarRunner` always looks for `main.jar` — don't break this convention.
- **A created project is frozen, not a cache.** `ProjectCreator.create` refuses an existing project home (`check(!Files.exists(home))`) — a project is a saved document, immutable once created; it is never re-downloaded or upgraded in place. Only a *failed/partial* create is cleaned up: `create` builds into a sibling `.staging/`, verifies `main.jar`, then atomic-moves into place, so a crash mid-create can't poison the home path.
- **kotlin-wrappers is at `2026.7.1`** (bumped 2026-07-07 alongside the JVM 26 / Gradle 9.6.1 update; kzen-auto tracks the same version). The initial 2026.x upgrade landed 2026-05-11 with a shallow patch (no FC migration). Three pieces of scaffolding in `wrap/React.kt` carry the load: (1) `RPureComponent` was re-implemented as `Component` + `shouldComponentUpdate` that shallow-compares props and state, because `react.PureComponent` was removed from the wrappers — the new class has the same shape as `RComponent`, callers don't change; (2) a new `KClass<out Component<P, *>>.react` extension property bridges the old `SomeComponent::class.react { ... }` DSL (which lived in `kotlin-react-legacy`, also gone) to the modern `ElementType.invoke`, by casting `KClass.js` to `ComponentType` via `js.reflect.unsafeCast`; (3) `kotlinWrappers.reactLegacy` was dropped from `kzen-launcher-js/build.gradle.kts`. Two `key = <String>` sites (`ProjectList.kt`, `ProjectRunning.kt`) were wrapped in `react.Key(...)` for the new `Key?` type. This was used as the template for the kzen-auto migration (also done 2026-05-11). kzen-auto needed one additional bridge — a `createRef<T>()` top-level — because kzen-auto's class components use `react.createRef` heavily and that was also removed; kzen-launcher itself doesn't use refs so it didn't surface here. Copy this `wrap/React.kt` scaffolding verbatim for any future JS sibling that bumps to 2026.x+; only the package path differs.

## Pointers

- **Composite build + toolchain** → [`../kzen/AGENTS.md`](../kzen/AGENTS.md).
- **Runtime host** → [`../kzen-shell/AGENTS.md`](../kzen-shell/AGENTS.md) (kzen-shell downloads and spawns the launcher).
- **What gets launched** → [`../kzen-project/AGENTS.md`](../kzen-project/AGENTS.md) (the typical archetype).
