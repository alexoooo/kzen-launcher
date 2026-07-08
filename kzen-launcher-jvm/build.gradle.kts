@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    kotlin("jvm")
}


kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmToolchainVersion))
    }
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
    }
}


dependencies {
    implementation(project(":kzen-launcher-common"))

    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
//    implementation("org.jetbrains.kotlin-wrappers:kotlin-css-jvm:1.0.0-$wrapperKotlinVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("org.apache.commons:commons-compress:$commonsCompressVersion")
    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonModuleKotlin")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:$jacksonDataformatYaml")


    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")

    testImplementation(kotlin("test"))
}


tasks.withType<ProcessResources> {
    val jsProject = project(":kzen-launcher-js")

    // esbuild bundle (replaces webpack) → build/dist/js/productionExecutable/<module>.js (+ .js.map)
    val bundleTask = jsProject.tasks.named("jsEsbuildBundle")
    dependsOn(bundleTask)

    from(jsProject.layout.buildDirectory.dir("dist/js/productionExecutable")) {
        into("static")
    }
}


//tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
//    compilerOptions {
//        freeCompilerArgs = listOf("-Xjsr305=strict")
//    }
//}


tasks.compileJava {
    options.release.set(javaVersion)
}


val dependenciesDir = "dependencies"
tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
        .into("${layout.buildDirectory.get().asFile}/libs/$dependenciesDir")
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


// Distribution zip: main.jar (the thin jar, Class-Path -> dependencies/) + dependencies/ at the
//  root — the layout kzen-shell's ArtifactRepo and the launcher's ProjectCreator both expect.
tasks.register<Zip>("dist") {
    dependsOn("jar", "copyDependencies")
    archiveFileName.set("kzen-launcher-$version.zip")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))

    from(tasks.named("jar")) { rename { "main.jar" } }
    from(layout.buildDirectory.dir("libs/$dependenciesDir")) { into(dependenciesDir) }
}
