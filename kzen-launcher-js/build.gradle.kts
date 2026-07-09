import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}


// Read via providers.gradleProperty (tracked by the configuration cache), NOT properties.containsKey
// (reads the untracked legacy project-properties map). With containsKey, a cached config entry built
// without -PjsWatch is silently reused on later -PjsWatch runs, so the dev loop bundles the minified
// production executable instead of the development one and edits never reach the screen.
val devMode = providers.gradleProperty("jsWatch").isPresent


kotlin {
    js {
        // ESM output (MUI 9 ships proper ESM via package.json exports, which unblocked this —
        // under MUI 6, useEsModules() broke @mui/icons-material with "'createSvgIcon' has no
        // default export").
        // NB: @mui/icons-material is pinned to match the @mui/material version the
        // kotlin-wrappers BOM resolves (9.2.0) — keep them aligned on any wrappers bump.
        useEsModules()
        binaries.executable()

        browser {
            val webpackMode =
                if (devMode) {
                    KotlinWebpackConfig.Mode.DEVELOPMENT
                }
                else {
                    KotlinWebpackConfig.Mode.PRODUCTION
                }

            // https://youtrack.jetbrains.com/issue/KTIJ-26086
            commonWebpackConfig {
                mode = webpackMode
            }
        }

        if (devMode) {
            compilations.all {
                compileTaskProvider.configure {
                    compilerOptions.freeCompilerArgs.add("-Xir-minimized-member-names=false")
                }
            }
        }
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":kzen-launcher-common"))

            implementation(kotlinWrappers.react)
            implementation(kotlinWrappers.reactDom)
            implementation(kotlinWrappers.emotion.styled)
            implementation(kotlinWrappers.mui.material)

            implementation(npm("core-js", coreJsVersion))
            implementation(npm("@mui/icons-material", muiIconsVersion))

            // NB: avoid "unmet peer dependency" warning
            implementation(npm("@babel/core", babelCoreVersion))

            // esbuild bundler (replaces webpack) — see jsEsbuildBundle task below
            implementation(npm("esbuild", esbuildVersion))
        }

        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}


// https://youtrack.jetbrains.com/issue/KT-52578/KJS-Gradle-KotlinNpmInstallTask-gradle-task-produces-unsolvable-warning-ignored-scripts-due-to-flag.
yarn.ignoreScripts = false


// === esbuild bundler (replaces webpack) ==========================================================
// Bundles the Kotlin/JS per-module CommonJS output with esbuild instead of webpack. esbuild bundles
// in ~1s vs webpack's ~28s for this module, and ships per-platform native binaries via npm so it
// stays Windows/Linux/macOS agnostic. The output filename + dist location match what the JVM server
// serves (build/dist/js/productionExecutable/<jsModuleName>.js) and what ProcessResources copies.

// KGP names the npm package "<rootProject>-<project>"; the per-module entry file shares that name.
val npmPackageName = "${rootProject.name}-${project.name}"
// The compileSync output dir holds one .js per Gradle module (kotlin-kotlin-stdlib.js,
// kzen-launcher-kzen-launcher-common.js, kzen-lib-kzen-lib.js, …); the entry only require()s them.
// Declare the whole dir as the task input — NOT just the entry file — so a change in any dependency
// module re-triggers the bundle. With only inputs.file(entry), such a change lands in a sibling file,
// the entry stays byte-identical, and jsEsbuildBundle wrongly stays UP-TO-DATE.
val esbuildInputDir = rootProject.layout.buildDirectory
    .dir("js/packages/$npmPackageName/kotlin")
val esbuildEntry = esbuildInputDir.map { it.file("$npmPackageName.mjs") }
val esbuildOutFile = layout.buildDirectory
    .file("dist/js/productionExecutable/${project.name}.js")

// esbuild's native binary lives in the per-platform optional dependency npm pulls in for this OS.
fun esbuildBinaryPath(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val isArm = arch.contains("aarch64") || arch.contains("arm")
    val (pkg, exe) = when {
        os.contains("win") -> "win32-x64" to "esbuild.exe"
        os.contains("mac") || os.contains("darwin") ->
            (if (isArm) "darwin-arm64" else "darwin-x64") to "bin/esbuild"
        else -> (if (isArm) "linux-arm64" else "linux-x64") to "bin/esbuild"
    }
    return rootProject.layout.buildDirectory
        .file("js/node_modules/@esbuild/$pkg/$exe").get().asFile.absolutePath
}

val jsEsbuildBundle = tasks.register<Exec>("jsEsbuildBundle") {
    group = "kotlin browser"
    description = "Bundle the Kotlin/JS output with esbuild (replaces webpack)"

    val production = ! devMode

    // In dev mode bundle the development executable (no DCE — recompiles in ~2s); else production.
    dependsOn(if (production) "jsProductionExecutableCompileSync" else "jsDevelopmentExecutableCompileSync")
    // esbuild resolves react / react-dom / etc. from build/js/node_modules; kotlinNpmInstall populates
    // it. The compileSync tasks don't depend on it (the compiler only emits require() calls, it doesn't
    // need the modules present), so without this edge esbuild can run against an empty node_modules and
    // fail with "Could not resolve react". webpack's bundle task depended on kotlinNpmInstall for this.
    dependsOn(rootProject.tasks.named("kotlinNpmInstall"))

    inputs.dir(esbuildInputDir)
    outputs.file(esbuildOutFile)

    val invocation = buildList {
        add(esbuildBinaryPath())
        add(esbuildEntry.get().asFile.absolutePath)
        add("--bundle")
        add("--format=iife")
        add("--platform=browser")
        add("--sourcemap")
        add("--outfile=${esbuildOutFile.get().asFile.absolutePath}")
        if (production) {
            add("--minify")
            add("--legal-comments=external")
        }
    }
    commandLine(invocation)
}

// esbuild (jsEsbuildBundle) replaces webpack for this module; disable the webpack tasks so
// `build`/`assemble` don't pay the webpack cost. Re-enable them to fall back to webpack.
tasks.matching {
    it.name == "jsBrowserProductionWebpack" ||
        it.name == "jsBrowserDevelopmentWebpack" ||
        it.name == "jsBrowserDistribution"
}.configureEach {
    enabled = false
}

// Dev loop: `./gradlew -t :kzen-launcher-js:jsEsbuildBundle -PjsWatch` (bundles the development
// executable, unminified, ~1s). Production bundling runs via the JVM jar's ProcessResources, which
// depends on jsEsbuildBundle. We deliberately do NOT wire `assemble` to jsEsbuildBundle: `assemble`
// builds both the production and development executables, whose compileSync tasks write the same
// packages dir, which would make the single esbuild input ambiguous to Gradle.