import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

plugins {
    kotlin("multiplatform")
}


val devMode = properties.containsKey("jsWatch")


kotlin {
    js {
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
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$kotlinReactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$kotlinReactDomVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion:$kotlinEmotionVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-material:$kotlinMuiVersion")

                implementation(npm("core-js", coreJsVersion))
                implementation(npm("@mui/icons-material", muiIconsVersion))
                implementation(npm("react-select", reactSelectVersion))

                // NB: avoid "unmet peer dependency" warning
                implementation(npm("@babel/core", babelCoreVersion))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}


run {}


// https://youtrack.jetbrains.com/issue/KT-52578/KJS-Gradle-KotlinNpmInstallTask-gradle-task-produces-unsolvable-warning-ignored-scripts-due-to-flag.
yarn.ignoreScripts = false