import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("org.jetbrains.kotlin.js")
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
}


dependencies {
    implementation(project(":kzen-launcher-common"))

    implementation("org.jetbrains.kotlinx:kotlinx-html-assembly:$kotlinxHtmlVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$kotlinReactVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$kotlinReactDomVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion:$kotlinEmotionVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-mui:$kotlinMuiVersion")

    implementation(npm("core-js", coreJsVersion))
//    implementation(npm("react", reactVersion))
//    implementation(npm("react-dom", reactVersion))
//    implementation(npm("react-is", reactVersion))
//    implementation(npm("inline-style-prefixer", inlineStylePrefixerVersion))
//    implementation(npm("styled-components", styledComponentsVersion))
//    implementation(npm("@material-ui/core", materialUiCoreVersion))
//    implementation(npm("@material-ui/icons", materialUiIconsVersion))
    implementation(npm("@mui/icons-material", muiIconsVersion))
    implementation(npm("react-select", reactSelectVersion))

    testImplementation(kotlin("test"))
}


run {}