import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}


kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmToolchainVersion))
    }

    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
        }
//        @Suppress("UNUSED_VARIABLE")
//        val main by compilations.getting {
//            kotlinOptions {
//                jvmTarget = jvmTargetVersion
//            }
//        }
    }

    js {
        browser {
            testTask {
                testLogging {
                    showExceptions = true
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    showCauses = true
                    showStackTraces = true
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

            // api (not implementation) so consumers — notably kzen-launcher-js, which decodes the DTOs —
            // get the serialization runtime transitively.
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }


        jvmMain.dependencies {
            implementation("ch.qos.logback:logback-classic:$logbackVersion")
//                implementation("org.jetbrains.kotlin-wrappers:kotlin-css-jvm:$kotlinCssVersion")
        }


        jsMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
        }
    }
}
