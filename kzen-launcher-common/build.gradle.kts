plugins {
    kotlin("multiplatform")
}


kotlin {
    jvm {
        @Suppress("UNUSED_VARIABLE")
        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = jvmTargetVersion
            }
        }
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
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-css:$kotlinCssVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }


        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation("ch.qos.logback:logback-classic:$logbackVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-css-jvm:$kotlinCssVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {}
        }


        @Suppress("UNUSED_VARIABLE")
        val jsMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsTest by getting {
            dependencies {}
        }
    }
}
