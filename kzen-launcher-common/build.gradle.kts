import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
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
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
//                implementation("org.jetbrains.kotlin-wrappers:kotlin-css:$kotlinCssVersion")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }


        val jvmMain by getting {
            dependencies {
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
//                implementation("org.jetbrains.kotlin-wrappers:kotlin-css-jvm:$kotlinCssVersion")
            }
        }

        val jvmTest by getting {
            dependencies {}
        }


        val jsMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
            }
        }

        val jsTest by getting {
            dependencies {}
        }
    }
}
