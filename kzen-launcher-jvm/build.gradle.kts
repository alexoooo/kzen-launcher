@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


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
    // Jackson 3 tree API, used only by ProjectRepo for the kzen-projects.yaml registry (standard
    //  YAML escaping for user-file compatibility); the REST wire is kotlinx.serialization.
    implementation("tools.jackson.core:jackson-databind:$jacksonDatabind")
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:$jacksonDataformatYaml")


    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinxHtmlVersion")

    testImplementation(kotlin("test"))
}


// Build stamp: version + build timestamp baked into the jar at /kzen-launcher-build.properties, read at
// startup by BuildInfo and surfaced as logo hover text (see KzenLauncherMain / indexPage). Deliberately
// never up-to-date so every build re-stamps the moment of build — only resource processing + the thin
// jar re-run, not Kotlin compilation.
val buildInfoDir = layout.buildDirectory.dir("generated-resources")
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val buildInfoFile = buildInfoDir.map { it.file("kzen-launcher-build.properties") }
    val buildVersion = version.toString()
    outputs.file(buildInfoFile)
    outputs.upToDateWhen { false }
    doLast {
        val timestamp = OffsetDateTime.now()
            .truncatedTo(ChronoUnit.SECONDS)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        buildInfoFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("version=$buildVersion\ntimestamp=$timestamp\n")
        }
    }
}

sourceSets.main {
    resources.srcDir(buildInfoDir)
}


tasks.withType<ProcessResources> {
    val jsProject = project(":kzen-launcher-js")

    // esbuild bundle (replaces webpack) → build/dist/js/productionExecutable/<module>.js (+ .js.map)
    val bundleTask = jsProject.tasks.named("jsEsbuildBundle")
    dependsOn(bundleTask)
    dependsOn(generateBuildInfo)

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
