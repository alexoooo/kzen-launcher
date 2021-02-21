plugins {
    kotlin("multiplatform") version kotlinVersion apply false
}


allprojects {
    group = "tech.kzen.launcher"
    version = "0.21.0"

    repositories {
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
        jcenter()
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-js-wrappers") }
        maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
        mavenCentral()

        mavenLocal()
    }
}