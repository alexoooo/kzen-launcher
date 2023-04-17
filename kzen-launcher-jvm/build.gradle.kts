@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack


plugins {
    kotlin("jvm")
//    id("org.springframework.boot") version springBootVersion
//    id("io.spring.dependency-management") version dependencyManagementVersion
//    kotlin("plugin.spring") version kotlinVersion
}


kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmToolchainVersion))
    }
}


dependencies {
    implementation(project(":kzen-launcher-common"))

    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css-jvm:1.0.0-$wrapperKotlinVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
//    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("org.apache.commons:commons-compress:$commonsCompressVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonModuleKotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonDataformatYaml")


    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")

    testImplementation(kotlin("test"))
//    testImplementation("org.springframework.boot:spring-boot-starter-test")
//    testImplementation("io.projectreactor:reactor-test")
}


tasks.withType<ProcessResources> {
    val jsProject = project(":kzen-launcher-js")
    val task = jsProject.tasks.getByName("browserProductionWebpack") as KotlinWebpack

    from(task.destinationDirectory) {
        into("static")
    }

    dependsOn(task)
}


tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = jvmTargetVersion
    }
}


tasks.compileJava {
    options.release.set(javaVersion)
}


val dependenciesDir = "dependencies"
task("copyDependencies", Copy::class) {
    from(configurations.default).into("$buildDir/libs/$dependenciesDir")
}


tasks.getByName<Jar>("jar") {
    val jvmProject = project(":kzen-launcher-jvm")
    val copyDependenciesTask = jvmProject.tasks.getByName("copyDependencies") as Copy
    dependsOn(copyDependenciesTask)

    manifest {
        attributes["Main-Class"] = "tech.kzen.launcher.server.KzenLauncherMainKt"
        attributes["Class-Path"] = configurations
            .runtimeClasspath
            .get()
            .joinToString(separator = " ") { file ->
                "$dependenciesDir/${file.name}"
            }
    }
}


//tasks.getByName<BootJar>("bootJar") {
//    archiveClassifier.set("boot")
//}