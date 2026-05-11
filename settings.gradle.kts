rootProject.name = "kzen-launcher"
include("kzen-launcher-common", "kzen-launcher-js", "kzen-launcher-jvm")


dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("kotlinWrappers") {
            val wrappersVersion = "2026.5.3"
            from("org.jetbrains.kotlin-wrappers:kotlin-wrappers-catalog:$wrappersVersion")
        }
    }
}