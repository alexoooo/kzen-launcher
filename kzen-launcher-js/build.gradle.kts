plugins {
    id("org.jetbrains.kotlin.js")
}


kotlin {
//    js(IR) {
//        binaries.executable()
//        browser {}
//    }

    js {
        useCommonJs()
        browser {}
    }
}


dependencies {
    implementation(project(":kzen-launcher-common"))

    implementation(npm("core-js", coreJsVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-html-assembly:$kotlinHtmlVersion")
    implementation("org.jetbrains:kotlin-react:$kotlinReactVersion")
    implementation("org.jetbrains:kotlin-react-dom:$kotlinReactDomVersion")
    implementation("org.jetbrains:kotlin-styled:$kotlinStyledVersion")
    implementation("org.jetbrains:kotlin-extensions:$kotlinExtensionsVersion")
    implementation("org.jetbrains:kotlin-css-js:$kotlinCssVersion")
    implementation(npm("react", reactVersion))
    implementation(npm("react-dom", reactVersion))
    implementation(npm("react-is", reactVersion))
    implementation(npm("inline-style-prefixer", inlineStylePrefixerVersion))
    implementation(npm("styled-components", styledComponentsVersion))
    testImplementation("org.jetbrains.kotlin:kotlin-test-js")

    implementation(npm("@material-ui/core", materialUiCoreVersion))
    implementation(npm("@material-ui/icons", materialUiIconsVersion))
    implementation(npm("react-select", reactSelectVersion))
}


run {}