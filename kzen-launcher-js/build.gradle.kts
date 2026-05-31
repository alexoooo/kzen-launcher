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

                // esbuild-based webpack minifier (replaces Terser in production) — see webpack.config.d
                implementation(npm("esbuild-loader", esbuildLoaderVersion))
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