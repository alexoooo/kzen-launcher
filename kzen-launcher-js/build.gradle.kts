import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("multiplatform")
}


val devMode = properties.containsKey("jsWatch")


kotlin {
    js {
        // useEsModules() breaks @mui/icons-material 7.3.11 ('createSvgIcon' has no
        // default export under ESM resolution). Stay on CommonJS until MUI is bumped
        // to a version that ships ESM, or kotlin-wrappers updates the MUI binding.
        useCommonJs()
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
        val jsMain by getting {
            dependencies {
                implementation(project(":kzen-launcher-common"))

                implementation("org.jetbrains.kotlinx:kotlinx-html-assembly:$kotlinxHtmlAssemblyVersion")

                implementation(kotlinWrappers.react)
                implementation(kotlinWrappers.reactDom)
                implementation(kotlinWrappers.emotion.styled)
                implementation(kotlinWrappers.mui.material)

                implementation(npm("core-js", coreJsVersion))
                implementation(npm("@mui/icons-material", muiIconsVersion))
                implementation(npm("react-select", reactSelectVersion))

                // NB: avoid "unmet peer dependency" warning
                implementation(npm("@babel/core", babelCoreVersion))

                // esbuild bundler (replaces webpack) — see jsEsbuildBundle task below
                implementation(npm("esbuild", esbuildVersion))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
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
val esbuildEntry = rootProject.layout.buildDirectory
    .file("js/packages/$npmPackageName/kotlin/$npmPackageName.js")
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

    inputs.file(esbuildEntry)
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

// esbuild (jsEsbuildBundle) replaces webpack for this module; disable the now-unused webpack tasks so
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