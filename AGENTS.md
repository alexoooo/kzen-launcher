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

The launcher fat jar gets repackaged into `kzen-launcher-<v>.zip` (hand-built, NOT a Gradle task) and bundled with `dependencies/`. That zip is what `kzen-shell` downloads/unpacks and spawns on startup.

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

- **Hard-coded archetype URL.** `KzenLauncherMain.kt:99` pins:
  ```
  file:///C:/Users/ostro/IdeaProjects/kzen-project/kzen-project-jvm/build/libs/kzen-project-0.29.1-SNAPSHOT.zip
  ```
  Bumping the project version (or running on another machine) means editing this line *and* hand-rebuilding the `kzen-project-<v>.zip`. There's a commented-out GitHub releases URL on the next line as the alternate source.
- **No connection to kzen-lib's notation model.** Don't reach for `ObjectLocation` / `GraphStructure` / etc. here — those concepts don't apply. The launcher is a plain Ktor REST app.
- **`ProjectCreator.kt:62`** renames the downloaded archetype jar to `main.jar` inside the project home directory. kzen-shell's `MainJarRunner` always looks for `main.jar` — don't break this convention.
- **kotlin-wrappers ceiling is `2025.12.11`** (same as kzen-auto). Blast radius for the future upgrade is tiny here: 19 `.kt` files total, only 4 use `RComponent` (`ProjectLauncher`, `ProjectList`, `ProjectRunning`, `ManageProjectsScreen`), zero `key`/`ChangeEvent` breakage. Whatever idiom kzen-auto adopts when it migrates, mirror it here. See [`../kzen-auto/docs/js-architecture.md`](../kzen-auto/docs/js-architecture.md) for the broader pre-refactor picture.

## Pointers

- **Composite build + toolchain** → [`../kzen/AGENTS.md`](../kzen/AGENTS.md).
- **Runtime host** → [`../kzen-shell/AGENTS.md`](../kzen-shell/AGENTS.md) (kzen-shell downloads and spawns the launcher).
- **What gets launched** → [`../kzen-project/AGENTS.md`](../kzen-project/AGENTS.md) (the typical archetype).
